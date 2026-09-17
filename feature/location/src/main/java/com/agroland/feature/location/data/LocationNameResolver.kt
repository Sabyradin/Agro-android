package com.agroland.feature.location.data

import com.agroland.core.network.ApiResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Локация каталогының ID → атау кэші.
 *
 * Лента жауабы (`GET /announcements`) қала/аудан атауын жібермейді — тек
 * `location: {region_id, district_id}` келеді. Карточкада иесіз пин тұрмас
 * үшін атауды осы шешуші береді: каталог бір рет жүктеліп, процесс бойы
 * кэште қалады (ол сирек өзгереді). Жүктелмесе — null, сонда UI локация
 * жолын мүлдем көрсетпейді.
 */
@Singleton
class LocationNameResolver @Inject constructor(
    private val repository: LocationRepository,
) {

    private val mutex = Mutex()
    private var regions: Map<Int, CatalogLocation>? = null
    private var districts: Map<Int, CatalogLocation>? = null

    /**
     * Орын атауы. [short] — лента карточкасына арналған қысқа түрі: тек аудан
     * (болмаса облыс), себебі «Аудан, Облыс» тар плиткада қиылып қалады.
     * Толық түрі («Аудан, Облыс») деталь бетінде қолданылады.
     */
    suspend fun label(regionId: Int?, districtId: Int?, short: Boolean = true): String? {
        if (regionId == null && districtId == null) return null
        ensureLoaded()
        val locale = Locale.getDefault().toLanguageTag()
        val district = districtId?.let { districts?.get(it) }?.localizedName(locale)?.takeIf { it.isNotBlank() }
        val region = regionId?.let { regions?.get(it) }?.localizedName(locale)?.takeIf { it.isNotBlank() }
        if (short) return district ?: region
        return listOfNotNull(district, region).distinct().joinToString(", ").takeIf { it.isNotBlank() }
    }

    /** Екі каталог қатар жүктеледі — лента бірінші рендерін екі емес, бір сұрау уақыты күтеді. */
    private suspend fun ensureLoaded() {
        if (regions != null && districts != null) return
        mutex.withLock {
            if (regions != null && districts != null) return
            coroutineScope {
                val regionsJob = async { repository.getRegions(DEFAULT_COUNTRY_ID) }
                val districtsJob = async { repository.getDistricts(countryId = DEFAULT_COUNTRY_ID) }
                (regionsJob.await() as? ApiResult.Success)?.let { regions = it.value.associateBy { r -> r.id } }
                (districtsJob.await() as? ApiResult.Success)?.let { districts = it.value.associateBy { d -> d.id } }
            }
        }
    }

    private companion object {
        /** Қазақстан — лентадағы жарнамалардың басым бөлігі (country_id=4). */
        const val DEFAULT_COUNTRY_ID = 4
    }
}
