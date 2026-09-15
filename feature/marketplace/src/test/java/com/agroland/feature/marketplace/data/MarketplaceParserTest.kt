package com.agroland.feature.marketplace.data

import com.agroland.feature.location.data.SelectedLocation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MarketplaceParser кешірімді парсингі + AnnouncementFilter query map. */
class MarketplaceParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `лента беті items total page pages оқиды`() {
        val root = obj(
            """
            {
              "items": [
                {
                  "id": 1,
                  "title": "Бидай 2-сұрып",
                  "price": 125000,
                  "currency": "KZT",
                  "city": "Астана",
                  "image_url": "/media/a.jpg",
                  "is_vip": true,
                  "negotiable": true,
                  "views_count": "42"
                },
                {"id": 2, "title": "Трактор", "price": "8500000.5", "is_hot": true}
              ],
              "total": "57",
              "page": 2,
              "pages": 3
            }
            """.trimIndent(),
        )
        val page = MarketplaceParser.parseAnnouncementPage(root)

        assertEquals(2, page.items.size)
        assertEquals(57, page.total)
        assertEquals(2, page.page)
        assertEquals(3, page.pages)
        assertTrue(page.hasMore)
        val first = page.items[0]
        assertEquals(1L, first.id)
        assertEquals("Бидай 2-сұрып", first.title)
        assertEquals(125000.0, first.price!!, 0.001)
        assertTrue(first.isVip)
        assertTrue(first.negotiable)
        // int string түрінде келсе де оқылады.
        assertEquals(42, first.viewsCount)
        // double string түрінде келсе де оқылады.
        assertEquals(8500000.5, page.items[1].price!!, 0.001)
        assertTrue(page.items[1].isHot)
    }

    @Test
    fun `жарнама camelCase және hot alias қолдауы`() {
        val item = MarketplaceParser.parseAnnouncement(
            obj(
                """
                {
                  "id": 9,
                  "title": "Жем 20 кг",
                  "createdAt": "2026-09-01T10:00:00Z",
                  "imageUrl": "/media/x.jpg",
                  "isFavorite": "true",
                  "hot": 1,
                  "image_urls": ["/media/1.jpg", "/media/2.jpg", ""]
                }
                """.trimIndent(),
            ),
        )!!
        assertEquals("2026-09-01T10:00:00Z", item.createdAt)
        assertTrue(item.isFavorite)
        assertTrue(item.isHot)
        // imageUrls бос элементтерден тазартылады.
        assertEquals(listOf("/media/1.jpg", "/media/2.jpg"), item.imageUrls)
    }

    @Test
    fun `толық деталь seller contacts similar additional оқиды`() {
        val root = obj(
            """
            {
              "id": 5,
              "title": "Сүт өткізу",
              "description": "Толық сипаттама",
              "price_includes_vat": true,
              "seller": {
                "id": 77,
                "name": "Асан",
                "rating": 4.7,
                "phone_number": "+77770001122",
                "is_vip_seller": true
              },
              "contact_numbers": ["+77771112233", {"phone_number": "+77774445566"}],
              "category_id": 4,
              "subcategory_id": 9,
              "additional_announcements": [{"id": 6, "title": "Басқа жарнама"}],
              "similar_announcements": [{"id": 7, "title": "Ұқсас жарнама"}]
            }
            """.trimIndent(),
        )
        val detail = MarketplaceParser.parseFullAnnouncement(root)!!

        assertEquals("Толық сипаттама", detail.description)
        assertTrue(detail.priceIncludesVat)
        assertEquals("Асан", detail.seller?.name)
        assertEquals(4.7, detail.seller?.rating!!, 0.001)
        assertTrue(detail.seller?.isVipSeller!!)
        assertEquals(listOf("+77771112233", "+77774445566"), detail.contactNumbers)
        assertEquals(4, detail.categoryId)
        assertEquals(9, detail.subcategoryId)
        assertEquals(1, detail.additional.size)
        assertEquals(1, detail.similar.size)
    }

    @Test
    fun `категория локализацияланған атаулары мен fallbacks`() {
        val category = MarketplaceParser.parseCategory(
            obj(
                """
                {"id": 3, "name_kz": "Дәнді дақылдар", "name_ru": "Зерновые",
                 "name_en": "Grains", "name_ch": "谷物", "announcement_count": "12"}
                """.trimIndent(),
            ),
        )!!
        assertEquals("Дәнді дақылдар", category.localizedName("kk-KZ"))
        assertEquals("Зерновые", category.localizedName("ru"))
        assertEquals("Grains", category.localizedName("en-US"))
        assertEquals("谷物", category.localizedName("zh"))
        assertEquals(12, category.announcementCount)

        val fallback = MarketplaceParser.parseCategory(obj("""{"id": 4, "name": "Басқа"}"""))!!
        assertEquals("Басқа", fallback.localizedName(null))
        assertNull(MarketplaceParser.parseCategory(obj("""{"name": "id жоқ"}""")))
    }

    @Test
    fun `grouped 6 bucket оқиды`() {
        val root = obj(
            """
            {
              "crops": [{"id": 1}],
              "livestock": [{"id": 2}],
              "products": [],
              "technology": [{"id": 3}, {"id": 4}],
              "services": [{"id": 5}],
              "other": []
            }
            """.trimIndent(),
        )
        val grouped = MarketplaceParser.parseGroupedCategories(root)

        assertEquals(6, grouped.size)
        assertEquals(1, grouped["crops"]?.size)
        assertEquals(0, grouped["products"]?.size)
        assertEquals(2, grouped["technology"]?.size)
    }

    @Test
    fun `іздеу ұсыныстары items немесе suggestions түбірінен`() {
        val root = obj(
            """
            {"suggestions": [{"title": "Бидай", "category": "Дақылдар",
             "category_id": 1, "sub_category": "Күздік", "sub_category_id": 2,
             "location": "Астана"}]}
            """.trimIndent(),
        )
        val list = MarketplaceParser.parseSuggestions(root)

        assertEquals(1, list.size)
        assertEquals("Бидай", list[0].title)
        assertEquals("Дақылдар", list[0].category)
        assertEquals(1, list[0].categoryId)
        assertEquals(2, list[0].subCategoryId)
        assertEquals("Астана", list[0].location)
    }

    @Test
    fun `таңдаулы күйі әр түрлі кілттерден оқиды`() {
        assertTrue(MarketplaceParser.parseFavoriteStatus(obj("""{"is_favorite": true}""")))
        assertTrue(MarketplaceParser.parseFavoriteStatus(obj("""{"favorite": "true"}""")))
        assertFalse(MarketplaceParser.parseFavoriteStatus(obj("""{}""")))
    }

    @Test
    fun `сүзгі query map тек толтырылған өрістерді жібереді`() {
        val default = AnnouncementFilter().toQueryMap(page = 1, limit = 20)
        assertEquals(mapOf("page" to "1", "limit" to "20"), default)

        val filter = AnnouncementFilter(
            query = " бидай ",
            categoryId = 3,
            subcategoryId = 7,
            minPrice = 10000.0,
            maxPrice = 500000.0,
            negotiable = true,
            sort = FilterSort.PRICE_DESC,
        )
        val map = filter.toQueryMap(page = 2, limit = 20)
        assertEquals("бидай", map["q"])
        assertEquals("3", map["category_id"])
        assertEquals("7", map["subcategory_id"])
        assertEquals("10000.0", map["min_price"])
        assertEquals("500000.0", map["max_price"])
        assertEquals("true", map["negotiable"])
        assertEquals("desc", map["sort_by_price"])
        assertEquals("2", map["page"])
        assertEquals("20", map["limit"])
        assertFalse(map.containsKey("is_vip"))
        assertFalse(map.containsKey("order_random"))
    }

    @Test
    fun `сүзгі VIP және random реттеу параметрлері`() {
        val vip = AnnouncementFilter(isVip = true, orderRandom = true, typeAd = "rec")
        val map = vip.toQueryMap(page = 1, limit = 20)
        assertEquals("true", map["is_vip"])
        assertEquals("true", map["order_random"])
        assertEquals("rec", map["type_ad"])

        val dateAsc = AnnouncementFilter(sort = FilterSort.DATE_ASC)
        assertEquals("asc", dateAsc.toQueryMap(1, 20)["sort_by_date"])
        val plain = AnnouncementFilter(sort = FilterSort.DEFAULT)
        assertFalse(plain.toQueryMap(1, 20).containsKey("sort_by_date"))
    }

    @Test
    fun `сүзгі локация каталог ID-лерін жібереді — Фаза 7`() {
        val filter = AnnouncementFilter(
            location = SelectedLocation(
                countryId = 1,
                regionId = 10,
                districtId = 100,
                latitude = 51.1282,
                longitude = 71.4307,
            ),
        )
        val map = filter.toQueryMap(page = 1, limit = 20)
        assertEquals("1", map["country_id"])
        assertEquals("10", map["region_id"])
        assertEquals("100", map["district_id"])

        // Локация бар фильтр әдепкі емес.
        assertFalse(filter.isDefault())

        // Толық емес локация: тек country.
        val partial = AnnouncementFilter(location = SelectedLocation(countryId = 1))
        val partialMap = partial.toQueryMap(1, 20)
        assertEquals("1", partialMap["country_id"])
        assertFalse(partialMap.containsKey("region_id"))
        assertFalse(partialMap.containsKey("district_id"))

        // Локация жоқ фильтр ешқандай локация параметрін жібермейді.
        val noLocation = AnnouncementFilter().toQueryMap(1, 20)
        assertFalse(noLocation.containsKey("country_id"))
        assertTrue(AnnouncementFilter().isDefault())
    }
}