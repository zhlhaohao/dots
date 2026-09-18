package com.lianghao.myapp.grid

import android.content.Context
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 九宫格配置仓库：出厂配置（assets）→ 用户配置（filesDir）的读、拷贝与保存。
 *
 * 语义（见 docs/adr/0002-grid-config-persistence.md）：
 * - 首次启动（用户配置文件不存在）把出厂配置拷贝落盘为用户配置；
 * - 用户配置是唯一事实来源，编辑一律写回 [USER_CONFIG_FILE]；
 * - 用户配置缺失或损坏时回退出厂配置渲染，但**不**自动覆盖写回（保留现场，恢复动作留给用户显式触发）；
 * - 不足九格的配置读取时补齐空格子，保证网格形状完整。
 */
class GridConfigStore(private val context: Context) {

    companion object {
        const val FACTORY_ASSET = "grid_items.json"
        const val USER_CONFIG_FILE = "grid_items.json"
        const val GRID_SIZE = GridConfigJson.MAX_ITEMS
        const val GRID_SPAN = 3
    }

    private val userConfigFile: File
        get() = File(context.filesDir, USER_CONFIG_FILE)

    /** 读取用户配置；首次启动落盘出厂配置，缺失或损坏时回退出厂配置。结果恒为 9 格（不足补空格子）。 */
    fun load(): List<GridItem> {
        val user = readUserConfig()
        if (user == null) {
            // 首次启动或用户配置损坏：以出厂配置渲染；仅「文件不存在」（首次启动）时拷贝落盘，
            // 损坏时不覆盖写回，保留现场供用户排查。
            val factory = factoryConfig()
            if (!userConfigFile.exists()) {
                runCatching { write(factory) }
            }
            return factory
        }
        return padToGrid(user)
    }

    /** 保存用户配置（编辑/排序/恢复默认的统一落盘入口）。 */
    fun save(items: List<GridItem>) {
        write(padToGrid(items))
    }

    /** 出厂配置：随包内置，读失败时返回全空格子（不崩溃）。 */
    fun factoryConfig(): List<GridItem> =
        padToGrid(runCatching {
            context.assets.open(FACTORY_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("").let { GridConfigJson.decodeOrNull(it) ?: emptyList() })

    private fun readUserConfig(): List<GridItem>? {
        val file = userConfigFile
        if (!file.isFile) return null
        val text = runCatching { file.readText(StandardCharsets.UTF_8) }.getOrNull() ?: return null
        return GridConfigJson.decodeOrNull(text)
    }

    private fun write(items: List<GridItem>) {
        userConfigFile.writeText(GridConfigJson.encode(items), StandardCharsets.UTF_8)
    }

    private fun padToGrid(items: List<GridItem>): List<GridItem> {
        val padded = items.take(GRID_SIZE).toMutableList()
        while (padded.size < GRID_SIZE) padded.add(GridItem("", ""))
        return padded.toList()
    }
}
