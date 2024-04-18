/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::env;

fn main() {
    let target = env::var("TARGET").unwrap();

    let mut build = cc::Build::new();
    build.cpp(true).flag_if_supported("-std=c++17");

    if target.contains("android") {
        build.file("src/commonMain/cpp/android.cc");
        build.cpp_link_stdlib("c++_shared");
        println!("cargo:rerun-if-changed=src/commonMain/cpp/android.cc");
        println!("cargo:rustc-link-lib=dylib=aaudio");
    } else if target.contains("apple") {
        build.file("src/commonMain/cpp/apple.mm");
        build.cpp_link_stdlib("c++");
        println!("cargo:rerun-if-changed=src/commonMain/cpp/apple.mm");
        println!("cargo:rustc-link-lib=framework=Foundation");
        println!("cargo:rustc-link-lib=framework=AVFoundation");
    } else {
        build.file("src/commonMain/cpp/other.cc");
        println!("cargo:rerun-if-changed=src/commonMain/cpp/other.cc");
    }

    build.compile("audiocpp");
}
