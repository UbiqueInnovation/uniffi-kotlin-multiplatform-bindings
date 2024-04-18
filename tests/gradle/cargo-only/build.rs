/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::env;

use cbindgen::{Builder, Language};

fn main() {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();

    Builder::new()
        .with_crate(crate_dir)
        .with_language(Language::C)
        .with_cpp_compat(false)
        .generate()
        .unwrap()
        .write_to_file("build/generated/bindings.h");

    println!("cargo:rustc-env=OPT_LEVEL={}", env::var("OPT_LEVEL").unwrap());

    let features = env::vars().map(|(k, _)| k).filter_map(|k| {
        k.strip_prefix("CARGO_FEATURE_")
            .map(|f| f.to_lowercase().replace('_', "-"))
    });
    println!("cargo:rustc-env=FEATURES={}", features.collect::<Vec<_>>().join(","));
}
