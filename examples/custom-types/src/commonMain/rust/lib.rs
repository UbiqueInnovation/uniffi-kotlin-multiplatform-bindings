/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use url::Url;

// A custom guid defined via a proc-macro (ie, not referenced in the UDL)
// By far the easiest way to define custom types.
pub struct ExampleCustomType(String);
uniffi::custom_newtype!(ExampleCustomType, String);

// Custom Handle type which trivially wraps an i64.
pub struct Handle(pub i64);

// Custom TimeIntervalMs type which trivially wraps an i64.
pub struct TimeIntervalMs(pub i64);

// Custom TimeIntervalSecDbl type which trivially wraps an f64.
pub struct TimeIntervalSecDbl(pub f64);

// Custom TimeIntervalSecFlt type which trivially wraps an f32.
pub struct TimeIntervalSecFlt(pub f32);

// Each custom type needs a `custom_type!` (or `custom_newtype!`) invocation on the
// scaffolding side, telling uniffi how to convert to and from the builtin type.
uniffi::custom_type!(Handle, i64, {
    // Convert our custom type to the builtin used across the FFI
    lower: |obj| obj.0,
    // Convert the builtin back to our custom type
    try_lift: |val| Ok(Handle(val)),
});

// `Url` gets converted to a `String` to pass across the FFI.
uniffi::custom_type!(Url, String, {
    // `remote` is required since `Url` is from a different crate
    remote,
    lower: |obj| obj.into(),
    try_lift: |val| Ok(Url::parse(&val)?),
});

uniffi::custom_type!(TimeIntervalMs, i64, {
    lower: |obj| obj.0,
    try_lift: |val| Ok(TimeIntervalMs(val)),
});

uniffi::custom_type!(TimeIntervalSecDbl, f64, {
    lower: |obj| obj.0,
    try_lift: |val| Ok(TimeIntervalSecDbl(val)),
});

// For a trivial newtype wrapper, `custom_newtype!` does the same thing in one line.
uniffi::custom_newtype!(TimeIntervalSecFlt, f32);

// And a little struct and function that ties them together.
pub struct CustomTypesDemo {
    url: Url,
    handle: Handle,
    time_interval_ms: TimeIntervalMs,
    time_interval_sec_dbl: TimeIntervalSecDbl,
    time_interval_sec_flt: TimeIntervalSecFlt,
}

pub fn get_custom_types_demo(v: Option<CustomTypesDemo>) -> CustomTypesDemo {
    v.unwrap_or_else(|| CustomTypesDemo {
        url: Url::parse("http://example.com/").unwrap(),
        handle: Handle(123),
        time_interval_ms: TimeIntervalMs(456000),
        time_interval_sec_dbl: TimeIntervalSecDbl(456.0),
        time_interval_sec_flt: TimeIntervalSecFlt(777.0),
    })
}

#[uniffi::export]
pub fn get_example_custom_type() -> ExampleCustomType {
    ExampleCustomType("abadidea".to_string())
}

uniffi::include_scaffolding!("custom-types");