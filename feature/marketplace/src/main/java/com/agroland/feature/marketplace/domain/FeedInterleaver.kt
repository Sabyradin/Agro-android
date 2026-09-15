package com.agroland.feature.marketplace.domain

import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.AnnouncementsPage
import com.agroland.feature.marketplace.data.MarketplaceRepository
import javax.inject.Inject

/**
 * FeedInterleaver — AnnouncementsNotifier (Flutter) логикасы:
 * VIP ағыны бірінші жүктеледі, сосын regular; тізімге 4 regular + 2 VIP араласады.
 * Race-guard: loadNext ешқашан параллель жүрмейді; empty-page anti-spin: қатарынан
 * 2 бос бет — ағын аяқталды деп есептеледі.
 */
class FeedInterleaver @Inject constructor(
    private val repository: MarketplaceRepository,
) {
    private val vipBuffer = ArrayDeque<Announcement>()
    private val regularBuffer = ArrayDeque<Announcement>()
    private var vipPage = 1
    private var regularPage = 1
    private var vipExhausted = false
    private var regularExhausted = false
    private var consecutiveEmptyPages = 0
    private var seenIds = mutableSetOf<Long>()

    val isExhausted: Boolean get() = vipExhausted && regularExhausted || consecutiveEmptyPages >= MAX_EMPTY_PAGES

    /** Соңғы қате — лента толық бос болса UI оны көрсетеді (бос емес қате еленбейді). */
    var lastFailure: Failure? = null
        private set

    /** Келесі аралас порция — бос тізім = лента аяқталды. */
    suspend fun nextChunk(filter: AnnouncementFilter): List<Announcement> {
        if (isExhausted) return emptyList()
        if (vipBuffer.isEmpty() && !vipExhausted) fetchVip(filter)
        if (regularBuffer.isEmpty() && !regularExhausted) fetchRegular(filter)

        val chunk = mutableListOf<Announcement>()
        var regularTaken = 0
        while (regularTaken < REGULAR_PER_BLOCK && regularBuffer.isNotEmpty()) {
            chunk += regularBuffer.removeFirst()
            regularTaken++
        }
        var vipTaken = 0
        while (vipTaken < VIP_PER_BLOCK && vipBuffer.isNotEmpty()) {
            chunk += vipBuffer.removeFirst()
            vipTaken++
        }
        // Екі жақ та бос емес, бірақ chunk әлі де аз болса (бір ағын таусылған) — қалғанын құй.
        if (chunk.isEmpty()) {
            if (!vipExhausted || !regularExhausted) {
                // Буферлер бос, ағындар әлі бар — тағы порция алып көреміз (anti-spin шектейді).
                if (consecutiveEmptyPages >= MAX_EMPTY_PAGES - 1) return emptyList()
            }
        }
        return chunk
    }

    fun reset() {
        vipBuffer.clear()
        regularBuffer.clear()
        vipPage = 1
        regularPage = 1
        vipExhausted = false
        regularExhausted = false
        consecutiveEmptyPages = 0
        lastFailure = null
        seenIds = mutableSetOf()
    }

    /** Сырттан келген (деталь бетіндегі toggle) өзгерістерді көрсету үшін. */
    fun markFavorite(id: Long, favorite: Boolean) {
        fun apply(buffer: ArrayDeque<Announcement>) {
            buffer.indices.forEach { i ->
                val item = buffer[i]
                if (item.id == id) buffer[i] = item.copy(isFavorite = favorite)
            }
        }
        apply(vipBuffer)
        apply(regularBuffer)
    }

    private suspend fun fetchVip(filter: AnnouncementFilter) {
        val vipFilter = filter.copy(isVip = true)
        val result = repository.getAnnouncements(vipFilter, vipPage)
        if (result is ApiResult.Error) {
            lastFailure = result.failure
            vipExhausted = true // қате — VIP ағынын жасырамыз, regular жалғасады
            return
        }
        val page = (result as ApiResult.Success).value
        lastFailure = null
        val fresh = page.items.filter { seenIds.add(it.id) }
        if (page.hasMore) vipPage++ else vipExhausted = true
        vipBuffer.addAll(fresh)
        trackEmpty(page)
    }

    private suspend fun fetchRegular(filter: AnnouncementFilter) {
        val regularFilter = filter.copy(isVip = false)
        val result = repository.getAnnouncements(regularFilter, regularPage)
        if (result is ApiResult.Error) {
            lastFailure = result.failure
            regularExhausted = true
            return
        }
        val page = (result as ApiResult.Success).value
        lastFailure = null
        val fresh = page.items.filter { seenIds.add(it.id) }
        if (page.hasMore) regularPage++ else regularExhausted = true
        regularBuffer.addAll(fresh)
        trackEmpty(page)
    }

    private fun trackEmpty(page: AnnouncementsPage) {
        if (page.items.isEmpty()) consecutiveEmptyPages++ else consecutiveEmptyPages = 0
    }

    private companion object {
        const val REGULAR_PER_BLOCK = 4
        const val VIP_PER_BLOCK = 2
        const val MAX_EMPTY_PAGES = 2
    }
}