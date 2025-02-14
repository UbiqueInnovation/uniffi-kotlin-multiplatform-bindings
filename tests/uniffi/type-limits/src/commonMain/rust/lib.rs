/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::str;

fn take_i8(v: i8) -> i8 {
    v
}
fn take_i16(v: i16) -> i16 {
    v
}
fn take_i32(v: i32) -> i32 {
    v
}
fn take_i64(v: i64) -> i64 {
    v
}

fn take_u8(v: u8) -> u8 {
    v
}
fn take_u16(v: u16) -> u16 {
    v
}
fn take_u32(v: u32) -> u32 {
    v
}
fn take_u64(v: u64) -> u64 {
    v
}

fn take_f32(v: f32) -> f32 {
    v
}
fn take_f64(v: f64) -> f64 {
    v
}

fn take_string(v: String) -> String {
    assert!(str::from_utf8(v.as_bytes()).is_ok());
    v
}
fn take_bytes(v: Vec<u8>) -> Vec<u8> {
    v
}

uniffi::include_scaffolding!("type-limits");