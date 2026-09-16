package com.agroland.feature.chat.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.auth.TokenStore
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.profile.data.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ағымдағы қолданушы (id + name) — Flutter userNotifierProvider.value?.user
 * баламасы. «Сіз:» prefix, optimistic хабарламаның sender деректері және
 * жүйелік чат атауы осыдан алынады (ISSUES.md #9 — шешілді).
 *
 * Кэш әрдайым профилден толықтырылады: TokenStore-та login кезінде user_id
 * сақталмайды (AuthRepository persist userId=null), сондықтан GET /user/profile
 * — бірден-бір сенімді дереккөз.
 */
@Singleton
class CurrentUserProvider @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val tokenStore: TokenStore,
) {

    data class CurrentUser(val id: Long, val name: String)

    private val _user = MutableStateFlow<CurrentUser?>(null)
    val user: StateFlow<CurrentUser?> = _user.asStateFlow()

    /** Профильді жүктеп, кэштейді (сәтсіз болса — алдыңғы кэш қалады). */
    suspend fun refresh(): CurrentUser? {
        if (_user.value != null) return _user.value
        when (val result = profileRepository.getProfile()) {
            is ApiResult.Success -> {
                result.value.id?.let { id ->
                    _user.value = CurrentUser(id, result.value.name.orEmpty())
                }
            }
            is ApiResult.Error -> Unit
        }
        return _user.value
    }

    /** Кэштелген қолданушы (болмаса — жүктейді). */
    suspend fun get(): CurrentUser? = _user.value ?: refresh()

    /** Шығу кезінде кэшті тазалау (жаңа аккаунт басқаша болуы мүмкін). */
    fun clear() {
        _user.value = null
    }

    /** Токен бар ма (чат socket-ы үшін алдын-ала тексеру). */
    fun hasToken(): Boolean = tokenStore.accessToken.value != null
}