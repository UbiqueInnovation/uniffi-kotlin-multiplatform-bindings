/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import io.gitlab.trixnity.uniffi.examples.todolist.TodoList
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.zeroValue
import platform.AppKit.*
import platform.Foundation.NSMakePoint
import platform.Foundation.NSMakeSize
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val app = NSApplication.sharedApplication.apply {
        setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        delegate = object : NSObject(), NSApplicationDelegateProtocol {
            override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication): Boolean = true
        }
    }

    val windowStyle = NSWindowStyleMaskTitled or
            NSWindowStyleMaskMiniaturizable or
            NSWindowStyleMaskClosable or
            NSWindowStyleMaskResizable

    val window = object : NSWindow(
        contentRect = zeroValue(),
        styleMask = windowStyle,
        backing = NSBackingStoreBuffered,
        defer = false,
    ) {
        override fun canBecomeKeyWindow(): Boolean = true
        override fun canBecomeMainWindow(): Boolean = true
    }

    window.apply {
        setContentViewController(ContentViewController(TodoList()))
        setContentSize(NSMakeSize(300.0, 200.0))
        cascadeTopLeftFromPoint(NSMakePoint(20.0, 20.0))
        makeKeyAndOrderFront(this)
    }

    app.run()
}