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

    fn default(
        &self,
        default: &DefaultValue,
        ci: &ComponentInterface,
        config: &super::Config,
    ) -> Result<String> {
        let default = self.builtin.default(default, ci, config)?;
        match config.custom_types.get(&self.name) {
            // If a value is defined in the config, it means that the custom type is aliased on the
            // kotlin side, and we need to lift the default value.
            Some(custom) => Ok(custom.lift(&default)),
            None => Ok(default),
        }
    }
}
