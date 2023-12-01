/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use camino::Utf8PathBuf;
use clap::Parser;

use uniffi_bindgen_kotlin_multiplatform::KotlinBindingGenerator;

#[derive(Parser)]
#[clap(name = "uniffi-bindgen")]
#[clap(version = clap::crate_version!())]
#[clap(propagate_version = true)]
struct Cli {
    /// Directory in which to write generated files. Default is same folder as .udl file.
    #[clap(long, short)]
    out_dir: Option<Utf8PathBuf>,

    /// Path to the optional uniffi config file.
    /// If not provided, uniffi-bindgen will try to guess it from the UDL's file location.
    #[clap(long, short)]
    config: Option<Utf8PathBuf>,

    /// Extract proc-macro metadata from a native lib (cdylib or staticlib) for this crate.
    #[clap(long, short)]
    lib_file: Option<Utf8PathBuf>,

    /// Pass in a cdylib path rather than a UDL file.
    #[clap(long = "library")]
    library_mode: bool,

    /// When `--library` is passed, only generate bindings for one crate.
    /// When `--library` is not passed, use this as the crate name instead of attempting to
    /// locate and parse Cargo.toml.
    #[clap(long = "crate")]
    crate_name: Option<String>,

    /// Path to the UDL file, or cdylib if `library-mode` is specified.
    source: Utf8PathBuf,
}

pub fn main() {
    generate_bindings().unwrap();
}

fn generate_bindings() -> anyhow::Result<()> {
    let Cli {
        out_dir,
        config,
        lib_file,
        library_mode,
        crate_name,
        source,
    } = Cli::parse();

    let binding_gen = KotlinBindingGenerator {};

    if library_mode {
        if lib_file.is_some() {
            panic!("--lib-file is not compatible with --library.")
        }
        if config.is_some() {
            panic!("--config is not compatible with --library.  The config file(s) will be found automatically.")
        }
        let out_dir = out_dir.expect("--out-dir is required when using --library");

        uniffi_bindgen::library_mode::generate_external_bindings(
            binding_gen,
            &source,
            crate_name,
            &out_dir,
        )?;
    } else {
        uniffi_bindgen::generate_external_bindings(
            binding_gen,
            &source,
            config.as_deref(),
            out_dir.as_deref(),
            lib_file.as_deref(),
            crate_name.as_deref(),
        )?;
    }

    Ok(())
}
