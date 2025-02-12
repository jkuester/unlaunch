package com.jkuester.unlauncher.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datasource.setSearchBarPosition
import com.jkuester.unlauncher.datastore.proto.SearchBarPosition
import com.sduduzog.slimlauncher.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchBarPositionDialog : DialogFragment() {
    @Inject
    lateinit var corePreferencesRepo: CorePreferencesRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog
        .Builder(context)
        .setTitle(R.string.choose_search_bar_position_dialog_title)
        .setSingleChoiceItems(
            R.array.search_bar_position_array,
            corePreferencesRepo.get().searchBarPosition.number,
            this::onSelection
        )
        .create()

    private fun onSelection(dialogInterface: DialogInterface, i: Int) = dialogInterface
        .dismiss()
        .also { corePreferencesRepo.updateAsync(setSearchBarPosition(SearchBarPosition.forNumber(i))) }
}
