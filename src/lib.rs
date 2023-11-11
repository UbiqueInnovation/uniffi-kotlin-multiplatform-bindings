use std::collections::HashMap;
use std::fs;
use std::fs::File;
use std::io::Write;

use anyhow::Result;
use camino::{Utf8Path, Utf8PathBuf};
use serde::{Deserialize, Serialize};
use uniffi_bindgen::{BindingGenerator, BindingsConfig, ComponentInterface};
use uniffi_bindgen::backend::TemplateExpression;

pub use gen_kotlin_multiplatform::generate_bindings;

pub mod gen_kotlin_multiplatform;

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct Config {
    package_name: Option<String>,
    cdylib_name: Option<String>,
    #[serde(default)]
    custom_types: HashMap<String, CustomTypeConfig>,
    #[serde(default)]
    external_packages: HashMap<String, String>,
}

impl Config {
    pub fn package_name(&self) -> String {
        if let Some(package_name) = &self.package_name {
            package_name.clone()
        } else {
            "uniffi".into()
        }
    }

    pub fn cdylib_name(&self) -> String {
        if let Some(cdylib_name) = &self.cdylib_name {
            cdylib_name.clone()
        } else {
            "uniffi".into()
        }
    }
}

impl BindingsConfig for Config {
    fn update_from_ci(&mut self, ci: &ComponentInterface) {
        self.package_name.get_or_insert_with(|| ci.namespace().into());
        self.cdylib_name.get_or_insert_with(|| format!("{}", ci.namespace()));
    }

    fn update_from_cdylib_name(&mut self, cdylib_name: &str) {
        self.cdylib_name.get_or_insert_with(|| cdylib_name.to_string());
    }

    fn update_from_dependency_configs(&mut self, _config_map: HashMap<&str, &Self>) {
        // unused
    }
}

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct CustomTypeConfig {
    imports: Option<Vec<String>>,
    type_name: Option<String>,
    into_custom: TemplateExpression,
    from_custom: TemplateExpression,
}

pub struct KotlinMultiplatformBindings {
    common: String,
    jvm: String,
    native: String,
    header: String,
}

pub struct KotlinBindingGenerator {}

impl BindingGenerator for KotlinBindingGenerator {
    type Config = Config;

    fn write_bindings(
        &self,
        ci: &ComponentInterface,
        config: &Self::Config,
        out_dir: &Utf8Path,
    ) -> Result<()> {
        let bindings = generate_bindings(config, ci)?;

        create_target(ci, config, out_dir, "common", bindings.common);
        create_target(ci, config, out_dir, "jvm", bindings.jvm);
        create_target(ci, config, out_dir, "native", bindings.native);

        create_cinterop(ci, out_dir, bindings.header);

        Ok(())
    }

    fn check_library_path(&self, _library_path: &Utf8Path, _cdylib_name: Option<&str>) -> Result<()> {
        // FIXME should we do something meaningful here?
        // TODO debug when this method is called and what arguments are passed here
        Ok(())
    }
}

fn create_target(ci: &ComponentInterface, config: &Config, out_dir: &Utf8Path, name: &str, content: String) {
    let source_set_name = format!("{}Main", name);
    let package_path: Utf8PathBuf = config.package_name().split(".").collect();
    let file_name = format!("{}.{}.kt", ci.namespace(), name);

    let dst_dir = Utf8PathBuf::from(out_dir)
        .join(&source_set_name).join("kotlin").join(package_path);
    let file_path = Utf8PathBuf::from(&dst_dir).join(file_name);

    fs::create_dir_all(&dst_dir).unwrap();
    let mut f = File::create(&file_path).unwrap();
    write!(f, "{}", content).unwrap();
}

fn create_cinterop(ci: &ComponentInterface, out_dir: &Utf8Path, content: String) {
    let dst_dir = Utf8PathBuf::from(out_dir)
        .join("nativeInterop").join("cinterop").join("headers").join(ci.namespace());
    fs::create_dir_all(&dst_dir).unwrap();
    let file_path = Utf8PathBuf::from(dst_dir).join(format!("{}.h", ci.namespace()));
    let mut f = File::create(&file_path).unwrap();
    write!(f, "{}", content).unwrap();
}
