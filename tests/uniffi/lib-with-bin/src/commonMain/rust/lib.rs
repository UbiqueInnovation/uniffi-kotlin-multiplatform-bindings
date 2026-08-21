/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

#[uniffi::export]
fn ping() -> String {
    "pong".to_string()
}

uniffi::setup_scaffolding!("lib_with_bin");
