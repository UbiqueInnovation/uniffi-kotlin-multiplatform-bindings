/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.windows

import io.gitlab.trixnity.uniffi.examples.app.Win32Exception
import kotlinx.cinterop.*
import platform.windows.*

@Suppress("LeakingThis")
@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
abstract class Window(private val parent: Window? = null) : AutoCloseable {
    init {
        parent?.children?.add(this)
    }

    private val windowClassNameByKotlinClassName = this::class.qualifiedName?.replace('.', '_') ?: "DefaultWindow"
    protected open val windowClassName: String = windowClassNameByKotlinClassName
    private val subclassed: Boolean
        get() = windowClassNameByKotlinClassName != windowClassName

    protected open val windowStyle: Int = WS_OVERLAPPEDWINDOW


    var title: String = ""
        get() {
            if (windowHandleInitialized) {
                memScoped {
                    val stringLength = GetWindowTextLengthW(windowHandle)
                    field = CharArray(stringLength + 1).apply {
                        usePinned {
                            GetWindowTextW(windowHandle, it.addressOf(0).reinterpret(), stringLength + 1)
                        }
                    }.concatToString(endIndex = stringLength)
                }
            }
            return field
        }
        set(value) {
            field = value
            if (windowHandleInitialized) {
                SetWindowTextW(windowHandle, value)
            }
        }

    var x: Int = CW_USEDEFAULT
        set(value) {
            field = value
            if (windowHandleInitialized) {
                updateWindowPosition()
            }
        }

    var y: Int = CW_USEDEFAULT
        set(value) {
            field = value
            if (windowHandleInitialized) {
                updateWindowPosition()
            }
        }

    var width: Int = CW_USEDEFAULT
        set(value) {
            field = value
            if (windowHandleInitialized) {
                updateWindowPosition()
            }
        }

    var height: Int = CW_USEDEFAULT
        set(value) {
            field = value
            if (windowHandleInitialized) {
                updateWindowPosition()
            }
        }

    private var windowHandleInitialized: Boolean = false

    protected val windowHandle: HWND by lazy {
        if (!isClassRegistered(windowClassName)) {
            registerClass(windowClassName)
        }
        createWindow().apply {
            windowHandleInitialized = true
        }
    }

    protected open fun handleMessage(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        if (msg == WM_COMMAND.toUInt()) {
            val sender = children.firstOrNull {
                it.windowHandle.rawValue.toLong() == lParam
            }
            if (sender != null) {
                sender.handleCommand(wParam)
                return 0
            }
        }

        if (subclassed) {
            return DefSubclassProc(windowHandle, msg, wParam, lParam)
        }
        return DefWindowProcW(windowHandle, msg, wParam, lParam)
    }

    protected open fun handleCommand(notification: WPARAM) {}

    fun open() {
        ShowWindow(windowHandle, SW_SHOW)
        UpdateWindow(windowHandle)
        children.forEach { it.open() }
    }

    private val children = mutableListOf<Window>()

    override fun close() {
        if (subclassed) {
            memScoped {
                // TODO: GetWindowSubclass makes a linker error
                // val dataReference = alloc<DWORD_PTRVar>()
                // GetWindowSubclass(
                //     windowHandle,
                //     staticCFunction(::subclassProcedure),
                //     0uL,
                //     dataReference.ptr,
                // )
                // windowRefOf(dataReference.value.toLong())?.dispose()
            }
        } else {
            windowRefOf(windowHandle)?.dispose()
        }
        DestroyWindow(windowHandle)
        children.forEach { it.close() }
    }

