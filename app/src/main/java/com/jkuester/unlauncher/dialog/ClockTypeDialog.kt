package com.jkuester.unlauncher.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datasource.setClockType
import com.jkuester.unlauncher.datastore.proto.ClockType
import com.sduduzog.slimlauncher.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ClockTypeDialog : DialogFragment() {
    @Inject
    lateinit var corePreferencesRepo: CorePreferencesRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog
        .Builder(context)
        .setTitle(R.string.choose_clock_type_dialog_title)
        .setSingleChoiceItems(
            R.array.clock_type_array,
            corePreferencesRepo.get().clockType.number,
            this::onSelection
        )
        .create()

    private fun onSelection(dialogInterface: DialogInterface, i: Int) = dialogInterface
        .dismiss()
        .also { corePreferencesRepo.updateAsync(setClockType(ClockType.forNumber(i))) }
}
