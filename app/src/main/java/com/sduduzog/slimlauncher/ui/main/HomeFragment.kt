package com.sduduzog.slimlauncher.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.jkuester.unlauncher.datastore.ClockType
import com.jkuester.unlauncher.datastore.SearchBarPosition
import com.jkuester.unlauncher.datastore.UnlauncherApp
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.adapters.AppDrawerAdapter
import com.sduduzog.slimlauncher.adapters.HomeAdapter
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.databinding.HomeFragmentBottomBinding
import com.sduduzog.slimlauncher.databinding.HomeFragmentContentBinding
import com.sduduzog.slimlauncher.databinding.HomeFragmentDefaultBinding
import com.sduduzog.slimlauncher.datasource.UnlauncherDataSource
import com.sduduzog.slimlauncher.datasource.quickbuttonprefs.QuickButtonPreferencesRepository
import com.sduduzog.slimlauncher.models.HomeApp
import com.sduduzog.slimlauncher.models.MainViewModel
import com.sduduzog.slimlauncher.ui.dialogs.RenameAppDisplayNameDialog
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.OnLaunchAppListener
import com.sduduzog.slimlauncher.utils.isSystemApp
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val APP_TILE_SIZE: Int = 3

@AndroidEntryPoint
class HomeFragment : BaseFragment(), OnLaunchAppListener {
    @Inject
    lateinit var unlauncherDataSource: UnlauncherDataSource

    private val viewModel: MainViewModel by viewModels()

    private lateinit var receiver: BroadcastReceiver
    private lateinit var appDrawerAdapter: AppDrawerAdapter
    private lateinit var uninstallAppLauncher: ActivityResultLauncher<Intent>

