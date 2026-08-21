/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::sync::Arc;
use uniffi_kmm_example_custom_types::Handle;
use uniffi_kmm_fixture_ext_types_custom_types::{ANestedGuid, Guid, Ouid};
use uniffi_kmm_fixture_ext_types_external_crate::{
    ExternalCrateDictionary, ExternalCrateInterface, ExternalCrateNonExhaustiveEnum,
};
use uniffi_kmm_fixture_ext_types_sub_lib::SubLibType;
use uniffi_kmm_fixture_ext_types_uniffi_one::{
    UniffiOneEnum, UniffiOneInterface, UniffiOneProcMacroType, UniffiOneTrait, UniffiOneType,
    UniffiOneUDLTrait,
};
use url::Url;

// Remote types require a macro call in the Rust source.
uniffi::use_remote_type!(uniffi_kmm_example_custom_types::Url);

pub struct CombinedType {
    pub uoe: UniffiOneEnum,
    pub uot: UniffiOneType,
    pub uots: Vec<UniffiOneType>,
    pub maybe_uot: Option<UniffiOneType>,

    pub guid: Guid,
    pub guids: Vec<Guid>,
    pub maybe_guid: Option<Guid>,

    pub url: Url,
    pub urls: Vec<Url>,
    pub maybe_url: Option<Url>,

    pub handle: Handle,
    pub handles: Vec<Handle>,
    pub maybe_handle: Option<Handle>,

    pub ecd: ExternalCrateDictionary,
    pub ecnee: ExternalCrateNonExhaustiveEnum,
}

fn get_combined_type(existing: Option<CombinedType>) -> CombinedType {
    existing.unwrap_or_else(|| CombinedType {
        uoe: UniffiOneEnum::One,
        uot: UniffiOneType {
            sval: "hello".to_string(),
        },
        uots: vec![
            UniffiOneType {
                sval: "first of many".to_string(),
            },
            UniffiOneType {
                sval: "second of many".to_string(),
            },
        ],
        maybe_uot: None,

        guid: Guid("a-guid".into()),
        guids: vec![Guid("b-guid".into()), Guid("c-guid".into())],
        maybe_guid: None,

        url: Url::parse("http://example.com/").unwrap(),
        urls: vec![],
        maybe_url: None,

        handle: Handle(123),
        handles: vec![Handle(1), Handle(2), Handle(3)],
        maybe_handle: Some(Handle(4)),

        ecd: ExternalCrateDictionary { sval: "ecd".into() },
        ecnee: ExternalCrateNonExhaustiveEnum::One,
    })
}

// Not part of CombinedType as (a) object refs prevent equality testing and
// (b) it's not currently possible to refer to external traits in UDL.
#[derive(Default, uniffi::Record)]
pub struct ObjectsType {
    pub maybe_trait: Option<Arc<dyn UniffiOneTrait>>,
    // XXX - can't refer to UniffiOneInterface here - #1854
    //pub maybe_interface: Option<Arc<UniffiOneInterface>>,
    // Use this in the meantime so the tests can still refer to it.
    pub maybe_interface: Option<u8>,
    pub sub: SubLibType,
}

#[uniffi::export]
fn get_objects_type(value: Option<ObjectsType>) -> ObjectsType {
    value.unwrap_or_default()
}

// A Custom type
fn get_url(url: Url) -> Url {
    url
}

fn get_urls(urls: Vec<Url>) -> Vec<Url> {
    urls
}

fn get_maybe_url(url: Option<Url>) -> Option<Url> {
    url
}

fn get_maybe_urls(urls: Vec<Option<Url>>) -> Vec<Option<Url>> {
    urls
}

// XXX - #1854
// fn get_imported_guid(guid: Guid) -> Guid {

#[uniffi::export]
fn get_imported_ouid(ouid: Ouid) -> Ouid {
    ouid
}

// external custom types wrapping external custom types.
#[uniffi::export]
fn get_imported_nested_guid(guid: Option<ANestedGuid>) -> ANestedGuid {
    guid.unwrap_or_else(|| ANestedGuid(Guid("nested".to_string())))
}

