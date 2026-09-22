package com.lianghao.dots.grid

/**
 * 九宫格的格子条目（术语见 CONTEXT.md）。
 *
 * title 与 url 同时为空即为「空格子」：灰显占位、不响应单击、长按可编辑填充。
 */
data class GridItem(val title: String, val url: String) {

    val isEmpty: Boolean get() = title.isBlank() && url.isBlank()

    /** 格子色块内展示的首字：非空取标题首个字符，空格子返回 null（由 UI 决定占位样式）。 */
    val badgeChar: Char? get() = if (isEmpty) null else title.trim().firstOrNull()
}
