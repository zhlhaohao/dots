package com.lianghao.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.lianghao.myapp.grid.GridAdapter
import com.lianghao.myapp.grid.GridConfigStore

/**
 * 九宫格启动台（术语见 CONTEXT.md）：3×3 格子网，单击非空格子打开其 URL。
 * 配置读取与落盘走 GridConfigStore（用户配置为唯一事实来源，见 docs/adr/0002）。
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var gridAdapter: GridAdapter
    private lateinit var configStore: GridConfigStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        title = getString(R.string.dashboard_title)

        configStore = GridConfigStore(this)
        gridAdapter = GridAdapter(emptyList()) { item ->
            WebViewActivity.start(this, item.url)
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.gridView).apply {
            layoutManager = GridLayoutManager(this@DashboardActivity, GridConfigStore.GRID_SPAN)
            adapter = gridAdapter
        }

        gridAdapter.submit(configStore.load())
    }
}
