package com.agroland.feature.china.data

import com.agroland.core.l10n.AppLocale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChinaParser кешірімді парсингі — MercuryX жауаптарының барлық variantтары:
 * names JSON-строка, has_children `{cnt}`, image string/array, парақтау
 * әдепкілері, cart/order ораулары, china_order_id нормализациясы.
 */
class ChinaParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    // ── names + локализация ──

    @Test
    fun `names JSON-строка да объект сияқты ашылады`() {
        val element = Json.parseToJsonElement(
            """{"names": "{\"cn\": \"拖拉机\", \"ru\": \"Трактор\", \"en\": \"Tractor\"}"}""",
        )
        val names = ChinaParser.parseNames(element.jsonObject["names"])
        assertEquals("Трактор", names["ru"])
        assertEquals("拖拉机", names["cn"])
        // zh → cn кілті, fallback ru→kk→en→cn.
        assertEquals("Трактор", ChinaParser.localizedName(names, AppLocale.RU))
        assertEquals("Трактор", ChinaParser.localizedName(names, AppLocale.KK))
        assertEquals("Tractor", ChinaParser.localizedName(names, AppLocale.EN))
        assertEquals("拖拉机", ChinaParser.localizedName(names, AppLocale.ZH))
    }

    @Test
    fun `бос names → бос map және em-dash fallback`() {
        val names = ChinaParser.parseNames(Json.parseToJsonElement("42"))
        assertTrue(names.isEmpty())
        assertEquals("—", ChinaParser.localizedName(names, AppLocale.KK))
    }

    // ── Категориялар ──

    @Test
    fun `категория has_children cnt объекті мен image массивын ашады`() {
        val root = obj(
            """
            {"data": [
              {"id": 1, "names": {"ru": "Техника"}, "has_children": {"cnt": 3},
               "products_count": 7, "entity": {"image": "https://img/a.svg"}},
              {"id": 2, "names": {"ru": "Тұқым"}, "has_children": 0,
               "entity": {"image": ["https://img/b.png", "https://img/c.png"]}},
              {"id": 3, "names": {"ru": "Тыңайтқыш"}, "has_children": false},
              "не объект"
            ]}
            """,
        )
        val categories = ChinaParser.parseCategories(root)
        assertEquals(3, categories.size)
        val first = categories[0]
        assertTrue(first.hasChildren)
        assertEquals(7, first.productCount)
        assertEquals("https://img/a.svg", first.imageUrl)
        // Массив → бірінші URL; has_children=0 → false.
        assertEquals("https://img/b.png", categories[1].imageUrl)
        assertTrue(!categories[1].hasChildren)
        assertTrue(!categories[2].hasChildren)
    }

    // ── Тауар тізімі (парақтау) ──

    @Test
    fun `тауар тізімі Laravel парақтау өрістерін әдепкілендіреді`() {
        val root = obj(
            """
            {"data": {"current_page": 2, "last_page": 5, "total": 41, "data": [
              {"productId": 100, "names": {"ru": "Мотоблок"}, "image": ["u1", "u2"],
               "price": 250000, "currency": "KZT", "minQty": 10},
              {"productId": 101, "names": {"ru": "Сеялка"}, "image": "u3"}
            ]}}
            """,
        )
        val page = ChinaParser.parseProducts(root)
        assertEquals(2, page.products.size)
        assertEquals(2, page.page)
        assertEquals(5, page.totalPages)
        assertEquals(41, page.total)
        assertTrue(page.canLoadMore)
        val first = page.products[0]
        assertEquals(100L, first.productId)
        assertEquals(listOf("u1", "u2"), first.images)
        assertEquals(10, first.minQty)
        // image — строка: imagesFrom жалғыз элемент тізімін береді, minQty → 1.
        assertEquals(listOf("u3"), page.products[1].images)
        assertEquals(1, page.products[1].minQty)
    }

    @Test
    fun `тауар тізімі data орауыз қуыс бетті береді`() {
        val page = ChinaParser.parseProducts(obj("""{"success": true}"""))
        assertTrue(page.products.isEmpty())
        assertTrue(!page.canLoadMore)
    }

    // ── Корзина ──

    @Test
    fun `корзина items және data-items орауларынан оқылады`() {
        val root = obj(
            """
            {"data": {"items": [
              {"id": 5, "product_id": 100, "sku_id": 9, "quantity": 3, "min_qty": 2,
               "price_at_add": 250000.5, "title_snapshot": "Мотоблок", "image_url": "u1",
               "created_at": "2026-09-01T10:00:00Z"}
            ]}}
            """,
        )
        val items = ChinaParser.parseCart(root)
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(5L, item.id)
        assertEquals(100L, item.productId)
        assertEquals(3, item.quantity)
        assertEquals(2, item.minQty)
        assertEquals(750001.5, item.lineTotal, 0.001)
    }

    @Test
    fun `корзина элементі id-сіз → id=-1 fallback`() {
        val item = ChinaParser.parseCartItem(obj("""{"product_id": 100}"""))
        assertEquals(-1L, item.id)
    }

    // ── Тапсырыстар ──

    @Test
    fun `create-order china_order_id id-ға нормализацияланады`() {
        val root = obj(
            """
            {"success": true, "data": {"china_order_id": 77, "phone": "77712345678",
             "bin": "123456789012", "status": "forwarded",
             "mercuryx_number": "MX-100"}}
            """,
        )
        val order = ChinaParser.parseCreatedOrder(root)
        assertEquals(77L, order.id)
        assertEquals(ChinaOrderStatus.FORWARDED, order.status)
        assertEquals("MX-100", order.mercuryxNumber)
    }

    @Test
    fun `тапсырыс тарихы үш орау варианттан оқылады`() {
        // data.orders
        val a = ChinaParser.parseOrders(
            obj("""{"data": {"orders": [{"id": 1, "status": "failed", "error_message": "MercuryX қатесі"}]}}"""),
        )
        assertEquals(1, a.size)
        assertEquals(ChinaOrderStatus.FAILED, a[0].status)
        // data — тікелей массив
        val b = ChinaParser.parseOrders(obj("""{"data": [{"id": 2, "status": "cancelled"}]}"""))
        assertEquals(ChinaOrderStatus.CANCELLED, b[0].status)
        // orders тамырда
        val c = ChinaParser.parseOrders(obj("""{"orders": [{"id": 3}]}"""))
        assertEquals(3L, c[0].id)
        assertEquals(ChinaOrderStatus.UNKNOWN, c[0].status)
    }

    // ── HTML тазарту + локальді өріс ──

    @Test
    fun `stripChinaHtml тегтерді алып тастап бос аралықтарды жинайды`() {
        assertEquals(
            "Сипаттама мәтіні",
            stripChinaHtml("<p>Сипаттама&nbsp; мәтіні</p>"),
        )
        assertEquals(null, stripChinaHtml("   "))
        assertEquals(null, stripChinaHtml(null))
    }

    @Test
    fun `pickChinaLocalized объекттен locale кілтін, строкадан өзін алады`() {
        val value = Json.parseToJsonElement("""{"ru": "Түс", "en": "Color"}""")
        assertEquals("Түс", pickChinaLocalized(value, AppLocale.KK)) // kk жоқ → ru fallback
        assertEquals("Color", pickChinaLocalized(value, AppLocale.EN))
        assertEquals(
            "Жай строка",
            pickChinaLocalized(Json.parseToJsonElement("\"Жай строка\""), AppLocale.RU),
        )
    }
}