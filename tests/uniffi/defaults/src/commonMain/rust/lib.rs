/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#[derive(uniffi::Enum)]
pub enum MyEnum {
    MyVariant {
        #[uniffi(default)]
        d: u8,
        #[uniffi(default = 1)]
        e: u8,
    },
}

#[derive(uniffi::Record)]
pub struct MyStruct {
    #[uniffi(default)]
    pub a: u8,
    #[uniffi(default = 1)]
    pub b: u8,
}

#[uniffi::export(default(sep = ",", max_splits))]
pub fn split(
    text: String,
    sep: String,             // Will have a default of `","`
    max_splits: Option<u32>, // Will have a default of `None`
) -> Vec<String> {
    let max_splits = max_splits.unwrap_or(0);
    text.splitn((max_splits + 1) as usize, &sep)
        .map(|s| s.to_string())
        .collect()
}

#[derive(uniffi::Object)]
pub struct TextSplitter {
    sep: String,
}

#[uniffi::export]
impl TextSplitter {
    #[uniffi::constructor(default(sep = ","))]
    fn new(sep: String) -> Self {
        Self { sep }
    }

    #[uniffi::method(default(max_splits))]
    fn split(&self, text: String, max_splits: Option<u32>) -> Vec<String> {
        let max_splits = max_splits.unwrap_or(0);
        text.splitn((max_splits + 1) as usize, &self.sep)
            .map(|s| s.to_string())
            .collect()
    }
}

pub struct CustomType(pub i64);

uniffi::custom_type!(CustomType, String, {
    lower: |obj| obj.0.to_string(),
    try_lift: |val| Ok(CustomType(val.parse()?)),
});

#[uniffi::export(default(v = "42"))]
pub fn do_something_with_custom_type(v: CustomType) -> CustomType {
    CustomType(v.0 + 1)
}

#[uniffi::export(default(v = None))]
pub fn is_optional_custom_type_none_default_none(v: Option<CustomType>) -> bool {
    v.is_none()
}

#[uniffi::export(default(v = Some("42")))]
pub fn is_optional_custom_type_none_default_some(v: Option<CustomType>) -> bool {
    v.is_none()
}

uniffi::setup_scaffolding!("defaults");
