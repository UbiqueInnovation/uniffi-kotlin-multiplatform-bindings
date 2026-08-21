/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

//! Borrowed bytes: `&[u8]` in Rust, `[ByRef] bytes` in UDL.
//!
//! Since uniffi 0.32 such an argument crosses the FFI as a `ForeignBytes` - a pointer into
//! the caller's buffer plus a length - rather than being copied into a `RustBuffer`. Rust
//! only reads through it, and only for as long as the call is on the stack.
//!
//! Every function here is deliberately written to touch all of the bytes it is lent, so
//! that a pointer which is stale, unpinned or short by a byte shows up as a wrong answer
//! rather than passing by luck.

use std::sync::{Arc, Mutex};

#[derive(Debug, thiserror::Error)]
pub enum ByteError {
    #[error("no bytes")]
    Empty,
}

pub fn sum_bytes(data: &[u8]) -> u64 {
    data.iter().map(|byte| u64::from(*byte)).sum()
}

pub fn echo_bytes(data: &[u8]) -> Vec<u8> {
    data.to_vec()
}

pub fn concat_bytes(first: &[u8], second: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(first.len() + second.len());
    out.extend_from_slice(first);
    out.extend_from_slice(second);
    out
}

pub fn wrap_bytes(prefix: u8, data: &[u8], suffix: String) -> Vec<u8> {
    let mut out = vec![prefix];
    out.extend_from_slice(data);
    out.extend_from_slice(suffix.as_bytes());
    out
}

pub fn sum_non_empty_bytes(data: &[u8]) -> Result<u64, ByteError> {
    if data.is_empty() {
        return Err(ByteError::Empty);
    }
    Ok(sum_bytes(data))
}

/// Keeps a copy of everything it has been lent, so a test can check that what Rust read
/// during a call still matches what the caller passed after the call returned.
pub struct ByteSink {
    seen: Mutex<Vec<u8>>,
}

impl ByteSink {
    fn new(seed: &[u8]) -> Self {
        Self {
            seen: Mutex::new(seed.to_vec()),
        }
    }

    fn absorb(&self, data: &[u8]) -> u64 {
        let mut seen = self.seen.lock().unwrap();
        seen.extend_from_slice(data);
        seen.len() as u64
    }

    fn contents(&self) -> Vec<u8> {
        self.seen.lock().unwrap().clone()
    }
}

// The same surface again through the proc-macros, which reach `&[u8]` by a different route:
// `NamedArg::new` rewrites the reference to a `ForeignBytes` parameter, where UDL goes
// through the `[ByRef]` attribute.

#[uniffi::export]
pub fn proc_sum_bytes(data: &[u8]) -> u64 {
    sum_bytes(data)
}

#[uniffi::export]
pub fn proc_concat_bytes(first: &[u8], second: &[u8]) -> Vec<u8> {
    concat_bytes(first, second)
}

/// The argument name is a Kotlin hard keyword, so the generated parameter is backticked.
/// The name the borrow is bound to must not be.
#[uniffi::export]
pub fn proc_sum_keyword_named_bytes(object: &[u8]) -> u64 {
    sum_bytes(object)
}

#[derive(uniffi::Object)]
pub struct ProcByteSink {
    seen: Mutex<Vec<u8>>,
}

#[uniffi::export]
impl ProcByteSink {
    #[uniffi::constructor]
    fn new(seed: &[u8]) -> Arc<Self> {
        Arc::new(Self {
            seen: Mutex::new(seed.to_vec()),
        })
    }

    fn absorb(&self, data: &[u8]) -> u64 {
        let mut seen = self.seen.lock().unwrap();
        seen.extend_from_slice(data);
        seen.len() as u64
    }

    fn contents(&self) -> Vec<u8> {
        self.seen.lock().unwrap().clone()
    }
}

uniffi::include_scaffolding!("borrowed-bytes");
