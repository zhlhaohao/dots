package com.lianghao.dots

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.lianghao.dots.grid.GridAdapter
import com.lianghao.dots.grid.GridCellEditDialog
import com.lianghao.dots.grid.GridConfigStore
import com.lianghao.dots.grid.GridItem

/**
 * 格子网启动台（术语见 CONTEXT.md）：3 列纵向滚动，条目上限 18（见 docs/adr/0003）。
 * 单击非空格子打开其 URL；配置读取与落盘走 GridConfigStore（用户配置为唯一事实来源，见 docs/adr/0002）。
 *
 * 手势：单击打开；长按格子静止松手弹「格子编辑」；长按拖动排序；长按留白区弹「新建格子」。
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
            attachBlankLongPress(this)
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

    /**
     * 留白区长按 → 新建条目（ADR-0003）。
     *
     * 「留白区」= 没落在任何 item 上的触摸（列表底 padding，clipToPadding=false 滚动可见）。
     * 实现：OnItemTouchListener 从 DOWN 起收到完整事件流，喂给 GestureDetector；
     * 长按触发时校验落点不在 item 上（findChildViewUnder == null）才弹新建框。
     * 不消费事件（返回 false），正常滚动/点击不受影响；手势移动超过 slop 时
     * GestureDetector 自动取消长按计时，与滚动天然互斥。
     */
    private fun attachBlankLongPress(recyclerView: RecyclerView) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (recyclerView.findChildViewUnder(e.x, e.y) == null) showCreateDialog()
            }
        })
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                detector.onTouchEvent(e)
                return false
            }
        })
    }

    /** 满员（实条目 ≥18，空格子不计）则提示；否则弹新建框，保存后落位：填第一个空格子，无洞追加末尾（ADR-0003）。
     *  满员判定看实条目数而非列表长度——删除留洞后列表可能仍是 18 条，此时必须允许填洞。 */
    private fun showCreateDialog() {
        if (gridItems.count { !it.isEmpty } >= GridConfigStore.MAX_ITEMS) {
            Toast.makeText(this, R.string.grid_full_prompt, Toast.LENGTH_SHORT).show()
            return
        }
        GridCellEditDialog.show(
            activity = this,
            item = GridItem("", ""),
            onSave = { created ->
                gridItems = gridItems.toMutableList().apply {
                    val slot = indexOfFirst { it.isEmpty }
                    if (slot >= 0) this[slot] = created else add(created)
                }
                persistAndRefresh()
            }
        )
    }

    private fun showEditDialog(position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        val current = gridItems.getOrNull(position) ?: return
        GridCellEditDialog.show(
            activity = this,
            item = current,
            onSave = { updated ->
                gridItems = gridItems.toMutableList().also { it[position] = updated }
                persistAndRefresh()
            },
            onRestoreDefaults = { restoreDefaults() },
            onDelete = { deleteItem(position) }
        )
    }

    /** 删除条目：就地留洞（该格变空格子，其余位置不动，见 ADR-0003）。 */
    private fun deleteItem(position: Int) {
        val item = gridItems.getOrNull(position) ?: return
        if (item.isEmpty) return
        gridItems = gridItems.toMutableList().also { it[position] = GridItem("", "") }
        persistAndRefresh()
        Toast.makeText(this, getString(R.string.grid_deleted_toast, item.title), Toast.LENGTH_SHORT).show()
    }

    /** 一键恢复默认：回写出厂配置并立即刷新（工单03）。 */
    private fun restoreDefaults() {
        gridItems = configStore.factoryConfig()
        persistAndRefresh()
    }

    private fun persistAndRefresh() {
        configStore.save(gridItems)
        gridAdapter.submit(gridItems)
    }
}
