/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use anyhow::{bail, Result};
use uniffi_bindgen::interface::{DefaultValue, Literal};
use uniffi_bindgen::ComponentInterface;

use super::CodeType;

#[derive(Debug)]
pub struct EnumCodeType {
    id: String,
}

impl EnumCodeType {
    pub fn new(id: String) -> Self {
        Self { id }
    }
}

impl CodeType for EnumCodeType {
    fn type_label(&self, ci: &ComponentInterface) -> String {
        super::KotlinCodeOracle.class_name(ci, &self.id)
    }

    fn canonical_name(&self) -> String {
        format!("Type{}", self.id)
    }

    // A bare `#[uniffi(default)]` has no meaning for an enum - there is no variant to
    // pick - so only the explicit `Literal::Enum` form is accepted. The base `{}()`
    // rendering would emit a constructor call for a type that has no constructor.
    fn default(
        &self,
        default: &DefaultValue,
        ci: &ComponentInterface,
        _config: &super::Config,
    ) -> Result<String> {
        if let DefaultValue::Literal(Literal::Enum(v, _)) = default {
            Ok(format!(
                "{}.{}",
                self.type_label(ci),
                super::KotlinCodeOracle.enum_variant_name(v)
            ))
        } else {
            bail!("Invalid default for enum type: {default:?}")
        }
    }
}
