package com.agroland.feature.auth.session

import com.agroland.core.network.auth.AuthInterceptor
import com.agroland.core.network.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Сессия күйі — барлық экран осы ағынды бақылайды.
 * Unauthorized хабары AuthInterceptor-дан келгенде Guest күйіне өтеді.
 */
sealed interface SessionState {
    data object Loading : SessionState
    data object Authorized : SessionState
    data object Guest : SessionState
}

@Singleton
class SessionController @Inject constructor(
    private val tokenStore: TokenStore,
    authInterceptor: AuthInterceptor,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state

    init {
        _state.value = if (tokenStore.isAuthorized) SessionState.Authorized else SessionState.Guest
        // Single-flight refresh сәтсіз → logout.
        authInterceptor.onUnauthorized {
            _state.value = SessionState.Guest
        }
    }

    /** Login сәтті аяқталғанда шақырылады. */
    fun onLoggedIn() {
        _state.value = SessionState.Authorized
    }

    /** Пайдаланушы шыққанда. */
    fun onLoggedOut() {
        tokenStore.clear()
        _state.value = SessionState.Guest
    }

    /** Token күйін қайта оқу (мыс. биометриялық кіруден кейін). */
    fun refreshState() {
        _state.value = if (tokenStore.isAuthorized) SessionState.Authorized else SessionState.Guest
    }
}