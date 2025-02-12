/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
use anyhow::Context;
use camino::Utf8PathBuf;
use clap::Parser;
use uniffi_bindgen_kotlin_multiplatform_bootstrap::KotlinBindingGenerator;

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

    #[clap(long = "format", default_value_t = false)]
    try_format_code: bool,

    /// Path to the UDL file, or cdylib if `library-mode` is specified.
    source: Utf8PathBuf,

    /// Whether we should exclude dependencies when running "cargo metadata".
    /// This will mean external types may not be resolved if they are implemented in crates
    /// outside of this workspace.
    /// This can be used in environments when all types are in the namespace and fetching
    /// all sub-dependencies causes obscure platform specific problems.
    #[clap(long)]
    metadata_no_deps: bool,
}

fn main() -> anyhow::Result<()> {
    let Cli {
        out_dir,
        config,
        lib_file,
        library_mode,
        crate_name,
        source,
        try_format_code,
        metadata_no_deps,
    } = Cli::parse();

    let binding_generator = KotlinBindingGenerator;

    if library_mode {
        if lib_file.is_some() {
            panic!("--lib-file is not compatible with --library.")
        }
        let out_dir = out_dir.expect("--out-dir is required when using --library");

        let config_supplier = {
            use uniffi_bindgen::cargo_metadata::CrateConfigSupplier;
            let mut cmd = cargo_metadata::MetadataCommand::new();
            if metadata_no_deps {
                cmd.no_deps();
            }
            let metadata = cmd.exec().context("error running cargo metadata")?;
            CrateConfigSupplier::from(metadata)
        };

        uniffi_bindgen::library_mode::generate_bindings(
            &source,
            crate_name,
            &binding_generator,
            &config_supplier,
            config.as_deref(),
            &out_dir,
            try_format_code,
        )?;
    } else {
        uniffi_bindgen::generate_external_bindings(
            &binding_generator,
            source,
            config,
            out_dir,
            lib_file,
            crate_name.as_deref(),
            try_format_code,
        )?;
    }

    Ok(())
}
