package com.jkuester.unlauncher.dialog

import android.app.AlertDialog
import android.content.DialogInterface
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datasource.setSearchBarPosition
import com.jkuester.unlauncher.datastore.proto.SearchBarPosition
import com.sduduzog.slimlauncher.R
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@MockKExtension.CheckUnnecessaryStub
// @MockKExtension.ConfirmVerification Weird bug between mockk/kotlin/Java is causing this to fail
@ExtendWith(MockKExtension::class)
class SearchBarPositionDialogTest {
    @MockK
    lateinit var corePrefsRepo: CorePreferencesRepository
    @MockK
    lateinit var alertDialog: AlertDialog

    @Test
    fun onCreateDialog() {
        mockkConstructor(AlertDialog.Builder::class)
        val onSelectionSlot = slot<DialogInterface.OnClickListener>()
        every { anyConstructed<AlertDialog.Builder>().setTitle(any(Int::class)) } answers
            { self as AlertDialog.Builder }
        every {
            anyConstructed<AlertDialog.Builder>().setSingleChoiceItems(
                any(Int::class),
                any(Int::class),
                capture(onSelectionSlot)
            )
        } answers { self as AlertDialog.Builder }
        every { anyConstructed<AlertDialog.Builder>().create() } returns alertDialog
        every { corePrefsRepo.get().searchBarPosition.number } returns 0
        justRun { alertDialog.dismiss() }
        every { corePrefsRepo.updateAsync(any()) } returns mockk()
        mockkStatic(::setSearchBarPosition)
        every { setSearchBarPosition(any()) } returns mockk()

        val dialogFragment = SearchBarPositionDialog()
            .apply { corePreferencesRepo = corePrefsRepo }
        val result = dialogFragment.onCreateDialog(null)

        result shouldBe alertDialog
        verify(exactly = 1) {
            anyConstructed<AlertDialog.Builder>().setTitle(R.string.choose_search_bar_position_dialog_title)
        }
        verify(exactly = 1) {
            anyConstructed<AlertDialog.Builder>().setSingleChoiceItems(
                R.array.search_bar_position_array,
                0,
                onSelectionSlot.captured
            )
        }
        verify(exactly = 1) { anyConstructed<AlertDialog.Builder>().create() }
        verify(exactly = 1) { corePrefsRepo.get().searchBarPosition.number }

        // Trigger the listener
        onSelectionSlot.captured.onClick(alertDialog, 0)

        verify(exactly = 1) { alertDialog.dismiss() }
        verify(exactly = 1) { corePrefsRepo.updateAsync(any()) }
        verify(exactly = 1) { setSearchBarPosition(SearchBarPosition.forNumber(0)) }
    }
}
