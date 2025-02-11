package com.example.modb

import module_b.hello
import rust_common.TestStruct

public fun testModB() = hello()

fun createTestStruct(s: String)
    = TestStruct(s)