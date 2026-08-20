/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

//! `mutable_records`: the per-record escape hatch from `generate_immutable_records`.
//!
//! Records default to `val` fields in this backend. A record named in the
//! `mutable_records` list in `uniffi.toml` keeps `var` fields instead. The list is keyed
//! by the record's name, so the UDL and proc-macro paths are covered here side by side.

pub struct UdlFrozen {
    pub counter: i64,
    pub label: String,
}

pub struct UdlThawed {
    pub counter: i64,
    pub label: String,
}

pub fn echo_udl_thawed(value: UdlThawed) -> UdlThawed {
    value
}

pub fn echo_udl_frozen(value: UdlFrozen) -> UdlFrozen {
    value
}

/// Not named in `mutable_records`, so its fields are `val`.
#[derive(uniffi::Record)]
pub struct ProcFrozen {
    pub counter: i64,
    pub label: String,
}

/// Named in `mutable_records`, so its fields stay `var`.
#[derive(uniffi::Record)]
pub struct ProcThawed {
    pub counter: i64,
    /// A field with a default, to check that a `var` still carries one.
    #[uniffi(default = "unset")]
    pub label: String,
}

#[uniffi::export]
pub fn echo_proc_thawed(value: ProcThawed) -> ProcThawed {
    value
}

#[uniffi::export]
pub fn echo_proc_frozen(value: ProcFrozen) -> ProcFrozen {
    value
}

uniffi::include_scaffolding!("mutable-records");
