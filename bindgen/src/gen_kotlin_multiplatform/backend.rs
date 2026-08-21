/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

//! Backend-agnostic askama filters.

use askama::Result;
use uniffi_bindgen::interface::{
    AsType, CallbackInterface, ComponentInterface, FfiType, Object, Record,
};
use uniffi_bindgen::to_askama_error;

macro_rules! lookup_error {
    ($($args:tt)*) => {
        to_askama_error(&format!($($args)*))
    }
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
