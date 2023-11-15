/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use paste::paste;
use uniffi_bindgen::backend::{CodeType, Literal};
use uniffi_bindgen::interface::{Radix, Type};

fn render_literal(literal: &Literal) -> String {
    fn typed_number(type_: &Type, num_str: String) -> String {
        match type_ {
            // Bytes, Shorts and Ints can all be inferred from the type.
            Type::Int8 | Type::Int16 | Type::Int32 => num_str,
            Type::Int64 => format!("{num_str}L"),

            Type::UInt8 | Type::UInt16 | Type::UInt32 => format!("{num_str}u"),
            Type::UInt64 => format!("{num_str}uL"),

            Type::Float32 => format!("{num_str}f"),
            Type::Float64 => num_str,
            _ => panic!("Unexpected literal: {num_str} is not a number"),
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
    ($T:ty, $class_name:literal, $canonical_name:literal) => {
        paste! {
            #[derive(Debug)]
            pub struct $T;

            impl CodeType for $T  {
                fn type_label(&self) -> String {
                    $class_name.into()
                }

                fn canonical_name(&self) -> String {
                    $canonical_name.into()
                }

                fn literal(&self, literal: &Literal) -> String {
                    render_literal(&literal)
                }
            }
        }
    };
}

impl_code_type_for_primitive!(BooleanCodeType, "Boolean", "Boolean");
impl_code_type_for_primitive!(StringCodeType, "String", "String");
impl_code_type_for_primitive!(Int8CodeType, "Byte", "Byte");
impl_code_type_for_primitive!(Int16CodeType, "Short", "Short");
impl_code_type_for_primitive!(Int32CodeType, "Int", "Int");
impl_code_type_for_primitive!(Int64CodeType, "Long", "Long");
impl_code_type_for_primitive!(UInt8CodeType, "UByte", "UByte");
impl_code_type_for_primitive!(UInt16CodeType, "UShort", "UShort");
impl_code_type_for_primitive!(UInt32CodeType, "UInt", "UInt");
impl_code_type_for_primitive!(UInt64CodeType, "ULong", "ULong");
impl_code_type_for_primitive!(Float32CodeType, "Float", "Float");
impl_code_type_for_primitive!(Float64CodeType, "Double", "Double");
impl_code_type_for_primitive!(BytesCodeType, "ByteArray", "ByteArray");
