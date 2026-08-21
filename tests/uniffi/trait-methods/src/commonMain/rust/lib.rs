/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::sync::Arc;

#[derive(Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct TraitMethods {
    val: String,
}

impl TraitMethods {
    fn new(val: String) -> Self {
        Self { val }
    }
}

impl std::fmt::Display for TraitMethods {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "TraitMethods({})", self.val)
    }
}

#[derive(Debug, PartialEq, Eq, PartialOrd, Ord, Hash, uniffi::Object)]
#[uniffi::export(Debug, Display, Eq, Hash, Ord)]
pub struct ProcTraitMethods {
    val: String,
}

#[uniffi::export]
impl ProcTraitMethods {
    #[uniffi::constructor]
    fn new(val: String) -> Arc<Self> {
        Arc::new(Self { val })
    }
}

impl std::fmt::Display for ProcTraitMethods {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "ProcTraitMethods({})", self.val)
    }
}

// Records and enums can carry exported methods and uniffi trait impls exactly like objects can.
// Unlike an object, the receiver travels as a serialized value rather than a handle, so `self` is
// lowered through the type's own FfiConverter.

#[derive(Debug, PartialEq, Eq, PartialOrd, Ord, Hash, uniffi::Record)]
#[uniffi::export(Debug, Display, Eq, Hash, Ord)]
pub struct TraitMethodsRecord {
    pub text: String,
}

#[uniffi::export]
impl TraitMethodsRecord {
    /// A plain method on a record.
    fn shout(&self) -> String {
        self.text.to_uppercase()
    }

    /// ... taking an argument, to check the receiver is not confused with the arg list.
    fn repeated(&self, times: u32) -> String {
        self.text.repeat(times as usize)
    }

    /// ... and an async one, which lowers the receiver on the `uniffiRustCallAsync` path.
    async fn shout_later(&self) -> String {
        self.text.to_uppercase()
    }
}

impl std::fmt::Display for TraitMethodsRecord {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "TraitMethodsRecord({})", self.text)
    }
}

/// An enum with associated data, which becomes a Kotlin `sealed class`.
#[derive(Debug, PartialEq, Eq, PartialOrd, Ord, Hash, uniffi::Enum)]
#[uniffi::export(Debug, Display, Eq, Hash, Ord)]
pub enum TraitMethodsEnum {
    Plain,
    WithData { text: String },
}

#[uniffi::export]
impl TraitMethodsEnum {
    fn describe(&self) -> String {
        match self {
            TraitMethodsEnum::Plain => "plain".to_string(),
            TraitMethodsEnum::WithData { text } => format!("data:{text}"),
        }
    }
}

impl std::fmt::Display for TraitMethodsEnum {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "TraitMethodsEnum({})", self.describe())
    }
}

/// A fieldless enum, which becomes a Kotlin `enum class`. `equals`, `hashCode` and
/// `compareTo` are final on `kotlin.Enum`, so only `Display`/`Debug` can be rendered
/// there - the other three already behave the way Rust's derives would.
#[derive(Debug, PartialEq, Eq, Hash, uniffi::Enum)]
#[uniffi::export(Debug, Display)]
pub enum TraitMethodsFlatEnum {
    One,
    Two,
}

#[uniffi::export]
impl TraitMethodsFlatEnum {
    fn doubled(&self) -> u32 {
        match self {
            TraitMethodsFlatEnum::One => 2,
            TraitMethodsFlatEnum::Two => 4,
        }
    }
}

impl std::fmt::Display for TraitMethodsFlatEnum {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "TraitMethodsFlatEnum({})", self.doubled())
    }
}

uniffi::include_scaffolding!("trait-methods");