    // cached list of Apps list time we get it from BaseFragment
    private var curPinnedShortcuts = listOf<App>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uninstallAppLauncher = registerForActivityResult(StartActivityForResult()) { refreshApps() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val coreRepository = unlauncherDataSource.corePreferencesRepo
        return if (coreRepository.get().searchBarPosition == SearchBarPosition.bottom) {
            HomeFragmentBottomBinding.inflate(layoutInflater, container, false).root
        } else {
            HomeFragmentDefaultBinding.inflate(layoutInflater, container, false).root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter1 = HomeAdapter(this, unlauncherDataSource)
        val adapter2 = HomeAdapter(this, unlauncherDataSource)
        val homeFragmentContent = HomeFragmentContentBinding.bind(view)
        homeFragmentContent.homeFragmentList.adapter = adapter1
        homeFragmentContent.homeFragmentListExp.adapter = adapter2

        val unlauncherAppsRepo = unlauncherDataSource.unlauncherAppsRepo

        viewModel.apps.observe(viewLifecycleOwner) { list ->
            list?.let { apps ->
                adapter1.setItems(
                    apps.filter {
                        it.sortingIndex < APP_TILE_SIZE
                    }
                )
                adapter2.setItems(
                    apps.filter {
                        it.sortingIndex >= APP_TILE_SIZE
                    }
                )

                // Set the home apps in the Unlauncher data
                lifecycleScope.launch {
                    unlauncherAppsRepo.setHomeApps(apps)
                }
            }
        }

        appDrawerAdapter = AppDrawerAdapter(
            AppDrawerListener(),
            viewLifecycleOwner,
            unlauncherDataSource
        )

        setEventListeners()

        homeFragmentContent.appDrawerFragmentList.adapter = appDrawerAdapter

        unlauncherDataSource.corePreferencesRepo.liveData().observe(
            viewLifecycleOwner
        ) { corePreferences ->
            homeFragmentContent.appDrawerEditText
                .visibility = if (corePreferences.showSearchBar) View.VISIBLE else View.GONE

            val clockType = corePreferences.clockType
            homeFragmentContent.homeFragmentTime
                .visibility = if (clockType == ClockType.digital) View.VISIBLE else View.GONE
            homeFragmentContent.homeFragmentAnalogTime
                .visibility = when (clockType) {
                ClockType.analog_0,
                ClockType.analog_1,
                ClockType.analog_2,
                ClockType.analog_3,
                ClockType.analog_4,
                ClockType.analog_6,
                ClockType.analog_12,
                ClockType.analog_60 -> View.VISIBLE
                else -> View.GONE
            }
            homeFragmentContent.homeFragmentBinTime
                .visibility = if (clockType == ClockType.binary) View.VISIBLE else View.GONE
            homeFragmentContent.homeFragmentDate
                .visibility = if (clockType != ClockType.none) View.VISIBLE else View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        receiver = ClockReceiver()
        activity?.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun getFragmentView(): ViewGroup = HomeFragmentDefaultBinding.bind(
        requireView()
    ).homeFragment

    override fun onResume() {
        super.onResume()
        updateClock()

        refreshApps()
        if (!::appDrawerAdapter.isInitialized) {
            appDrawerAdapter.setAppFilter()
        }

        // scroll back to the top if user returns to this fragment
        val appDrawerFragmentList = HomeFragmentContentBinding.bind(
            requireView()
        ).appDrawerFragmentList
        val layoutManager = appDrawerFragmentList.layoutManager as LinearLayoutManager
        if (layoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
            appDrawerFragmentList.scrollToPosition(0)
        }
    }

    private fun refreshApps() {
        val installedApps = getInstalledApps()
        curPinnedShortcuts = installedApps.filter { it.appType == 1 }
        lifecycleScope.launch(Dispatchers.IO) {
            unlauncherDataSource.unlauncherAppsRepo.setApps(installedApps)
            viewModel.filterHomeApps(installedApps)
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(receiver)
        resetAppDrawerEditText()
    }

    private fun setEventListeners() {
        val launchShowAlarms = OnClickListener {
            try {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                launchActivity(it, intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                // Do nothing, we've failed :(
            }
        }
        val homeFragmentContent = HomeFragmentContentBinding.bind(requireView())
        homeFragmentContent.homeFragmentTime.setOnClickListener(launchShowAlarms)
        homeFragmentContent.homeFragmentAnalogTime.setOnClickListener(launchShowAlarms)
        homeFragmentContent.homeFragmentBinTime.setOnClickListener(launchShowAlarms)

        homeFragmentContent.homeFragmentDate.setOnClickListener {
            try {
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                val intent = Intent(Intent.ACTION_VIEW, builder.build())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                launchActivity(it, intent)
            } catch (e: ActivityNotFoundException) {
                // Do nothing, we've failed :(
            }
        }

        unlauncherDataSource.quickButtonPreferencesRepo.liveData()
            .observe(viewLifecycleOwner) { prefs ->
                val leftButtonIcon = QuickButtonPreferencesRepository.RES_BY_ICON.getValue(
                    prefs.leftButton.iconId
                )
                homeFragmentContent.homeFragmentCall.setImageResource(leftButtonIcon)
                var anyOn = false
                if (leftButtonIcon != R.drawable.ic_empty) {
                    anyOn = true
                    homeFragmentContent.homeFragmentCall.setOnClickListener { view ->
                        try {
                            val pm = context?.packageManager!!
                            val intent = Intent(Intent.ACTION_DIAL)
                            val componentName = intent.resolveActivity(pm)
                            if (componentName == null) {
                                launchActivity(view, intent)
                            } else {
                                pm.getLaunchIntentForPackage(componentName.packageName)?.let {
                                    launchActivity(view, it)
                                } ?: run { launchActivity(view, intent) }
                            }
                        } catch (e: Exception) {
                            // Do nothing
                        }
                    }
                }

                val centerButtonIcon = QuickButtonPreferencesRepository.RES_BY_ICON.getValue(
                    prefs.centerButton.iconId
                )
                homeFragmentContent.homeFragmentOptions.setImageResource(centerButtonIcon)
                if (centerButtonIcon != R.drawable.ic_empty) {
                    anyOn = true
                    homeFragmentContent.homeFragmentOptions.setOnClickListener(
                        Navigation.createNavigateOnClickListener(
                            R.id.action_homeFragment_to_optionsFragment
                        )
                    )
                }

                val rightButtonIcon = QuickButtonPreferencesRepository.RES_BY_ICON.getValue(
                    prefs.rightButton.iconId
                )
                homeFragmentContent.homeFragmentCamera.setImageResource(rightButtonIcon)
                if (rightButtonIcon != R.drawable.ic_empty) {
                    anyOn = true
                    homeFragmentContent.homeFragmentCamera.setOnClickListener {
                        try {
                            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            launchActivity(it, intent)
                        } catch (e: Exception) {
                            // Do nothing
                        }
                    }
                }

                val vis = if (anyOn) View.VISIBLE else View.GONE
                homeFragmentContent.homeFragmentCall.visibility = vis
                homeFragmentContent.homeFragmentOptions.visibility = vis
                homeFragmentContent.homeFragmentCamera.visibility = vis
            }

        homeFragmentContent.appDrawerEditText.addTextChangedListener(
            appDrawerAdapter.searchBoxListener
        )

        val homeFragment = HomeFragmentDefaultBinding.bind(requireView()).root
        homeFragmentContent.appDrawerEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && appDrawerAdapter.itemCount > 0) {
                val firstApp = appDrawerAdapter.getFirstApp()
                launch(
                    firstApp.appType,
                    firstApp.packageName,
                    firstApp.className,
                    firstApp.userSerial
                )
                homeFragment.transitionToStart()
                true
            } else {
                false
            }
        }

        homeFragment.setTransitionListener(object : TransitionListener {
            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                val inputMethodManager = requireContext().getSystemService(
                    Activity.INPUT_METHOD_SERVICE
                ) as InputMethodManager

                when (currentId) {
                    motionLayout?.startState -> {
                        // hide the keyboard and remove focus from the EditText when swiping back up
                        resetAppDrawerEditText()
                        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
                    }

                    motionLayout?.endState -> {
                        val preferences = unlauncherDataSource.corePreferencesRepo.get()
                        // Check for preferences to open the keyboard
                        // only if the search field is shown
                        if (preferences.showSearchBar && preferences.activateKeyboardInDrawer) {
                            homeFragmentContent.appDrawerEditText.requestFocus()
                            // show the keyboard and set focus to the EditText when swiping down
                            inputMethodManager.showSoftInput(
                                homeFragmentContent.appDrawerEditText,
                                InputMethodManager.SHOW_IMPLICIT
                            )
                        }
                    }
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
                // do nothing
            }

            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
                // do nothing
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                // do nothing
            }
        })
    }

    fun updateClock() {
        updateDate()
        val homeFragmentContent = HomeFragmentContentBinding.bind(requireView())
        val clockType = unlauncherDataSource.corePreferencesRepo.get().clockType
        when (clockType) {
            ClockType.digital -> updateClockDigital()
            ClockType.analog_0,
            ClockType.analog_1,
            ClockType.analog_2,
            ClockType.analog_3,
            ClockType.analog_4,
            ClockType.analog_6,
            ClockType.analog_12,
            ClockType.analog_60 -> {
                homeFragmentContent.homeFragmentAnalogTime.updateClock(clockType)
            }
            ClockType.binary -> homeFragmentContent.homeFragmentBinTime.updateClock(clockType)
            else -> {}
        }
    }

    private fun updateClockDigital() {
        val timeFormat = context?.getSharedPreferences(
            getString(R.string.prefs_settings),
            Context.MODE_PRIVATE
        )
            ?.getInt(getString(R.string.prefs_settings_key_time_format), 0)
        val fWatchTime = when (timeFormat) {
            1 -> SimpleDateFormat("H:mm", Locale.getDefault())
            2 -> SimpleDateFormat("h:mm aa", Locale.getDefault())
            else -> DateFormat.getTimeFormat(context)
        }
        val homeFragmentContent = HomeFragmentContentBinding.bind(requireView())
        homeFragmentContent.homeFragmentTime.text = fWatchTime.format(Date())
    }

    private fun updateDate() {
        val fWatchDate = SimpleDateFormat(getString(R.string.main_date_format), Locale.getDefault())
        val homeFragmentContent = HomeFragmentContentBinding.bind(requireView())
        homeFragmentContent.homeFragmentDate.text = fWatchDate.format(Date())
    }

    override fun onLaunch(app: HomeApp, view: View) {
        launch(app.appType, app.packageName, app.activityName, app.userSerial)
    }

    override fun onBack(): Boolean {
        val homeFragment = HomeFragmentDefaultBinding.bind(requireView()).root
        homeFragment.transitionToStart()
        return true
    }

    override fun onHome() {
        val homeFragment = HomeFragmentDefaultBinding.bind(requireView()).root
        homeFragment.transitionToStart()
    }

    inner class ClockReceiver : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            updateClock()
        }
    }

