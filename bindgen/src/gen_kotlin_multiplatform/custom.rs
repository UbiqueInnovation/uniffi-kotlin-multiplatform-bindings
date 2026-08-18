/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use anyhow::{anyhow, Result};
use uniffi_bindgen::interface::DefaultValue;
use uniffi_bindgen::ComponentInterface;

use super::CodeType;

#[derive(Debug)]
pub struct CustomCodeType {
    name: String,
    builtin: Box<dyn CodeType>,
}

impl CustomCodeType {
    pub fn new(name: String, builtin: Box<dyn CodeType>) -> Self {
        CustomCodeType { name, builtin }
    }
}

impl CodeType for CustomCodeType {
    fn type_label(&self, ci: &ComponentInterface) -> String {
        super::KotlinCodeOracle.class_name(ci, &self.name)
    }

    fn canonical_name(&self) -> String {
        format!("Type{}", self.name)
    }

    // A custom type is its builtin at the Kotlin level, so a default is whatever the
    // builtin renders. Without this the base impl would reach `literal`, which a custom
    // type does not implement.
    fn default(&self, default: &DefaultValue, ci: &ComponentInterface) -> Result<String> {
        self.builtin
            .default(default, ci)
            .map_err(|_e| anyhow!("Unsupported default value for {}", self.type_label(ci)))
    }
}
