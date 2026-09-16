package com.agroland.feature.reviews.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * «Қаралған жарнамалар» — spec §10 (iOS MyReviewsStore): жарнама беті
 * ашылғанда id жергілікті тізімге қосылады (Flutter FeedbackPopupNotifier
 * .addToViewed үлгісі). «Менің пікірлерім» үміткерлері осы тізімнен
 * жиналады; ең қаралы 50-і ғана сақталады.
 */
@Singleton
class ViewedAnnouncementsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** Жаңа қаралған id бірінші тұрады (recency реті). */
    val viewedIds: Flow<List<Long>> = dataStore.data.map { prefs ->
        prefs[VIEWED_IDS].orEmpty()
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
    }

    suspend fun addViewed(announcementId: Long) {
        dataStore.edit { prefs ->
            val current = prefs[VIEWED_IDS].orEmpty()
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
            val merged = listOf(announcementId) + current.filterNot { it == announcementId }
            prefs[VIEWED_IDS] = merged.take(MAX_VIEWED).joinToString(",")
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(VIEWED_IDS) }
    }

    private companion object {
        val VIEWED_IDS = stringPreferencesKey("viewed_announcement_ids")
        const val MAX_VIEWED = 50
    }
}