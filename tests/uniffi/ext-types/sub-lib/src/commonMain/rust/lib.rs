/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::sync::Arc;
use uniffi_kmm_fixture_ext_types_uniffi_one::{
    UniffiOneEnum, UniffiOneInterface, UniffiOneTrait, UniffiOneUDLTrait,
};

#[derive(Default, uniffi::Record)]
pub struct SubLibType {
    pub maybe_enum: Option<UniffiOneEnum>,
    pub maybe_trait: Option<Arc<dyn UniffiOneTrait>>,
    pub maybe_interface: Option<Arc<UniffiOneInterface>>,
}

#[uniffi::export]
fn get_sub_type(existing: Option<SubLibType>) -> SubLibType {
    existing.unwrap_or_default()
}

struct OneImpl;

impl UniffiOneTrait for OneImpl {
    fn hello(&self) -> String {
        "sub-lib trait impl says hello".to_string()
    }
}

#[uniffi::export]
fn get_trait_impl() -> Arc<dyn UniffiOneTrait> {
    Arc::new(OneImpl {})
}

/// Call back into a trait that belongs to *another* crate in this library.
///
/// Regression test for upstream #2343: `uniffi_one`'s vtables live behind its own lazy
/// `UniffiLib`, so unless `sub_lib`'s initialiser chains into it, Rust reaches an
/// unregistered vtable here and the process aborts. Lowering a Kotlin implementation
/// touches only the handle map, so nothing else on this path would have registered it.
#[uniffi::export]
fn call_uniffi_one_trait(t: Arc<dyn UniffiOneTrait>) -> String {
    t.hello()
}

/// As above, for the UDL-declared trait - it has a separate vtable of its own.
#[uniffi::export]
fn call_uniffi_one_udl_trait(t: Arc<dyn UniffiOneUDLTrait>) -> String {
    t.hello()
}

uniffi::setup_scaffolding!("sub_lib");
