/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.windows.controls

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import kotlinx.cinterop.*
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
class ListControl(
    parent: Window,
    title: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : CommonControl(parent) {
    override val windowStyle: Int = WS_VISIBLE or WS_CHILD or LVS_REPORT or LVS_EDITLABELS
    override val windowClassName: String = WC_LISTVIEWA

    private val columnTitle = title

    init {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        createColumn()
    }

    private fun createColumn() {
        memScoped {
            val listViewColumn = cValue<LVCOLUMNW> {
                mask = (LVCF_WIDTH or LVCF_TEXT or LVCF_SUBITEM or LVCF_FMT).toUInt()
                cx = width
                pszText = columnTitle.wcstr.ptr
                iSubItem = 0
                fmt = LVCFMT_FIXED_WIDTH
            }

            SendMessageW(
                windowHandle,
                LVM_INSERTCOLUMN.toUInt(),
                0u,
                listViewColumn.ptr.rawValue.toLong()
            )
        }
    }

    private var numRows: Int = 0

    fun add(entry: String) {
        memScoped {
            val listViewItem = cValue<LVITEMW> {
                mask = LVIF_TEXT.toUInt()
                pszText = entry.wcstr.ptr
                iItem = numRows
                iSubItem = 0
            }

            SendMessageW(
                windowHandle,
                LVM_INSERTITEMW.toUInt(),
                0u,
                listViewItem.ptr.rawValue.toLong()
            )

            numRows += 1
        }
    }

    fun clear() {
        SendMessageW(
            windowHandle,
            LVM_DELETEALLITEMS.toUInt(),
            0u,
            0,
        )
    }

    val onBackspace = mutableSetOf<() -> Unit>()

    override fun handleMessage(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        if (msg == WM_KEYDOWN.toUInt() && (wParam == VK_BACK.toULong() || wParam == VK_DELETE.toULong())) {
            onBackspace.forEach { it() }
            return 0
        }

        return super.handleMessage(msg, wParam, lParam)
    }

    @OptIn(ExperimentalForeignApi::class)
    val selectedIndex: Int
        get() = SendMessageW(
            windowHandle,
            LVM_GETNEXTITEM.toUInt(),
            (-1).toULong(),
            LVNI_SELECTED.toLong(),
        ).toInt()

    companion object {
        init {
            loadCommonControls(ICC_LISTVIEW_CLASSES)
        }
    }
}