    private fun launchAppAux(packageName: String, activityName: String, userSerial: Long) {
        try {
            val launcher = getLauncher()
            val componentName = ComponentName(packageName, activityName)
            val userHandle = getUserHandle(userSerial)
            launcher.startMainActivity(componentName, userHandle, view?.clipBounds, null)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.couldnt_start_app, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchShortcutAux(packageName: String, activityName: String, userSerial: Long) {
        try {
            val launcher = getLauncher()
            val userHandle = getUserHandle(userSerial)
            launcher.startShortcut(packageName, activityName, view?.clipBounds, null, userHandle)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.couldnt_start_shortcut, Toast.LENGTH_LONG).show()
        }
    }

    private fun launch(appType: Int, packageName: String, activityName: String, userSerial: Long) {
        when (appType) {
            0 -> launchAppAux(packageName, activityName, userSerial)
            1,
            2 -> launchShortcutAux(packageName, activityName, userSerial)
        }
    }

    private fun resetAppDrawerEditText() {
        val homeFragmentContent = HomeFragmentContentBinding.bind(requireView())
        homeFragmentContent.appDrawerEditText.clearComposingText()
        homeFragmentContent.appDrawerEditText.setText("")
        homeFragmentContent.appDrawerEditText.clearFocus()
    }

    inner class AppDrawerListener {
        @SuppressLint("DiscouragedPrivateApi")
        fun onAppLongClicked(app: UnlauncherApp, view: View): Boolean {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.app_long_press_menu)
            hideOptionsMaybe(app, popupMenu)

            popupMenu.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.open -> {
                        onAppClicked(app)
                    }
                    R.id.info -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addCategory(Intent.CATEGORY_DEFAULT)
                        intent.data = Uri.parse("package:" + app.packageName)
                        startActivity(intent)
                    }
                    R.id.hide -> {
                        unlauncherDataSource.unlauncherAppsRepo.updateDisplayInDrawer(app, false)
                        Toast.makeText(
                            context,
                            R.string.unhide_hint,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    R.id.rename -> {
                        RenameAppDisplayNameDialog.getInstance(
                            app,
                            unlauncherDataSource.unlauncherAppsRepo
                        ).show(childFragmentManager, "AppListAdapter")
                    }
                    R.id.uninstall -> {
                        if (app.appType == 0) {
                            val intent = Intent(Intent.ACTION_DELETE)
                            intent.data = Uri.parse("package:" + app.packageName)
                            uninstallAppLauncher.launch(intent)
                        }
                    }
                    R.id.remove -> {
                        if (app.appType == 1) {
                            var ids = curPinnedShortcuts.filter {
                                it.packageName == app.packageName &&
                                    it.activityName != app.className
                            }.map {
                                it.activityName
                            }
                            val userHandle = getUserHandle(app.userSerial)
                            try {
                                val launcher = getLauncher()
                                launcher.pinShortcuts(app.packageName, ids, userHandle)
                            } catch (e: IllegalStateException) {
                                Toast.makeText(
                                    context,
                                    R.string.unable_to_unpin,
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: SecurityException) {
                                Toast.makeText(
                                    context,
                                    R.string.unable_to_unpin,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            refreshApps()
                            Toast.makeText(context, R.string.shortcut_unpinned, Toast.LENGTH_LONG)
                                .show()
                            backHome()
                        }
                    }
                }
                true
            }

            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)

            popupMenu.show()
            return true
        }

        private fun hideOptionsMaybe(app: UnlauncherApp, popupMenu: PopupMenu) {
            val pm = requireContext().packageManager
            val info = pm.getApplicationInfo(app.packageName, 0)
            // System apps and shortcuts cannot be uninstalled
            if (info.isSystemApp() || (app.appType != 0)) {
                val item = popupMenu.menu.findItem(R.id.uninstall)
                item.isVisible = false
            }
            // Except for pinned shortcuts, nothing else can be removed:
            if (app.appType != 1) {
                val item = popupMenu.menu.findItem(R.id.remove)
                item.isVisible = false
            }
        }

        fun onAppClicked(app: UnlauncherApp) {
            launch(app.appType, app.packageName, app.className, app.userSerial)
            backHome()
        }

        fun backHome() {
            val homeFragment = HomeFragmentDefaultBinding.bind(requireView()).root
            homeFragment.transitionToStart()
        }
    }

    private fun getManager(): UserManager {
        return requireContext().getSystemService(Context.USER_SERVICE) as UserManager
    }

    private fun getLauncher(): LauncherApps {
        return requireContext().getSystemService(
            Context.LAUNCHER_APPS_SERVICE
        ) as LauncherApps
    }

    private fun getUserHandle(userSerial: Long): UserHandle {
        return getManager().getUserForSerialNumber(userSerial)
    }
}
