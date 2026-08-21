/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use uniffi_kmm_fixture_ext_types_error_lib::ExternalError;

#[uniffi::export]
fn throw_external_error() -> Result<(), ExternalError> {
    Err(ExternalError::Failed {
        reason: "oops".to_string(),
    })
}

uniffi::setup_scaffolding!("ext_errors");
