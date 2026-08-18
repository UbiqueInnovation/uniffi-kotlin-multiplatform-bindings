/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

//! Backend-agnostic askama filters.
//!
//! Vendored from `uniffi_bindgen::backend::filters`, which was removed upstream in
//! uniffi 0.30. Upstream now keeps an equivalent set private to each language
//! backend; external generators are expected to carry their own copy.
//!
//! Ported to the askama 0.14+ filter ABI: every filter carries `#[askama::filter_fn]`
//! and takes `&dyn askama::Values` as its second argument.

use askama::Result;
use uniffi_bindgen::interface::{
    AsType, CallbackInterface, ComponentInterface, Enum, FfiType, Function, Object, Record,
};
use uniffi_bindgen::to_askama_error;

macro_rules! lookup_error {
    ($($args:tt)*) => {
        to_askama_error(&format!($($args)*))
    }
}

/// Get an Enum definition by name
#[askama::filter_fn]
pub fn get_enum_definition<'a>(
    ci: &'a ComponentInterface,
    _: &dyn askama::Values,
    name: &str,
) -> Result<&'a Enum> {
    ci.get_enum_definition(name)
        .ok_or_else(|| lookup_error!("enum {name} not found"))
}

/// Get a Record definition by name
#[askama::filter_fn]
pub fn get_record_definition<'a>(
    ci: &'a ComponentInterface,
    _: &dyn askama::Values,
    name: &str,
) -> Result<&'a Record> {
    ci.get_record_definition(name)
        .ok_or_else(|| lookup_error!("record {name} not found"))
}

/// Get a Function definition by name
#[askama::filter_fn]
pub fn get_function_definition<'a>(
    ci: &'a ComponentInterface,
    _: &dyn askama::Values,
    name: &str,
) -> Result<&'a Function> {
    ci.get_function_definition(name)
        .ok_or_else(|| lookup_error!("function {name} not found"))
}

/// Get an Object definition by name
#[askama::filter_fn]
pub fn get_object_definition<'a>(
    ci: &'a ComponentInterface,
    _: &dyn askama::Values,
    name: &str,
) -> Result<&'a Object> {
    ci.get_object_definition(name)
        .ok_or_else(|| lookup_error!("object {name} not found"))
}

/// Get a Callback Interface definition by name
#[askama::filter_fn]
pub fn get_callback_interface_definition<'a>(
    ci: &'a ComponentInterface,
    _: &dyn askama::Values,
    name: &str,
) -> Result<&'a CallbackInterface> {
    ci.get_callback_interface_definition(name)
        .ok_or_else(|| lookup_error!("callback interface {name} not found"))
}

/// Get the FfiType for a Type
#[askama::filter_fn]
pub fn ffi_type(type_: &impl AsType, _: &dyn askama::Values) -> Result<FfiType> {
    Ok(type_.as_type().into())
}
