/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// This example uses `uniffi::` macros to describe the interface.

use uniffi::Enum;

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ArithmeticError {
    #[error("Integer overflow on an operation with {a} and {b}")]
    IntegerOverflow { a: u64, b: u64 },
}

#[derive(uniffi::Record)]
pub struct Operation {
    ty:OperationType,
    a: u64,
    b: u64,
}

#[derive(Enum)]
pub enum OperationType {
    Add,
    Sub,
    Mul,
    Div
}

#[uniffi::export]
fn apply_operation(op: Operation) -> Result<u64> {
   match op.ty {
        OperationType::Add => add(op.a, op.b),
        OperationType::Sub => sub(op.a, op.b),
        OperationType::Mul => mul(op.a, op.b),
        OperationType::Div => Ok(div(op.a,op.b)),
    }
}
#[uniffi::export]
fn add(a: u64, b: u64) -> Result<u64> {
    a.checked_add(b)
        .ok_or(ArithmeticError::IntegerOverflow { a, b })
}

#[uniffi::export]
fn sub(a: u64, b: u64) -> Result<u64> {
    a.checked_sub(b)
        .ok_or(ArithmeticError::IntegerOverflow { a, b })
}
#[uniffi::export]
fn mul(a: u64, b: u64) -> Result<u64> {
    a.checked_mul(b).ok_or(ArithmeticError::IntegerOverflow { a, b })
}

#[uniffi::export]
fn div(dividend: u64, divisor: u64) -> u64 {
    if divisor == 0 {
        panic!("Can't divide by zero");
    }
    dividend / divisor
}

#[uniffi::export]
fn equal(a: u64, b: u64) -> bool {
    a == b
}

type Result<T, E = ArithmeticError> = std::result::Result<T, E>;

uniffi::setup_scaffolding!();
