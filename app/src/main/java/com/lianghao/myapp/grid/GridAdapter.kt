package com.lianghao.myapp.grid

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lianghao.myapp.R

/**
 * 九宫格适配器：非空格子 = 首字母色块 + 标题；空格子 = 灰显占位、不响应单击。
 *
 * 手势分工（工单04）：短按单击打开；长按后拖动（超 touch slop）启动拖拽排序；
 * 长按静止松手弹「格子编辑」对话框。同一长按手势不会同时触发两者。
 */
class GridAdapter(
    private var items: List<GridItem>,
    private val onCellClick: (GridItem) -> Unit,
    private val onCellLongClick: (Int) -> Unit = {}
) : RecyclerView.Adapter<GridAdapter.CellViewHolder>() {

    /** 由宿主注入：长按拖动时启动 ItemTouchHelper 拖拽。 */
    var dragStarter: (RecyclerView.ViewHolder) -> Unit = {}

    /** 按位置取色调色板，空格子不消费（灰显）。 */
    private val palette = intArrayOf(
        R.color.grid_c0, R.color.grid_c1, R.color.grid_c2,
        R.color.grid_c3, R.color.grid_c4, R.color.grid_c5,
        R.color.grid_c6, R.color.grid_c7, R.color.grid_c8
    )

    val currentItems: List<GridItem> get() = items

    fun submit(newItems: List<GridItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    /** 拖拽换序（ItemTouchHelper onMove 调用）；松手后由宿主取 [currentItems] 持久化。 */
    fun moveItem(from: Int, to: Int) {
        if (from == to || from !in items.indices || to !in items.indices) return
        items = items.toMutableList().apply { add(to, removeAt(from)) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder =
        CellViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_grid_cell, parent, false),
            dragStarter,
            onCellClick,
            onCellLongClick
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val item = items[position]
        holder.positionProvider = { holder.bindingAdapterPosition }
        if (item.isEmpty) {
            holder.bindEmpty()
        } else {
            holder.bind(item, ContextCompat.getColor(holder.itemView.context, palette[position % palette.size]))
        }
    }

    class CellViewHolder(
        view: View,
        dragStarter: (RecyclerView.ViewHolder) -> Unit,
        onCellClick: (GridItem) -> Unit,
        private val onCellLongClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val badge: TextView = view.findViewById(R.id.tvBadge)
        private val title: TextView = view.findViewById(R.id.tvCellTitle)
        private val body: View = view.findViewById(R.id.cellBody)

        /** 手势回调取实时位置（拖拽/复用后 holder 位置会变）。 */
        var positionProvider: () -> Int = { RecyclerView.NO_POSITION }

        private var clickItem: GridItem? = null

        init {
            view.setOnClickListener { clickItem?.let(onCellClick) }
            view.setOnTouchListener(CellGestureHandler(view, dragStarter, this))
        }

        fun bind(item: GridItem, color: Int) {
            clickItem = item
            badge.visibility = View.VISIBLE
            badge.text = item.badgeChar?.toString()
            badge.setBackgroundColor(color)
            title.text = item.title
            body.alpha = 1f
        }

        fun bindEmpty() {
            clickItem = null
            badge.visibility = View.INVISIBLE
            title.text = ""
            body.alpha = 0.35f
        }

        private fun edit() {
            val pos = positionProvider()
            if (pos != RecyclerView.NO_POSITION) onCellLongClick(pos)
        }

        /**
         * 手势状态机：DOWN 记起点 → 长按置位 → 移动超 slop 交 ItemTouchHelper 拖拽；
         * 长按后静止松手 → 编辑。长按一旦发生即消费事件，避免系统 click 误触发。
         */
        private class CellGestureHandler(
            view: View,
            private val dragStarter: (RecyclerView.ViewHolder) -> Unit,
            private val holder: CellViewHolder
        ) : View.OnTouchListener {

            private val slop = ViewConfiguration.get(view.context).scaledTouchSlop
            private var longPressed = false
            private var dragStarted = false
            private var downX = 0f
            private var downY = 0f

            private val detector = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    longPressed = true
                }
            })

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                detector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        longPressed = false
                        dragStarted = false
                        downX = event.x
                        downY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> if (longPressed && !dragStarted) {
                        val dx = event.x - downX
                        val dy = event.y - downY
                        if (dx * dx + dy * dy > slop * slop) {
                            dragStarted = true
                            dragStarter(holder)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (longPressed && !dragStarted) holder.edit()
                        longPressed = false
                        dragStarted = false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        longPressed = false
                        dragStarted = false
                    }
                }
                return longPressed || dragStarted
            }
        }
    }
}
