/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use crate_one::CrateOneType;
use crate_two::CrateTwoType;

pub struct CombinedType {
    pub cot: CrateOneType,
    pub ctt: CrateTwoType,
}

fn get_combined_type(existing: Option<CombinedType>) -> CombinedType {
    existing.unwrap_or_else(|| CombinedType {
        cot: CrateOneType {
            sval: "hello".to_string(),
        },
        ctt: CrateTwoType { ival: 1 },
    })
}

uniffi::include_scaffolding!("external_types");
uniffi_reexport_scaffolding!();
