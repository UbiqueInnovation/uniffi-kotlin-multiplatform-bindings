/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::env;
use std::path::Path;

pub fn generate_scaffolding_from_current_dir() {
    let test_name = get_test_name_from_path(env::current_dir().unwrap());
    uniffi::generate_scaffolding(format!("src/{test_name}.udl")).unwrap();
}

fn get_test_name_from_path(path: impl AsRef<Path>) -> String {
    let path = path.as_ref();
    let directory_name = path.file_name().unwrap();
    let directory_name = directory_name.to_str().unwrap();
    if directory_name != "uniffi" {
        return directory_name.to_owned();
    }
    get_test_name_from_path(path.parent().unwrap())
}
