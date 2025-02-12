package com.jkuester.unlauncher.dialog

import android.app.AlertDialog
import android.content.DialogInterface
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datasource.setClockType
import com.jkuester.unlauncher.datastore.proto.ClockType
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
class ClockTypeDialogTest {
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
        every { corePrefsRepo.get().clockType.number } returns 0
        justRun { alertDialog.dismiss() }
        every { corePrefsRepo.updateAsync(any()) } returns mockk()
        mockkStatic(::setClockType)
        every { setClockType(any()) } returns mockk()

        val dialogFragment = ClockTypeDialog()
            .apply { corePreferencesRepo = corePrefsRepo }
        val result = dialogFragment.onCreateDialog(null)

        result shouldBe alertDialog
        verify(exactly = 1) { anyConstructed<AlertDialog.Builder>().setTitle(R.string.choose_clock_type_dialog_title) }
        verify(exactly = 1) {
            anyConstructed<AlertDialog.Builder>().setSingleChoiceItems(
                R.array.clock_type_array,
                0,
                onSelectionSlot.captured
            )
        }
        verify(exactly = 1) { anyConstructed<AlertDialog.Builder>().create() }
        verify(exactly = 1) { corePrefsRepo.get().clockType.number }

        // Trigger the listener
        onSelectionSlot.captured.onClick(alertDialog, 0)

        verify(exactly = 1) { alertDialog.dismiss() }
        verify(exactly = 1) { corePrefsRepo.updateAsync(any()) }
        verify(exactly = 1) { setClockType(ClockType.forNumber(0)) }
    }
}
