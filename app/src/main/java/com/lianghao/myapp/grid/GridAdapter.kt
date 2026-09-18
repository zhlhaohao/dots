package com.lianghao.myapp.grid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lianghao.myapp.R

/**
 * 九宫格适配器：非空格子 = 首字母色块 + 标题；空格子 = 灰显占位、不响应单击。
 */
class GridAdapter(
    private var items: List<GridItem>,
    private val onCellClick: (GridItem) -> Unit
) : RecyclerView.Adapter<GridAdapter.CellViewHolder>() {

    /** 按位置取色调色板，空格子不消费（灰显）。 */
    private val palette = intArrayOf(
        R.color.grid_c0, R.color.grid_c1, R.color.grid_c2,
        R.color.grid_c3, R.color.grid_c4, R.color.grid_c5,
        R.color.grid_c6, R.color.grid_c7, R.color.grid_c8
    )

    fun submit(newItems: List<GridItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grid_cell, parent, false)
        return CellViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val item = items[position]
        if (item.isEmpty) {
            holder.bindEmpty()
            holder.itemView.setOnClickListener(null)
        } else {
            holder.bind(item, ContextCompat.getColor(holder.itemView.context, palette[position % palette.size]))
            holder.itemView.setOnClickListener { onCellClick(item) }
        }
    }

    class CellViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val badge: TextView = view.findViewById(R.id.tvBadge)
        private val title: TextView = view.findViewById(R.id.tvCellTitle)
        private val body: View = view.findViewById(R.id.cellBody)

        fun bind(item: GridItem, color: Int) {
            badge.visibility = View.VISIBLE
            badge.text = item.badgeChar?.toString()
            badge.setBackgroundColor(color)
            title.text = item.title
            body.alpha = 1f
        }

        fun bindEmpty() {
            badge.visibility = View.INVISIBLE
            title.text = ""
            body.alpha = 0.35f
        }
    }
}
