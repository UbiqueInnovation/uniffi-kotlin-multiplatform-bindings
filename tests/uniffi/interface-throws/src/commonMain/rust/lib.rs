/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

//! Fixture for throwing methods that are reachable both through an exported
//! trait (interface) and through the `uniffi::Object` that implements it.
//!
//! The bindings generate an `interface` plus a class for every object, and the
//! `@Throws` annotation has to be present on *both* of them. On the JVM the
//! annotation is what puts the `throws` clause into the class file, and without
//! it Java callers holding a reference of the concrete class type cannot catch
//! the exception at all.

use std::sync::Arc;

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum GreeterError {
    #[error("Greeter is not in the mood: {reason}")]
    NotInTheMood { reason: String },
}

/// A trait exposed as an interface, with a method that can throw.
#[uniffi::export(with_foreign)]
pub trait Greeter: Send + Sync {
    fn greet(&self, name: String) -> Result<String, GreeterError>;
}

/// An object implementing the trait above. Its generated class has to expose
/// the same `@Throws` contract as the interface.
#[derive(uniffi::Object)]
pub struct RustGreeter {
    greeting: String,
}

impl Greeter for RustGreeter {
    fn greet(&self, name: String) -> Result<String, GreeterError> {
        if name.is_empty() {
            return Err(GreeterError::NotInTheMood {
                reason: "nobody to greet".to_string(),
            });
        }
        Ok(format!("{}, {}!", self.greeting, name))
    }
}

#[uniffi::export]
impl RustGreeter {
    #[uniffi::constructor]
    fn new(greeting: String) -> Arc<Self> {
        Arc::new(Self { greeting })
    }

    fn greet(&self, name: String) -> Result<String, GreeterError> {
        Greeter::greet(self, name)
    }
}

/// Hands out the very same object, but typed as the trait.
#[uniffi::export]
fn make_greeter_trait(greeting: String) -> Arc<dyn Greeter> {
    Arc::new(RustGreeter { greeting })
}

uniffi::setup_scaffolding!("interface_throws");
