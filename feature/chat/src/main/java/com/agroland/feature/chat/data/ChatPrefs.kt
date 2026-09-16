package com.agroland.feature.chat.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Пинделген / үнсіз етілген чат roomId-лері (Flutter PinnedChatsNotifier /
 * MutedChatsNotifier, SharedPreferences). Үнсіз күй — жергілікті (backend
 * mute endpoint әзірленгенше).
 */
@Singleton
class ChatPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("agroland_chat_prefs", Context.MODE_PRIVATE)

    private val _pinnedIds = MutableStateFlow<Set<Long>>(emptySet())
    val pinnedIds: StateFlow<Set<Long>> = _pinnedIds.asStateFlow()

    private val _mutedIds = MutableStateFlow<Set<Long>>(emptySet())
    val mutedIds: StateFlow<Set<Long>> = _mutedIds.asStateFlow()

    init {
        _pinnedIds.value = prefs.getStringSet(KEY_PINNED, emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        _mutedIds.value = prefs.getStringSet(KEY_MUTED, emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    fun isPinned(roomId: Long): Boolean = roomId in _pinnedIds.value
    fun isMuted(roomId: Long): Boolean = roomId in _mutedIds.value

    fun togglePin(roomId: Long) = toggle(_pinnedIds, KEY_PINNED, roomId)
    fun toggleMute(roomId: Long) = toggle(_mutedIds, KEY_MUTED, roomId)

    private fun toggle(flow: MutableStateFlow<Set<Long>>, key: String, roomId: Long) {
        val next = flow.value.toMutableSet()
        if (roomId in next) next.remove(roomId) else next.add(roomId)
        flow.value = next
        prefs.edit().putStringSet(key, next.map(Long::toString).toSet()).apply()
    }

    private companion object {
        const val KEY_PINNED = "pinned_chat_ids"
        const val KEY_MUTED = "muted_chat_ids"
    }
}