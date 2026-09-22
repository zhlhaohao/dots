package com.lianghao.myapp.grid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridCellEditDialogTest {

    @Test
    fun `blank url is valid (empty slot path)`() {
        assertTrue(GridCellEditDialog.isValidUrl(""))
        assertTrue(GridCellEditDialog.isValidUrl("   "))
    }

    @Test
    fun `http https and file urls are valid`() {
        assertTrue(GridCellEditDialog.isValidUrl("https://github.com"))
        assertTrue(GridCellEditDialog.isValidUrl("http://example.com/path?q=1"))
        assertTrue(GridCellEditDialog.isValidUrl("file:///android_asset/webpage/jsbridge_demo.html"))
    }

    @Test
    fun `other schemes and malformed urls are rejected`() {
        assertFalse(GridCellEditDialog.isValidUrl("javascript:alert(1)"))
        assertFalse(GridCellEditDialog.isValidUrl("ftp://example.com"))
        assertFalse(GridCellEditDialog.isValidUrl("example.com"))          // 无协议
        assertFalse(GridCellEditDialog.isValidUrl("https://a b.com"))     // 含空格
        assertFalse(GridCellEditDialog.isValidUrl("intent://x#y"))        // intent 裸串
    }
}
