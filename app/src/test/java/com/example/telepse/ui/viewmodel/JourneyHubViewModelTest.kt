package com.example.telepse.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class JourneyHubViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var sessionStore: SessionStore
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        sessionStore = SessionStore.createPlainForUnitTests(context)
        sessionStore.clear()
        authRepository =
            AuthRepository(
                httpClient = HttpClient(MockEngine { respondOk("{}") }),
                sessionStore = sessionStore,
            )
    }

    @After
    fun tearDown() {
        sessionStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun saveSession_updatesUiStateWithoutManualRefresh() =
        runTest {
            val vm = JourneyHubViewModel(authRepository)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.loggedIn)

            sessionStore.saveSession("test-token", "tipper@example.com")
            advanceUntilIdle()
            assertTrue(vm.uiState.value.loggedIn)
            assertEquals("tipper@example.com", vm.uiState.value.tipperEmail)
        }

    @Test
    fun clearSession_updatesUiState() =
        runTest {
            sessionStore.saveSession("test-token", "tipper@example.com")
            val vm = JourneyHubViewModel(authRepository)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.loggedIn)

            sessionStore.clear()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.loggedIn)
            assertEquals(null, vm.uiState.value.tipperEmail)
        }

    @Test
    fun signOut_clearsUiState() =
        runTest {
            sessionStore.saveSession("test-token", "tipper@example.com")
            val vm = JourneyHubViewModel(authRepository)
            advanceUntilIdle()

            vm.signOut()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.loggedIn)
            assertFalse(sessionStore.isLoggedIn())
        }

    @Test
    fun refresh_reflectsCurrentSessionStore() =
        runTest {
            val vm = JourneyHubViewModel(authRepository)
            advanceUntilIdle()
            sessionStore.saveSession("token", "refresh@test.com")
            vm.refresh()
            assertTrue(vm.uiState.value.loggedIn)
            assertEquals("refresh@test.com", vm.uiState.value.tipperEmail)
        }
}
