package com.lianghao.myapp.grid

import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.lianghao.myapp.R

/**
 * 「格子编辑」对话框：改标题与 URL；两项均留空保存 = 清空为空格子。
 * 对话框内提供「恢复默认」入口（带确认，见工单03）。
 *
 * URL 规则：留空合法（空格子路径）；非空必须是 http(s) 或 file 前缀，避免产生打不开的入口。
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
        onRestoreDefaults: () -> Unit = {}
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_grid_cell, null)
        val etTitle = view.findViewById<EditText>(R.id.etCellTitle)
        val etUrl = view.findViewById<EditText>(R.id.etCellUrl)
        val tvError = view.findViewById<TextView>(R.id.tvUrlError)

        etTitle.setText(item.title)
        etUrl.setText(item.url)

        AlertDialog.Builder(activity)
            .setTitle(R.string.grid_edit_title)
            .setView(view)
            .setPositiveButton(R.string.grid_edit_save) { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newUrl = etUrl.text.toString().trim()
                if (!isValidUrl(newUrl)) {
                    // 校验失败不关框：重新弹出保留输入（AlertDialog 点击即dismiss，故重新show）
                    showAgain(activity, newTitle, newUrl, true, onSave, onRestoreDefaults)
                    return@setPositiveButton
                }
                onSave(GridItem(newTitle, newUrl))
            }
            .setNegativeButton(R.string.grid_edit_cancel, null)
            .setNeutralButton(R.string.grid_edit_restore) { _, _ -> confirmRestore(activity, onRestoreDefaults) }
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

    private fun showAgain(
        activity: androidx.appcompat.app.AppCompatActivity,
        title: String,
        url: String,
        showErr: Boolean,
        onSave: (GridItem) -> Unit,
        onRestoreDefaults: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_grid_cell, null)
        val etTitle = view.findViewById<EditText>(R.id.etCellTitle)
        val etUrl = view.findViewById<EditText>(R.id.etCellUrl)
        val tvError = view.findViewById<TextView>(R.id.tvUrlError)
        etTitle.setText(title)
        etUrl.setText(url)
        tvError.visibility = if (showErr) TextView.VISIBLE else TextView.GONE

        AlertDialog.Builder(activity)
            .setTitle(R.string.grid_edit_title)
            .setView(view)
            .setPositiveButton(R.string.grid_edit_save) { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newUrl = etUrl.text.toString().trim()
                if (!isValidUrl(newUrl)) {
                    showAgain(activity, newTitle, newUrl, true, onSave, onRestoreDefaults)
                    return@setPositiveButton
                }
                onSave(GridItem(newTitle, newUrl))
            }
            .setNegativeButton(R.string.grid_edit_cancel, null)
            .setNeutralButton(R.string.grid_edit_restore) { _, _ -> confirmRestore(activity, onRestoreDefaults) }
            .show()
    }
}