    private fun createWindow(): HWND {
        @Suppress("UNUSED_PARAMETER")
        @OptIn(ExperimentalForeignApi::class)
        fun subclassProcedure(
            windowHandle: HWND?,
            msg: UINT,
            wParam: WPARAM,
            lParam: LPARAM,
            subclassId: UINT_PTR,
            referenceData: DWORD_PTR,
        ): LRESULT {
            val windowRef =
                windowRefOf(referenceData.toLong()) ?: return DefSubclassProc(windowHandle, msg, wParam, lParam)
            return windowRef.get().handleMessage(msg, wParam, lParam)
        }

        val windowRef = StableRef.create(this).asCPointer()

        val window = CreateWindowExW(
            dwExStyle = 0u,
            lpClassName = windowClassName,
            lpWindowName = title,
            dwStyle = windowStyle.toUInt(),
            X = x,
            Y = y,
            nWidth = width,
            nHeight = height,
            hWndParent = parent?.windowHandle,
            hMenu = null,
            hInstance = instanceHandle,
            lpParam = windowRef.takeIf { !subclassed },
        ) ?: throw Win32Exception()

        if (subclassed) {
            SetWindowSubclass(
                window,
                staticCFunction(::subclassProcedure),
                0UL,
                windowRef.toLong().toULong(),
            )
        }

        return window
    }

    private fun updateWindowPosition() {
        memScoped {
            val rect = alloc<RECT> {
                left = x
                top = y
                right = x + width
                bottom = y + height
            }

            AdjustWindowRect(rect.ptr, windowStyle.toUInt(), 0)
            SetWindowPos(
                windowHandle,
                null,
                rect.left,
                rect.top,
                rect.right - rect.left,
                rect.bottom - rect.top,
                0u,
            ).let { result ->
                if (result == 0) {
                    throw Win32Exception()
                }
            }
        }
    }

    companion object {
        fun runMessageLoop() {
            memScoped {
                val msg = alloc<MSG>()
                while (GetMessageW(msg.ptr, null, 0u, 0u) > 0) {
                    TranslateMessage(msg.ptr)
                    DispatchMessageW(msg.ptr)
                }
            }
        }

        private val instanceHandle = GetModuleHandleW(null)

        private fun isClassRegistered(className: String): Boolean {
            memScoped {
                return GetClassInfoW(instanceHandle, className, alloc<WNDCLASSW>().ptr) != 0
            }
        }

        private fun registerClass(className: String) {
            @OptIn(ExperimentalForeignApi::class)
            fun procedure(windowHandle: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
                if (msg == WM_CREATE.toUInt()) {
                    val createStruct = lParam.toCPointer<CREATESTRUCTW>()!!
                    val createParams = createStruct.pointed.lpCreateParams!!
                    SetWindowLongPtrW(windowHandle, GWLP_USERDATA, createParams.toLong())
                    return DefWindowProcW(windowHandle, msg, wParam, lParam)
                }

                val windowRef = windowRefOf(windowHandle) ?: return DefWindowProcW(windowHandle, msg, wParam, lParam)
                return windowRef.get().handleMessage(msg, wParam, lParam)
            }

            memScoped {
                val windowClass = cValue<WNDCLASSEX> {
                    cbSize = sizeOf<WNDCLASSEX>().toUInt()
                    style = 0u
                    lpfnWndProc = staticCFunction(::procedure)
                    cbClsExtra = 0
                    cbWndExtra = 0
                    hInstance = instanceHandle
                    hIcon = null
                    hCursor = null
                    lpszMenuName = null
                    lpszClassName = className.wcstr.ptr
                    hbrBackground = GetStockObject(GRAY_BRUSH)?.reinterpret()
                    hIconSm = null
                }

                RegisterClassExW(windowClass.ptr).let { result ->
                    if (result == 0.toUShort()) {
                        throw Win32Exception()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun windowRefOf(windowHandle: HWND?): StableRef<Window>? {
    return GetWindowLongPtrW(windowHandle, GWLP_USERDATA).toCPointer<CPointed>()?.asStableRef()
}

@OptIn(ExperimentalForeignApi::class)
private fun windowRefOf(pointer: Long): StableRef<Window>? {
    return pointer.toCPointer<CPointed>()?.asStableRef()
}