package com.sduduzog.slimlauncher.ui.options

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datasource.setActivateKeyboardInDrawer
import com.jkuester.unlauncher.datasource.setSearchAllAppsInDrawer
import com.jkuester.unlauncher.datasource.setShowSearchBar
import com.jkuester.unlauncher.dialog.SearchBarPositionDialog
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.databinding.CustomizeAppDrawerFragmentSearchFieldOptionsBinding
import com.sduduzog.slimlauncher.utils.BaseFragment
import com.sduduzog.slimlauncher.utils.createTitleAndSubtitleText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CustomizeSearchFieldFragment : BaseFragment() {

    @Inject
    lateinit var corePreferencesRepo: CorePreferencesRepository

    override fun getFragmentView(): ViewGroup = CustomizeAppDrawerFragmentSearchFieldOptionsBinding.bind(
        requireView()
    ).customizeAppDrawerFragmentSearchFieldOptions

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(
            R.layout.customize_app_drawer_fragment_search_field_options,
            container,
            false
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val options = CustomizeAppDrawerFragmentSearchFieldOptionsBinding.bind(
            view
        )
        options.customiseAppsFragmentBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setupShowSearchBarSwitch(options)
        setupSearchBarPositionOption(options)
        setupKeyboardSwitch(options)
        setupSearchAllAppsSwitch(options)
    }

    private fun setupShowSearchBarSwitch(options: CustomizeAppDrawerFragmentSearchFieldOptionsBinding) {
        options.customizeAppDrawerFragmentShowSearchFieldSwitch
            .setOnCheckedChangeListener { _, checked ->
                corePreferencesRepo.updateAsync(setShowSearchBar(checked))
                enableSearchBarOptions(options, checked)
            }
        corePreferencesRepo.observe(viewLifecycleOwner) {
            val checked = it.showSearchBar
            options.customizeAppDrawerFragmentShowSearchFieldSwitch.isChecked = checked
            enableSearchBarOptions(options, checked)
        }
    }

    private fun enableSearchBarOptions(options: CustomizeAppDrawerFragmentSearchFieldOptionsBinding, enabled: Boolean) {
        options.customizeAppDrawerFragmentSearchFieldPosition.isEnabled = enabled
        options.customizeAppDrawerOpenKeyboardSwitch.isEnabled = enabled
        options.customizeAppDrawerSearchAllSwitch.isEnabled = enabled
    }

    private fun setupSearchBarPositionOption(options: CustomizeAppDrawerFragmentSearchFieldOptionsBinding) {
        options.customizeAppDrawerFragmentSearchFieldPosition.setOnClickListener {
            SearchBarPositionDialog().showNow(childFragmentManager, "POSITION_CHOOSER")
        }
        corePreferencesRepo.observe(viewLifecycleOwner) {
            val position = it.searchBarPosition.number
            val title = getText(R.string.customize_app_drawer_fragment_search_bar_position)
            val subtitle = resources.getTextArray(R.array.search_bar_position_array)[position]
            options.customizeAppDrawerFragmentSearchFieldPosition.text =
                createTitleAndSubtitleText(requireContext(), title, subtitle)
        }
    }

    private fun setupKeyboardSwitch(options: CustomizeAppDrawerFragmentSearchFieldOptionsBinding) {
        options.customizeAppDrawerOpenKeyboardSwitch.setOnCheckedChangeListener { _, checked ->
            corePreferencesRepo.updateAsync(setActivateKeyboardInDrawer(checked))
        }
        corePreferencesRepo.observe(viewLifecycleOwner) {
            options.customizeAppDrawerOpenKeyboardSwitch.isChecked = it.activateKeyboardInDrawer
        }
        options.customizeAppDrawerOpenKeyboardSwitch.text =
            createTitleAndSubtitleText(
                requireContext(),
                R.string.customize_app_drawer_fragment_open_keyboard,
                R.string.customize_app_drawer_fragment_open_keyboard_subtitle
            )
    }

    private fun setupSearchAllAppsSwitch(options: CustomizeAppDrawerFragmentSearchFieldOptionsBinding) {
        options.customizeAppDrawerSearchAllSwitch.setOnCheckedChangeListener { _, checked ->
            corePreferencesRepo.updateAsync(setSearchAllAppsInDrawer(checked))
        }
        corePreferencesRepo.observe(viewLifecycleOwner) {
            options.customizeAppDrawerSearchAllSwitch.isChecked = it.searchAllAppsInDrawer
        }
        options.customizeAppDrawerSearchAllSwitch.text =
            createTitleAndSubtitleText(
                requireContext(),
                R.string.customize_app_drawer_fragment_search_all,
                R.string.customize_app_drawer_fragment_search_all_subtitle
            )
    }
}
