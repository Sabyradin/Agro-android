package com.agroland.core.network.json

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonParserTest {

    private val obj = buildJsonObject {
        put("id", 42)
        put("id_str", "777")
        put("name", "Жарнама")
        put("price", "1250000.5")
        put("price_num", 990.0)
        put("flag_true", true)
        put("flag_str", "true")
        put("flag_bad", "yes")
        put("null_field", kotlinx.serialization.json.JsonNull)
        put("items", kotlinx.serialization.json.buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(1)); add(kotlinx.serialization.json.JsonPrimitive(2)) })
        put("single_item", buildJsonObject { put("x", 1) })
        put("nested", buildJsonObject { put("deep", "value") })
    }

    @Test
    fun `int from number`() {
        assertEquals(42, JsonParser.int(obj, "id"))
    }

    @Test
    fun `int from string`() {
        assertEquals(777, JsonParser.int(obj, "id_str"))
    }

    @Test
    fun `int from garbage is null`() {
        assertNull(JsonParser.int(obj, "name"))
    }

    @Test
    fun `double accepts both forms`() {
        assertEquals(1250000.5, JsonParser.double(obj, "price")!!, 0.001)
        assertEquals(990.0, JsonParser.double(obj, "price_num")!!, 0.001)
    }

    @Test
    fun `bool accepts native and string`() {
        assertEquals(true, JsonParser.bool(obj, "flag_true"))
        assertEquals(true, JsonParser.bool(obj, "flag_str"))
        assertNull(JsonParser.bool(obj, "flag_bad"))
    }

    @Test
    fun `null and missing fields are null`() {
        assertNull(JsonParser.string(obj, "null_field"))
        assertNull(JsonParser.string(obj, "missing"))
        assertNull(JsonParser.int(obj, "missing"))
    }

    @Test
    fun `array or single object becomes list`() {
        assertEquals(2, JsonParser.arrayOrSingle(obj, "items").size)
        assertEquals(1, JsonParser.arrayOrSingle(obj, "single_item").size)
        assertEquals(0, JsonParser.arrayOrSingle(obj, "missing").size)
    }

    @Test
    fun `nested object access`() {
        assertEquals("value", JsonParser.string(JsonParser.obj(obj, "nested"), "deep"))
    }

    @Test
    fun `parseObject returns null on garbage`() {
        assertNull(JsonParser.parseObject("not json"))
        assertNull(JsonParser.parseObject(null))
        assertNull(JsonParser.parseObject(""))
        assertTrue(JsonParser.parseObject("""{"a":1}""")!!.containsKey("a"))
    }

    @Test
    fun `string primitive`() {
        assertEquals("Жарнама", JsonParser.string(obj, "name"))
    }
}