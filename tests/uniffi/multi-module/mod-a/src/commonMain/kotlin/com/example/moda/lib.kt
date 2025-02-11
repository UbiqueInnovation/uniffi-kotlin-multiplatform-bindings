package com.example.moda

import module_a.hello
import rust_common.TestStruct

public fun testModA() = hello()

fun useTestStruct(s: TestStruct)
    = s.get()