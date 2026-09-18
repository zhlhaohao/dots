package com.lianghao.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.lianghao.myapp.grid.GridAdapter
import com.lianghao.myapp.grid.GridCellEditDialog
import com.lianghao.myapp.grid.GridConfigStore
import com.lianghao.myapp.grid.GridItem

/**
 * 九宫格启动台（术语见 CONTEXT.md）：3×3 格子网，单击非空格子打开其 URL。
 * 配置读取与落盘走 GridConfigStore（用户配置为唯一事实来源，见 docs/adr/0002）。
 *
 * 手势：单击打开；长按静止松手弹「格子编辑」；长按拖动排序，松手持久化（工单04）。
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var gridAdapter: GridAdapter
    private lateinit var configStore: GridConfigStore
    private var gridItems: List<GridItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        title = getString(R.string.dashboard_title)

        configStore = GridConfigStore(this)
        gridAdapter = GridAdapter(
            items = emptyList(),
            onCellClick = { item -> WebViewActivity.start(this, item.url) },
            onCellLongClick = { position -> showEditDialog(position) }
        )

        findViewById<RecyclerView>(R.id.gridView).apply {
            layoutManager = GridLayoutManager(this@DashboardActivity, GridConfigStore.GRID_SPAN)
            adapter = gridAdapter
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                ItemTouchHelper.START or ItemTouchHelper.END, 0)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                gridAdapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                gridAdapter.notifyItemMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = false // 长按由自管手势分发，避免双触发

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // 松手落位：新顺序持久化（单一数据源 gridAdapter.currentItems）
                gridItems = gridAdapter.currentItems
                configStore.save(gridItems)
            }
        }).also { helper ->
            helper.attachToRecyclerView(findViewById<RecyclerView>(R.id.gridView))
            gridAdapter.dragStarter = { viewHolder -> helper.startDrag(viewHolder) }
        }

        gridItems = configStore.load()
        gridAdapter.submit(gridItems)
    }

    private fun showEditDialog(position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        val current = gridItems.getOrNull(position) ?: return
        GridCellEditDialog.show(
            activity = this,
            item = current,
            onSave = { updated ->
                gridItems = gridItems.toMutableList().also { it[position] = updated }
                configStore.save(gridItems)
                gridAdapter.submit(gridItems)
            },
            onRestoreDefaults = { restoreDefaults() }
        )
    }

    /** 一键恢复默认：回写出厂配置并立即刷新（工单03）。 */
    private fun restoreDefaults() {
        gridItems = configStore.factoryConfig()
        configStore.save(gridItems)
        gridAdapter.submit(gridItems)
    }
}
