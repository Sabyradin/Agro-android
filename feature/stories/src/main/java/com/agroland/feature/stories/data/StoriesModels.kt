package com.agroland.feature.stories.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Admin стори (Flutter StoryModel.dart, 1:1): GET /stories → {stories: [...]}.
 * Тілдерге бөлек атаулар (kz/eng/ru/ch) — titleLocalized арқылы шығады.
 */
data class StoryItem(
    val id: String,
    val title: String,
    val kzTitle: String?,
    val engTitle: String?,
    val ruTitle: String?,
    val chTitle: String?,
    val imageUrl: String,
    val isViewed: Boolean,
    val ctaLink: String?,
) {
    /** Тіл тегі бойынша атау (kk → kz, zh → ch; болмаса әдепкі title). */
    fun titleLocalized(localeTag: String?): String = when (localeTag) {
        "kk" -> kzTitle ?: title
        "en" -> engTitle ?: title
        "ru" -> ruTitle ?: title
        "zh" -> chTitle ?: title
        else -> title
    }

    companion object {
        fun fromJson(root: JsonElement?): StoryItem? {
            val obj = root as? JsonObject ?: return null
            val id = JsonParser.string(obj, "id") ?: return null
            val imageUrl = JsonParser.string(obj, "image_url") ?: ""
            return StoryItem(
                id = id,
                title = JsonParser.string(obj, "title") ?: "",
                kzTitle = JsonParser.string(obj, "kz_title"),
                engTitle = JsonParser.string(obj, "eng_title"),
                ruTitle = JsonParser.string(obj, "ru_title"),
                chTitle = JsonParser.string(obj, "ch_title"),
                imageUrl = imageUrl,
                isViewed = JsonParser.bool(obj, "is_viewed") == true,
                ctaLink = JsonParser.string(obj, "cta_link"),
            )
        }

        fun listFromJson(root: JsonElement?): List<StoryItem> {
            // GET /stories → {stories: [...]}. Flutter-дегідей толерантты парсинг:
            // бір элемент қате болса — қалғаны қалыпта оқылады.
            val arr = ((root as? JsonObject)?.get("stories") as? kotlinx.serialization.json.JsonArray)
                ?: return emptyList()
            return arr.mapNotNull { fromJson(it) }
        }
    }
}

/**
 * Басты беттің төлемді баннері (Flutter promo_v2 BannerModel.dart, 1:1):
 * GET /banners/main → {items: [...]} — MainBannerSlot жолдары.
 * status=pending модерацияға дейін; активтілері ғана тізімге келеді.
 */
data class MainBanner(
    val id: Long,
    val imageUrl: String,
    val ctaUrl: String?,
    val title: String?,
    val announcementId: Long?,
    val status: String?,
) {
    companion object {
        fun fromJson(root: JsonElement?): MainBanner? {
            val obj = root as? JsonObject ?: return null
            val id = JsonParser.long(obj, "id") ?: return null
            val imageUrl = JsonParser.string(obj, "image_url") ?: return null
            return MainBanner(
                id = id,
                imageUrl = imageUrl,
                ctaUrl = JsonParser.string(obj, "cta_url"),
                title = JsonParser.string(obj, "title"),
                announcementId = JsonParser.long(obj, "announcement_id"),
                status = JsonParser.string(obj, "status"),
            )
        }

        fun listFromResponse(root: JsonElement?): List<MainBanner> {
            val obj = root as? JsonObject ?: return emptyList()
            val arr = obj["items"] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
            return arr.mapNotNull { item ->
                try {
                    fromJson(item)
                } catch (_: Throwable) {
                    // Flutter-дегідей: бір баннер қате болса — ұстап қалмаймыз.
                    null
                }
            }
        }
    }
}