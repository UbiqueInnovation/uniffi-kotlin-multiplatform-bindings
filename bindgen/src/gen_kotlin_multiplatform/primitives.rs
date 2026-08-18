/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use anyhow::Result;
use paste::paste;
use uniffi_bindgen::interface::Literal;
use uniffi_bindgen::interface::{ComponentInterface, DefaultValue, Radix, Type};

use super::CodeType;

fn render_literal(literal: &Literal, _ci: &ComponentInterface) -> String {
    fn typed_number(type_: &Type, num_str: String) -> String {
        let unwrapped_type = match type_ {
            Type::Optional { inner_type } => inner_type,
            t => t,
        };
        match unwrapped_type {
            // Bytes, Shorts and Ints can all be inferred from the type.
            Type::Int8 | Type::Int16 | Type::Int32 => num_str,
            Type::Int64 => format!("{num_str}L"),

            Type::UInt8 | Type::UInt16 | Type::UInt32 => format!("{num_str}u"),
            Type::UInt64 => format!("{num_str}uL"),

            Type::Float32 => format!("{num_str}f"),
            Type::Float64 => num_str,
            _ => panic!("Unexpected literal: {num_str} for type: {type_:?}"),
        }
    }

    match literal {
        Literal::Boolean(v) => format!("{v}"),
        Literal::String(s) => format!("\"{s}\""),
        Literal::Int(i, radix, type_) => typed_number(
            type_,
            match radix {
                Radix::Octal => format!("{i:#x}"),
                Radix::Decimal => format!("{i}"),
                Radix::Hexadecimal => format!("{i:#x}"),
            },
        ),
        Literal::UInt(i, radix, type_) => typed_number(
            type_,
            match radix {
                Radix::Octal => format!("{i:#x}"),
                Radix::Decimal => format!("{i}"),
                Radix::Hexadecimal => format!("{i:#x}"),
            },
        ),
        Literal::Float(string, type_) => typed_number(type_, string.clone()),

        _ => unreachable!("Literal"),
    }
}

macro_rules! impl_code_type_for_primitive {
    ($T:ty, $class_name:literal, $default:literal) => {
        paste! {
            #[derive(Debug)]
            pub struct $T;

            impl CodeType for $T  {
                fn type_label(&self, _ci: &ComponentInterface) -> String {
                    format!("kotlin.{}", $class_name)
                }

                fn canonical_name(&self) -> String {
                    $class_name.into()
                }

                // `#[uniffi(default)]` without a literal means "this type's own default",
                // which the base `{}()` rendering cannot express for a primitive - there
                // is no `kotlin.Int()`.
                fn default(&self, default: &DefaultValue, ci: &ComponentInterface) -> Result<String> {
                    match default {
                        DefaultValue::Default => Ok($default.into()),
                        DefaultValue::Literal(literal) => Ok(render_literal(literal, ci)),
                    }
                }
            }
        }
    };
}

impl_code_type_for_primitive!(BooleanCodeType, "Boolean", "false");
impl_code_type_for_primitive!(StringCodeType, "String", "\"\"");
impl_code_type_for_primitive!(BytesCodeType, "ByteArray", "byteArrayOf()");
impl_code_type_for_primitive!(Int8CodeType, "Byte", "0.toByte()");
impl_code_type_for_primitive!(Int16CodeType, "Short", "0");
impl_code_type_for_primitive!(Int32CodeType, "Int", "0");
impl_code_type_for_primitive!(Int64CodeType, "Long", "0L");
impl_code_type_for_primitive!(UInt8CodeType, "UByte", "0U");
impl_code_type_for_primitive!(UInt16CodeType, "UShort", "0U");
impl_code_type_for_primitive!(UInt32CodeType, "UInt", "0U");
impl_code_type_for_primitive!(UInt64CodeType, "ULong", "0UL");
impl_code_type_for_primitive!(Float32CodeType, "Float", "0.0f");
impl_code_type_for_primitive!(Float64CodeType, "Double", "0.0");
