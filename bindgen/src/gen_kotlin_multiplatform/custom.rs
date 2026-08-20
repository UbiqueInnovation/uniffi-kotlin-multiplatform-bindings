/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use anyhow::Result;
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

    // Without a config a custom type is a typealias to its builtin, so whatever the
    // builtin renders is already the right Kotlin expression. A config makes it a
    // distinct type instead, and the builtin's rendering has to go through the
    // configured `lift` to be assignable. Either way the override is needed: the base
    // impl renders `{}()`, and a custom type has no such constructor.
    fn default(
        &self,
        default: &DefaultValue,
        ci: &ComponentInterface,
        config: &super::Config,
    ) -> Result<String> {
        let default = self.builtin.default(default, ci, config)?;
        match config.custom_types.get(&self.name) {
            Some(custom) => Ok(custom.lift(&default)),
            None => Ok(default),
        }
    }
}