#[uniffi::export]
fn get_imported_nested_ouid(guid: Option<ANestedGuid>) -> ANestedGuid {
    guid.unwrap_or_else(|| ANestedGuid(Guid("nested".to_string())))
}

// A local custom type wrapping an external imported UDL type
// XXX - #1854
// pub struct NestedExternalGuid(pub Guid);
// ...
// fn get_nested_external_guid(nguid: Option<NestedExternalGuid>) -> NestedExternalGuid {

// A local custom type wrapping an external imported procmacro type
pub struct NestedExternalOuid(pub Ouid);
uniffi::custom_newtype!(NestedExternalOuid, Ouid);

#[uniffi::export]
fn get_nested_external_ouid(ouid: Option<NestedExternalOuid>) -> NestedExternalOuid {
    ouid.unwrap_or_else(|| NestedExternalOuid(Ouid("nested-external-ouid".to_string())))
}

// A struct
fn get_uniffi_one_type(t: UniffiOneType) -> UniffiOneType {
    t
}

fn get_uniffi_one_types(ts: Vec<UniffiOneType>) -> Vec<UniffiOneType> {
    ts
}

fn get_maybe_uniffi_one_type(t: Option<UniffiOneType>) -> Option<UniffiOneType> {
    t
}

fn get_maybe_uniffi_one_types(ts: Vec<Option<UniffiOneType>>) -> Vec<Option<UniffiOneType>> {
    ts
}

// An enum
fn get_uniffi_one_enum(e: UniffiOneEnum) -> UniffiOneEnum {
    e
}

fn get_uniffi_one_enums(es: Vec<UniffiOneEnum>) -> Vec<UniffiOneEnum> {
    es
}

fn get_maybe_uniffi_one_enum(e: Option<UniffiOneEnum>) -> Option<UniffiOneEnum> {
    e
}

fn get_maybe_uniffi_one_enums(es: Vec<Option<UniffiOneEnum>>) -> Vec<Option<UniffiOneEnum>> {
    es
}

fn get_uniffi_one_interface() -> Arc<UniffiOneInterface> {
    Arc::new(UniffiOneInterface::new())
}

#[uniffi::export]
fn get_uniffi_one_trait(t: Option<Arc<dyn UniffiOneTrait>>) -> Option<Arc<dyn UniffiOneTrait>> {
    t
}

fn get_uniffi_one_proc_macro_type(t: UniffiOneProcMacroType) -> UniffiOneProcMacroType {
    t
}

fn get_external_crate_interface(val: String) -> Arc<ExternalCrateInterface> {
    Arc::new(ExternalCrateInterface::new(val))
}

fn get_uniffi_one_udl_trait(
    t: Option<Arc<dyn UniffiOneUDLTrait>>,
) -> Option<Arc<dyn UniffiOneUDLTrait>> {
    t
}

/// Call back into a trait declared by `uniffi_one`, whose Kotlin package is generated by
/// the `sub-lib` Gradle module and therefore bound to *that* module's library. This
/// library statically links its own copy of `uniffi_one`, so it has a vtable cell of its
/// own that only `UniffiVtableRegistry` can fill in.
#[uniffi::export]
fn call_uniffi_one_trait_from_ext_types(t: Arc<dyn UniffiOneTrait>) -> String {
    t.hello()
}

/// As above for the UDL-declared trait. Worth having both: our own view of this one is
/// `typedef trait UniffiOneUDLTrait`, which is `Trait(RustOnly)` here - only `uniffi_one`'s
/// own interface knows it has a foreign vtable at all. That is also why this one is
/// declared in `ext-types.udl` rather than with `#[uniffi::export]`: a proc-macro export
/// would infer `Trait(Both)` and clash with the typedef.
fn call_uniffi_one_udl_trait_from_ext_types(t: Arc<dyn UniffiOneUDLTrait>) -> String {
    t.hello()
}

uniffi::include_scaffolding!("ext-types");
