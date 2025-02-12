package com.sduduzog.slimlauncher.ui.options

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.navigation.Navigation
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datasource.setKeepDeviceWallpaper
import com.jkuester.unlauncher.dialog.AlignmentFormatDialog
import com.jkuester.unlauncher.dialog.ClockTypeDialog
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.databinding.OptionsFragmentBinding
import com.sduduzog.slimlauncher.ui.dialogs.ChangeThemeDialog
import com.sduduzog.slimlauncher.ui.dialogs.ChooseTimeFormatDialog
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.createTitleAndSubtitleText
import com.sduduzog.slimlauncher.utils.isActivityDefaultLauncher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OptionsFragment : BaseFragment() {
    @Inject
    lateinit var corePreferencesRepo: CorePreferencesRepository

    override fun getFragmentView(): ViewGroup = OptionsFragmentBinding.bind(
        requireView()
    ).optionsFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.options_fragment, container, false)

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val optionsFragment = OptionsFragmentBinding.bind(requireView())
        optionsFragment.optionsFragmentDeviceSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_SETTINGS)
            launchActivity(it, intent)
        }
        optionsFragment.optionsFragmentBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        optionsFragment.optionsFragmentDeviceSettings.setOnLongClickListener {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            launchActivity(it, intent)
            true
        }
        optionsFragment.optionsFragmentChangeTheme.setOnClickListener {
            val changeThemeDialog = ChangeThemeDialog.getThemeChooser()
            changeThemeDialog.showNow(childFragmentManager, "THEME_CHOOSER")
        }
        optionsFragment.optionsFragmentChooseTimeFormat.setOnClickListener {
            val chooseTimeFormatDialog = ChooseTimeFormatDialog.getInstance()
            chooseTimeFormatDialog.showNow(childFragmentManager, "TIME_FORMAT_CHOOSER")
        }
        optionsFragment.optionsFragmentChooseClockType.setOnClickListener {
            ClockTypeDialog().showNow(childFragmentManager, "CLOCK_TYPE_CHOOSER")
        }
        optionsFragment.optionsFragmentChooseAlignment.setOnClickListener {
            AlignmentFormatDialog().showNow(childFragmentManager, "ALIGNMENT_CHOOSER")
        }
        optionsFragment.optionsFragmentToggleStatusBar.setOnClickListener {
            val settings = requireContext().getSharedPreferences(
                getString(R.string.prefs_settings),
                MODE_PRIVATE
            )
            val isHidden = settings.getBoolean(
                getString(R.string.prefs_settings_key_toggle_status_bar),
                false
            )
            settings.edit {
                putBoolean(getString(R.string.prefs_settings_key_toggle_status_bar), !isHidden)
            }
        }
        optionsFragment.optionsFragmentCustomiseApps.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_optionsFragment_to_customiseAppsFragment
            )
        )
        optionsFragment.optionsFragmentCustomizeQuickButtons.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_optionsFragment_to_customiseQuickButtonsFragment
            )
        )
        optionsFragment.optionsFragmentCustomizeAppDrawer.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_optionsFragment_to_customiseAppDrawerFragment
            )
        )
    }

    override fun onStart() {
        super.onStart()
        // setting up the switch text, since changing the default launcher re-starts the activity
        // this should able to adapt to it.
        setupAutomaticDeviceWallpaperSwitch()
    }

    private fun setupAutomaticDeviceWallpaperSwitch() {
        val appIsDefaultLauncher = isActivityDefaultLauncher(activity)
        val optionsFragment = OptionsFragmentBinding.bind(requireView())
        setupDeviceWallpaperSwitchText(optionsFragment, appIsDefaultLauncher)
        optionsFragment.optionsFragmentAutoDeviceThemeWallpaper.isEnabled = appIsDefaultLauncher

        corePreferencesRepo.observe(viewLifecycleOwner) {
            // always uncheck once app isn't default launcher
            optionsFragment.optionsFragmentAutoDeviceThemeWallpaper
                .isChecked = appIsDefaultLauncher && !it.keepDeviceWallpaper
        }
        optionsFragment.optionsFragmentAutoDeviceThemeWallpaper
            .setOnCheckedChangeListener { _, checked ->
                corePreferencesRepo.updateAsync(setKeepDeviceWallpaper(!checked))
            }
    }

    /**
     * Adds a hint text underneath the default text when app is not the default launcher.
     */
    private fun setupDeviceWallpaperSwitchText(optionsFragment: OptionsFragmentBinding, appIsDefaultLauncher: Boolean) {
        val text = if (appIsDefaultLauncher) {
            getText(R.string.customize_app_drawer_fragment_auto_theme_wallpaper_text)
        } else {
            buildSwitchTextWithHint()
        }
        optionsFragment.optionsFragmentAutoDeviceThemeWallpaper.text = text
    }

    private fun buildSwitchTextWithHint(): CharSequence {
        val titleText = getText(R.string.customize_app_drawer_fragment_auto_theme_wallpaper_text)
        // have a title text and a subtitle text to indicate that adapting the
        // wallpaper can only be done when app it the default launcher
        val subTitleText = getText(
            R.string.customize_app_drawer_fragment_auto_theme_wallpaper_subtext_no_default_launcher
        )
        return createTitleAndSubtitleText(requireContext(), titleText, subTitleText)
    }
}
