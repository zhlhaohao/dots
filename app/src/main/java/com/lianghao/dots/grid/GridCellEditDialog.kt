package com.lianghao.dots.grid

import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.lianghao.dots.R

/**
 * 「格子编辑/新建格子」对话框：改标题与 URL。
 *
 * - 非空格子：标题「编辑格子」，视图内提供「删除此格子」（二次确认显示条目名，见 ADR-0003）；
 * - 空格子：标题「新建格子」，隐藏删除入口（没有可删的内容）；
 * - 校验：URL 留空合法、格式非法与「标题 URL 全留空」均拒绝保存并回显错误
 *   （2026-09-22 起废弃「全留空保存=清空」旧路径，清空一律走删除条目）；
 * - 对话框内保留「恢复默认」入口（带确认）。
 */
object GridCellEditDialog {

    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return true
        return (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) &&
            !url.contains(' ')
    }

    fun show(
        activity: androidx.appcompat.app.AppCompatActivity,
        item: GridItem,
        onSave: (GridItem) -> Unit,
        onRestoreDefaults: () -> Unit = {},
        onDelete: () -> Unit = {}
    ) = showInternal(activity, item.title, item.url, item.isEmpty, onSave, onRestoreDefaults, onDelete)

    /** 校验失败不关框：AlertDialog 点按钮即 dismiss，故带着已输入内容与错误提示重新弹出。 */
    private fun showInternal(
        activity: androidx.appcompat.app.AppCompatActivity,
        title: String,
        url: String,
        isNewCell: Boolean,
        onSave: (GridItem) -> Unit,
        onRestoreDefaults: () -> Unit,
        onDelete: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_grid_cell, null)
        val etTitle = view.findViewById<EditText>(R.id.etCellTitle)
        val etUrl = view.findViewById<EditText>(R.id.etCellUrl)
        val tvUrlError = view.findViewById<TextView>(R.id.tvUrlError)
        val tvBlankError = view.findViewById<TextView>(R.id.tvBlankError)
        val btnDelete = view.findViewById<TextView>(R.id.btnDeleteCell)

        etTitle.setText(title)
        etUrl.setText(url)
        btnDelete.visibility = if (isNewCell) TextView.GONE else TextView.VISIBLE

        fun trySave() {
            val newTitle = etTitle.text.toString().trim()
            val newUrl = etUrl.text.toString().trim()
            val urlBad = !isValidUrl(newUrl)
            val bothBlank = newTitle.isBlank() && newUrl.isBlank()
            if (urlBad || bothBlank) {
                showInternal(activity, newTitle, newUrl, isNewCell, onSave, onRestoreDefaults, onDelete)
            } else {
                onSave(GridItem(newTitle, newUrl))
            }
        }

        btnDelete.setOnClickListener {
            confirmDelete(activity, title.ifBlank { url }, onDelete)
        }

        AlertDialog.Builder(activity)
            .setTitle(if (isNewCell) R.string.grid_new_title else R.string.grid_edit_title)
            .setView(view)
            .setPositiveButton(R.string.grid_edit_save) { _, _ -> trySave() }
            .setNegativeButton(R.string.grid_edit_cancel, null)
            .setNeutralButton(R.string.grid_edit_restore) { _, _ -> confirmRestore(activity, onRestoreDefaults) }
            .show()
    }

    /** 删除的二次确认：文案显示条目名，防误删最后一道防线（ADR-0003）。 */
    private fun confirmDelete(
        activity: androidx.appcompat.app.AppCompatActivity,
        itemTitle: String,
        onDelete: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.grid_delete_confirm_title)
            .setMessage(activity.getString(R.string.grid_delete_confirm_message, itemTitle))
            .setPositiveButton(R.string.grid_delete_confirm_yes) { _, _ -> onDelete() }
            .setNegativeButton(R.string.grid_edit_cancel, null)
            .show()
    }

    /** 恢复默认的二次确认：防误触清掉用户全部修改（工单03 验收项）。 */
    private fun confirmRestore(
        activity: androidx.appcompat.app.AppCompatActivity,
        onRestoreDefaults: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.grid_restore_confirm_title)
            .setMessage(R.string.grid_restore_confirm_message)
            .setPositiveButton(R.string.grid_restore_confirm_yes) { _, _ -> onRestoreDefaults() }
            .setNegativeButton(R.string.grid_edit_cancel, null)
            .show()
    }
}
