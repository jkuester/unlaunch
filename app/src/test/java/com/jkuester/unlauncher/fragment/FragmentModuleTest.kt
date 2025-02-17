package com.jkuester.unlauncher.fragment

import androidx.datastore.core.DataStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jkuester.unlauncher.datasource.CorePreferencesRepository
import com.jkuester.unlauncher.datastore.proto.CorePreferences
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@MockKExtension.CheckUnnecessaryStub
@MockKExtension.ConfirmVerification
@ExtendWith(MockKExtension::class)
class FragmentModuleTest {
    private val fragmentModule = FragmentModule()

    @Test
    fun provideFragmentManager() {
        val mFragment = mockk<Fragment>()
        val mFragmentManager = mockk<FragmentManager>()
        every { mFragment.childFragmentManager } returns mFragmentManager

        val fragmentManager = fragmentModule.provideFragmentManager(mFragment)

        fragmentManager shouldBe mFragmentManager
        verify(exactly = 1) { mFragment.childFragmentManager }
    }

    @Test
    fun provideLifecycleScope() {
        val mFragment = mockk<Fragment>()
        val mLifecycleScope = mockk<LifecycleCoroutineScope>()
        mockkStatic(LifecycleOwner::lifecycleScope)
        every { any<LifecycleOwner>().lifecycleScope } returns mLifecycleScope

        val lifecycleScope = fragmentModule.provideLifecycleScope(mFragment)

        lifecycleScope shouldBe mLifecycleScope
        verify(exactly = 1) { mFragment.lifecycleScope }
    }

    @Test
    fun provideLifecycleOwnerSupplier() {
        val mFragment = mockk<Fragment>()
        val mLifecycleOwner = mockk<LifecycleOwner>()
        every { mFragment.viewLifecycleOwner } returns mLifecycleOwner

        val lifecycleOwnerSupplier = fragmentModule.provideLifecycleOwnerSupplier(mFragment)

        lifecycleOwnerSupplier.get() shouldBe mLifecycleOwner
        verify(exactly = 1) { mFragment.viewLifecycleOwner }
    }

    @Test
    fun provideCorePreferencesRepo() {
        val prefsStore = mockk<DataStore<CorePreferences>>()
        every { prefsStore.data } returns emptyFlow()

        val prefsRepo = fragmentModule.provideCorePreferencesRepo(prefsStore, mockk(), mockk())

        prefsRepo.shouldBeInstanceOf<CorePreferencesRepository>()
        verify(exactly = 1) { prefsStore.data }
    }
}
