package com.lianghao.dots.grid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridConfigJsonTest {

    @Test
    fun `decode parses title url and keeps blank entry as empty slot`() {
        val json = """[{"title":"GitHub","url":"https://github.com"},{"title":"","url":""}]"""

        val items = GridConfigJson.decodeOrNull(json)!!

        assertEquals(GridItem("GitHub", "https://github.com"), items[0])
        assertTrue(items[1].isEmpty)
    }

    @Test
    fun `decode corrupt json returns null`() {
        assertNull(GridConfigJson.decodeOrNull("{oops"))
        assertNull(GridConfigJson.decodeOrNull(""))
    }

    @Test
    fun `decode truncates to nine items`() {
        val json = (1..12).joinToString(",", "[", "]") { """{"title":"t$it","url":"u$it"}""" }

        val items = GridConfigJson.decodeOrNull(json)!!

        assertEquals(GridConfigJson.MAX_ITEMS, items.size)
        assertEquals("t9", items.last().title)
    }

    @Test
    fun `encode round trips through decode`() {
        val items = listOf(
            GridItem("JSBridge 演示", "file:///android_asset/webpage/jsbridge_demo.html"),
            GridItem("", "")
        )

        assertEquals(items, GridConfigJson.decodeOrNull(GridConfigJson.encode(items)))
    }

    @Test
    fun `encode produces json a generic parser accepts`() {
        val text = GridConfigJson.encode(listOf(GridItem("百度", "https://www.baidu.com")))

        val parsed = JSONObject("{\"items\":$text}").getJSONArray("items")
        assertEquals("百度", parsed.getJSONObject(0).getString("title"))
    }

    @Test
    fun `grid item helpers`() {
        val item = GridItem("  Example ", "https://example.com")
        assertEquals('E', item.badgeChar)
        assertNull(GridItem("", "").badgeChar)
        assertTrue(GridItem(" ", "").isEmpty)
    }
}
