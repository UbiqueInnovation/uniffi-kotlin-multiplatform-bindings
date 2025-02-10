/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

 use std::env;
 use camino::Utf8Path;
 
 fn main() {
     let path = env::current_dir().unwrap();
     let test_directory = path.parent().unwrap();
     let test_directory_name = test_directory.file_name().unwrap();
     let test = test_directory_name.to_str().unwrap();
 
     let udl_file_path = format!("./src/{}.udl", test);
     let udl_file = Utf8Path::new(&udl_file_path);
     uniffi::generate_scaffolding(udl_file).unwrap();
 }
 