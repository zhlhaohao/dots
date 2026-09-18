package com.lianghao.myapp.grid

import org.json.JSONArray
import org.json.JSONException

/**
 * 九宫格配置与 JSON 之间的纯编解码（无 IO，可 JVM 单测）。
 *
 * 约定：
 * - 合法配置 = 最多 [MAX_ITEMS] 个条目的 JSON 数组，多出的截断；
 * - 返回 null 表示文本不是合法配置（调用方据此回退出厂配置）；
 * - 出厂配置不合法时宁可空列表也不抛异常（九宫格全部灰显，应用不崩溃）。
 */
object GridConfigJson {

    const val MAX_ITEMS = 9

    /** 解析失败（文本不是合法的配置数组）返回 null；合法但空返回空列表。 */
    fun decodeOrNull(jsonText: String): List<GridItem>? {
        val items = mutableListOf<GridItem>()
        return try {
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                if (i >= MAX_ITEMS) break
                val obj = array.optJSONObject(i) ?: continue
                items.add(GridItem(obj.optString("title"), obj.optString("url")))
            }
            items
        } catch (e: JSONException) {
            null
        }
    }

    fun encode(items: List<GridItem>): String {
        val array = JSONArray()
        items.take(MAX_ITEMS).forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("title", item.title)
            obj.put("url", item.url)
            array.put(obj)
        }
        return array.toString()
    }
}
