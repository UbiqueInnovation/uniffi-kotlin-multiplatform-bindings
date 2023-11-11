use std::collections::HashMap;
use std::fs;
use std::fs::File;
use std::io::Write;

use anyhow::Result;
use camino::{Utf8Path, Utf8PathBuf};
use include_dir::{include_dir, Dir};
use serde::{Deserialize, Serialize};
use uniffi_bindgen::backend::TemplateExpression;
use uniffi_bindgen::{BindingGenerator, BindingsConfig, ComponentInterface};

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

    fn update_from_dependency_configs(&mut self, config_map: HashMap<&str, &Self>) {
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
    common: HashMap<String, String>,
    jvm: HashMap<String, String>,
    native: HashMap<String, String>,
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

        create_target(config, include_dir!("kotlin-uniffi-base/src/commonMain/kotlin"), out_dir, "commonMain", bindings.common);
        create_target(config, include_dir!("kotlin-uniffi-base/src/jvmMain/kotlin"), out_dir, "jvmMain", bindings.jvm);
        create_target(config, include_dir!("kotlin-uniffi-base/src/nativeMain/kotlin"), out_dir, "nativeMain", bindings.native);

        create_cinterop(ci, out_dir, bindings.header);

        Ok(())
    }

    fn check_library_path(&self, _library_path: &Utf8Path, _cdylib_name: Option<&str>) -> Result<()> {
        // FIXME should we do something meaningful here?
        // TODO debug when this method is called and what arguments are passed here
        Ok(())
    }
}

fn create_target(config: &Config, base_dir: Dir, out_dir: &Utf8Path, name: &str, files: HashMap<String, String>) {
    let mut all_files: HashMap<String, String> = HashMap::new();
    for base_file in base_dir.files() {
        let file_name = base_file.path().file_name().unwrap().to_str().unwrap().to_string();
        let file_content = base_file.contents_utf8().unwrap().to_string();
        all_files.insert(file_name, file_content);
    }
    all_files.extend(files);

    let package_path: Utf8PathBuf = config.package_name().split(".").collect();
    let dst_dir = Utf8PathBuf::from(out_dir).join(&name).join("kotlin").join(package_path);
    fs::create_dir_all(&dst_dir).unwrap();
    for (file_name, file_content) in all_files {
        let file_path = Utf8PathBuf::from(&dst_dir).join(file_name);
        let mut f = File::create(&file_path).unwrap();
        writeln!(f, "package {}", config.package_name()).unwrap();
        writeln!(f, "").unwrap();
        write!(f, "{}", file_content).unwrap();
    }
}

fn create_cinterop(ci: &ComponentInterface, out_dir: &Utf8Path, content: String) {
    let dst_dir = Utf8PathBuf::from(out_dir).join("nativeInterop").join("cinterop").join("headers").join(ci.namespace());
    fs::create_dir_all(&dst_dir).unwrap();
    let file_path = Utf8PathBuf::from(dst_dir).join(format!("{}.h", ci.namespace()));
    let mut f = File::create(&file_path).unwrap();
    write!(f, "{}", content).unwrap();
}
