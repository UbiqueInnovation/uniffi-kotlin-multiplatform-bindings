/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::borrow::Borrow;
use std::cell::RefCell;
use std::collections::{BTreeSet, HashMap, HashSet};
use std::fmt::Debug;

use crate::gen_kotlin_multiplatform::filters::header_noescape_name;
use anyhow::{anyhow, Context, Result};
use askama::Template;
use filters::header_escape_name;
use heck::{ToLowerCamelCase, ToShoutySnakeCase, ToUpperCamelCase};
use serde::{Deserialize, Serialize};
use uniffi_bindgen::backend::TemplateExpression;
use uniffi_bindgen::interface::*;

mod callback_interface;
mod compounds;
mod custom;
mod enum_;
mod external;
mod miscellany;
mod object;
mod primitives;
mod record;
mod variant;

#[rustfmt::skip]
const CPP_KEYWORDS: &[&str] = &[
    "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor", "bool",
    "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t", "class",
    "compl", "concept", "const", "const_cast", "consteval", "constexpr", "constinit",
    "continue", "co_await", "co_return", "co_yield", "decltype", "default", "delete",
    "do", "double", "dynamic_cast", "else", "enum", "explicit", "export", "extern",
    "false", "float", "for", "friend", "goto", "if", "inline", "int", "long",
    "mutable", "namespace", "new", "noexcept", "not", "not_eq", "nullptr",
    "operator", "or", "or_eq", "private", "protected", "public", "register",
    "reinterpret_cast", "requires", "return", "short", "signed", "sizeof",
    "static", "static_assert", "static_cast", "struct", "switch", "template",
    "this", "thread_local", "throw", "true", "try", "typedef", "typeid", "typename",
    "union", "unsigned", "using", "virtual", "void", "volatile", "wchar_t", "while",
    "xor", "xor_eq"
];

trait CodeType: Debug {
    /// The language specific label used to reference this type. This will be used in
    /// method signatures and property declarations.
    fn type_label(&self, ci: &ComponentInterface) -> String;

    /// A representation of this type label that can be used as part of another
    /// identifier. e.g. `read_foo()`, or `FooInternals`.
    ///
    /// This is especially useful when creating specialized objects or methods to deal
    /// with this type only.
    fn canonical_name(&self) -> String;

    fn literal(&self, _literal: &Literal, ci: &ComponentInterface) -> String {
        unimplemented!("Unimplemented for {}", self.type_label(ci))
    }

    fn is_optional(&self) -> bool {
        false
    }

    /// Name of the FfiConverter
    ///
    /// This is the object that contains the lower, write, lift, and read methods for this type.
    /// Depending on the binding this will either be a singleton or a class with static methods.
    ///
    /// This is the newer way of handling these methods and replaces the lower, write, lift, and
    /// read CodeType methods.  Currently only used by Kotlin, but the plan is to move other
    /// backends to using this.
    fn ffi_converter_name(&self) -> String {
        format!("FfiConverter{}", self.canonical_name())
    }

    /// A list of imports that are needed if this type is in use.
    /// Classes are imported exactly once.
    #[allow(dead_code)]
    fn imports(&self) -> Option<Vec<String>> {
        None
    }

    /// Function to run at startup
    fn initialization_fn(&self) -> Option<String> {
        None
    }
}

// config options to customize the generated Kotlin.
#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct Config {
    pub(super) package_name: Option<String>,
    pub(super) cdylib_name: Option<String>,
    generate_immutable_records: Option<bool>,
    generate_serializable_records: Option<bool>,
    import_pointer_from: Option<Vec<String>>,
    #[serde(default)]
    custom_types: HashMap<String, CustomTypeConfig>,
    #[serde(default)]
    pub(super) external_packages: HashMap<String, String>,
    #[serde(default)]
    kotlin_target_version: Option<String>,
    #[serde(default)]
    disable_java_cleaner: bool,
}

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct CustomTypeConfig {
    imports: Option<Vec<String>>,
    type_name: Option<String>,
    into_custom: TemplateExpression,
    from_custom: TemplateExpression,
}

impl Config {
    // We insist someone has already configured us - any defaults we supply would be wrong.
    pub fn package_name(&self) -> String {
        self.package_name
            .as_ref()
            .expect("package name should have been set in update_component_configs")
            .clone()
    }

    pub fn cdylib_name(&self) -> String {
        self.cdylib_name
            .as_ref()
            .expect("cdylib name should have been set in update_component_configs")
            .clone()
    }

    /// Whether to generate immutable records (`val` instead of `var`)
    pub fn generate_immutable_records(&self) -> bool {
        self.generate_immutable_records.unwrap_or(false)
    }
    /// Whether to use kotlinx Serializable annotation on the data class
    pub fn generate_serializable_records(&self) -> bool {
        self.generate_serializable_records.unwrap_or(false)
    }
    /// Whether to use kotlinx Serializable annotation on the data class
    pub fn has_import_helpers(&self) -> bool {
        self.import_pointer_from.is_some()
    }
    pub fn import_helper_namespace(&self) -> Vec<String> {
        self.import_pointer_from
            .as_ref()
            .cloned().unwrap_or_default()
    }

    pub(crate) fn use_enum_entries(&self) -> bool {
        self.get_kotlin_version() >= KotlinVersion::new(1, 9, 0)
    }

    /// Returns a `Version` with the contents of `kotlin_target_version`.
    /// If `kotlin_target_version` is not defined, version `0.0.0` will be used as a fallback.
    /// If it's not valid, this function will panic.
    fn get_kotlin_version(&self) -> KotlinVersion {
        self.kotlin_target_version
            .clone()
            .map(|v| {
                KotlinVersion::parse(&v).unwrap_or_else(|_| {
                    panic!("Provided Kotlin target version is not valid: {}", v)
                })
            })
            .unwrap_or(KotlinVersion::new(0, 0, 0))
    }
}

#[derive(Debug, PartialEq, Eq, PartialOrd, Ord)]
struct KotlinVersion((u16, u16, u16));

impl KotlinVersion {
    fn new(major: u16, minor: u16, patch: u16) -> Self {
        Self((major, minor, patch))
    }

    fn parse(version: &str) -> Result<Self> {
        let components = version
            .split('.')
            .map(|n| {
                n.parse::<u16>()
                    .map_err(|_| anyhow!("Invalid version string ({n} is not an integer)"))
            })
            .collect::<Result<Vec<u16>>>()?;

        match components.as_slice() {
            [major, minor, patch] => Ok(Self((*major, *minor, *patch))),
            [major, minor] => Ok(Self((*major, *minor, 0))),
            [major] => Ok(Self((*major, 0, 0))),
            _ => Err(anyhow!(
                "Invalid version string (expected 1-3 components): {version}"
            )),
        }
    }
}

pub struct MultiplatformBindings {
    pub common: String,
    pub jvm: String,
    pub android: String,
    pub native: String,
    pub header: String,
}

// Generate kotlin bindings for the given ComponentInterface, as a string.
pub fn generate_bindings(
    config: &Config,
    ci: &ComponentInterface,
) -> Result<MultiplatformBindings> {
    let common = CommonKotlinWrapper::new("common", config.clone(), ci)
        .render()
        .context("failed to render common Kotlin bindings")?;

    let jvm = AndroidJvmKotlinWrapper::new("jvm", config.clone(), ci)
        .render()
        .context("failed to render Kotlin/JVM bindings")?;

    let android = AndroidJvmKotlinWrapper::new("android", config.clone(), ci)
        .render()
        .context("failed to render Android Kotlin/JVM bindings")?;

    let native = NativeKotlinWrapper::new("native", config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Native bindings")?;

    let header = HeaderKotlinWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Native header")?;

    Ok(MultiplatformBindings {
        common,
        jvm,
        android,
        native,
        header,
    })
}

/// A struct to record a Kotlin import statement.
#[derive(Clone, Debug, Eq, Ord, PartialEq, PartialOrd)]
pub enum ImportRequirement {
    /// The name we are importing.
    Import { name: String },
    /// Import the name with the specified local name.
    ImportAs { name: String, as_name: String },
}

impl ImportRequirement {
    /// Render the Kotlin import statement.
    fn render(&self) -> String {
        match &self {
            ImportRequirement::Import { name } => format!("import {name}"),
            ImportRequirement::ImportAs { name, as_name } => {
                format!("import {name} as {as_name}")
            }
        }
    }
}

const FFI_BUILTINS: &'static [&'static str] = &[
    "RustFutureContinuationCallback",
    "ForeignFutureFree",
    "CallbackInterfaceFree",
    "ForeignFuture",
    "ForeignFutureStructU8",
    "ForeignFutureStructI8",
    "ForeignFutureStructU16",
    "ForeignFutureStructI16",
    "ForeignFutureStructU32",
    "ForeignFutureStructI32",
    "ForeignFutureStructU64",
    "ForeignFutureStructI64",
    "ForeignFutureStructF32",
    "ForeignFutureStructF64",
    "ForeignFutureStructPointer",
    "ForeignFutureStructRustBuffer",
    "ForeignFutureStructVoid",
    "ForeignFutureCompleteU8",
    "ForeignFutureCompleteI8",
    "ForeignFutureCompleteU16",
    "ForeignFutureCompleteI16",
    "ForeignFutureCompleteU32",
    "ForeignFutureCompleteI32",
    "ForeignFutureCompleteU64",
    "ForeignFutureCompleteI64",
    "ForeignFutureCompleteF32",
    "ForeignFutureCompleteF64",
    "ForeignFutureCompletePointer",
    "ForeignFutureCompleteRustBuffer",
    "ForeignFutureCompleteVoid",
];

macro_rules! kotlin_type_renderer {
    ($TypeRenderer:ident, $source_file:literal) => {
        /// Renders Kotlin helper code for all types
        ///
        /// This template is a bit different than others in that it stores internal state from the render
        /// process.  Make sure to only call `render()` once.
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        #[allow(dead_code)]
        pub struct $TypeRenderer<'a> {
            module_name: &'a str,
            config: &'a Config,
            ci: &'a ComponentInterface,
            // Track included modules for the `include_once()` macro
            include_once_names: RefCell<HashSet<String>>,
            // Track imports added with the `add_import()` macro
            imports: RefCell<BTreeSet<ImportRequirement>>,
        }

        #[allow(dead_code)]
        impl<'a> $TypeRenderer<'a> {
            fn new(module_name: &'a str, config: &'a Config, ci: &'a ComponentInterface) -> Self {
                Self {
                    module_name,
                    config,
                    ci,
                    include_once_names: RefCell::new(HashSet::new()),
                    imports: RefCell::new(BTreeSet::new()),
                }
            }

            // Get the package name for an external type
            fn external_type_package_name(&self, module_path: &str, namespace: &str) -> String {
                // config overrides are keyed by the crate name, default fallback is the namespace.
                let crate_name = module_path.split("::").next().unwrap();
                match self.config.external_packages.get(crate_name) {
                    Some(name) => name.clone(),
                    // unreachable in library mode - all deps are in our config with correct namespace.
                    None => format!("uniffi.{namespace}"),
                }
            }

            // The following methods are used by the `Types.kt` macros.

            // Helper for the including a template, but only once.
            //
            // The first time this is called with a name it will return true, indicating that we should
            // include the template.  Subsequent calls will return false.
            fn include_once_check(&self, name: &str) -> bool {
                self.include_once_names
                    .borrow_mut()
                    .insert(name.to_string())
            }

            // Helper to add an import statement
            //
            // Call this inside your template to cause an import statement to be added at the top of the
            // file.  Imports will be sorted and de-deuped.
            //
            // Returns an empty string so that it can be used inside an askama `{{ }}` block.
            fn add_import(&self, name: &str) -> &str {
                self.imports.borrow_mut().insert(ImportRequirement::Import {
                    name: name.to_owned(),
                });
                ""
            }

            // Helper to check if a record can be serialized
            // We only allow records that store primitive types or other records and enums
            fn is_serializable(&self, rec: &Record) -> bool {
                for f in rec.fields() {
                    for inner_ty in f.iter_types() {
                        match inner_ty {
                            Type::Object { .. }
                            | Type::CallbackInterface { .. }
                            | Type::External { .. }
                            | Type::Custom { .. } => return false,
                            _ => return true,
                        }
                    }
                }
                true
            }
            // Helper to check if a enum variant can be serialized
            // We only allow records that store primitive types or other records and enums
            fn is_variant_serializable(&self, rec: &Variant) -> bool {
                for f in rec.fields() {
                    for inner_ty in f.iter_types() {
                        match inner_ty {
                            Type::Object { .. }
                            | Type::CallbackInterface { .. }
                            | Type::External { .. }
                            | Type::Custom { .. } => return false,
                            _ => return true,
                        }
                    }
                }
                true
            }

            // Like add_import, but arranges for `import name as as_name`
            fn add_import_as(&self, name: &str, as_name: &str) -> &str {
                self.imports
                    .borrow_mut()
                    .insert(ImportRequirement::ImportAs {
                        name: name.to_owned(),
                        as_name: as_name.to_owned(),
                    });
                ""
            }
        }
    };
}

macro_rules! kotlin_wrapper {
    ($KotlinWrapper:ident, $TypeRenderer:ident, $source_file:literal) => {
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        #[allow(dead_code)]
        pub struct $KotlinWrapper<'a> {
            module_name: &'a str,
            config: Config,
            ci: &'a ComponentInterface,
            type_helper_code: String,
            type_imports: BTreeSet<ImportRequirement>,
        }

        #[allow(dead_code)]
        impl<'a> $KotlinWrapper<'a> {
            pub fn new(module_name: &'a str, config: Config, ci: &'a ComponentInterface) -> Self {
                let type_renderer = $TypeRenderer::new(module_name, &config, ci);
                let type_helper_code = type_renderer.render().unwrap();
                let type_imports = type_renderer.imports.into_inner();
                Self {
                    module_name,
                    config,
                    ci,
                    type_helper_code,
                    type_imports,
                }
            }

            pub fn initialization_fns(&self) -> Vec<String> {
                self.ci
                    .iter_types()
                    .map(|t| KotlinCodeOracle.find(t))
                    .filter_map(|ct| ct.initialization_fn())
                    .collect()
            }

            pub fn imports(&self) -> Vec<ImportRequirement> {
                self.type_imports.iter().cloned().collect()
            }

            pub fn ffi_definitions_no_builtins(&self) -> impl Iterator<Item = FfiDefinition> + '_ {
                self.ci
                    .ffi_definitions()
                    .filter(|d| !FFI_BUILTINS.contains(&d.name()))
            }
        }
    };
}

kotlin_type_renderer!(CommonTypeRenderer, "common/Types.kt");
kotlin_wrapper!(CommonKotlinWrapper, CommonTypeRenderer, "common/wrapper.kt");

kotlin_type_renderer!(AndroidJvmTypeRenderer, "android+jvm/Types.kt");
kotlin_wrapper!(
    AndroidJvmKotlinWrapper,
    AndroidJvmTypeRenderer,
    "android+jvm/wrapper.kt"
);

kotlin_type_renderer!(NativeTypeRenderer, "native/Types.kt");
kotlin_wrapper!(NativeKotlinWrapper, NativeTypeRenderer, "native/wrapper.kt");

#[derive(Template)]
#[template(syntax = "c", escape = "none", path = "headers/wrapper.h")]
#[allow(dead_code)]
pub struct HeaderKotlinWrapper<'ci> {
    #[allow(dead_code)]
    config: Config,
    ci: &'ci ComponentInterface,
}

impl<'ci> HeaderKotlinWrapper<'ci> {
    pub fn new(config: Config, ci: &'ci ComponentInterface) -> Self {
        Self { config, ci }
    }
}

#[derive(Clone)]
pub struct KotlinCodeOracle;

impl KotlinCodeOracle {
    fn find(&self, type_: &Type) -> Box<dyn CodeType> {
        type_.clone().as_type().as_codetype()
    }

    /// Get the idiomatic Kotlin rendering of a class name (for enums, records, errors, etc).
    fn class_name(&self, ci: &ComponentInterface, nm: &str) -> String {
        let name = nm.to_string().to_upper_camel_case();
        // fixup errors.
        ci.is_name_used_as_error(nm)
            .then(|| self.convert_error_suffix(&name))
            .unwrap_or(name)
    }

    fn convert_error_suffix(&self, nm: &str) -> String {
        match nm.strip_suffix("Error") {
            None => nm.to_string(),
            Some(stripped) => format!("{stripped}Exception"),
        }
    }

    /// Get the idiomatic Kotlin rendering of a function name.
    fn fn_name(&self, nm: &str) -> String {
        format!("`{}`", nm.to_string().to_lower_camel_case())
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    fn var_name(&self, nm: &str) -> String {
        format!("`{}`", self.var_name_raw(nm))
    }

    /// `var_name` without the backticks.  Useful for using in `@Structure.FieldOrder`.
    pub fn var_name_raw(&self, nm: &str) -> String {
        header_escape_name(&nm.to_lower_camel_case()).unwrap()
    }

    pub fn var_name_raw_noescape(&self, nm: &str) -> String {
        header_noescape_name(&nm.to_lower_camel_case()).unwrap()
    }

    /// Get the idiomatic Kotlin rendering of an individual enum variant.
    fn enum_variant_name(&self, nm: &str) -> String {
        nm.to_string().to_shouty_snake_case()
    }

    /// Get the idiomatic Kotlin rendering of an FFI callback function name
    fn ffi_callback_name(&self, nm: &str) -> String {
        format!("Uniffi{}", nm.to_upper_camel_case())
    }

    fn ffi_callback_name_header(&self, nm: &str) -> String {
        format!("Uniffi{}", nm.to_upper_camel_case())
    }

    /// Get the idiomatic Kotlin rendering of an FFI struct name
    fn ffi_struct_name(&self, nm: &str) -> String {
        format!("Uniffi{}", nm.to_upper_camel_case())
    }

    fn ffi_struct_name_header(&self, nm: &str) -> String {
        format!("Uniffi{}", nm.to_upper_camel_case())
    }

    fn ffi_type_label_by_value(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::RustBuffer(_) => format!("{}ByValue", self.ffi_type_label(ffi_type)),
            FfiType::Struct(name) => format!("{}UniffiByValue", self.ffi_struct_name(name)),
            FfiType::Callback(name) => self.ffi_callback_name(name).to_string(),
            _ => self.ffi_type_label(ffi_type),
        }
    }

    fn ffi_type_label_for_ffi_function(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::RustBuffer(_) => format!("{}ByValue", self.ffi_type_label(ffi_type)),
            FfiType::Struct(name) => format!("{}UniffiByValue", self.ffi_struct_name(name)),
            // FfiType::Callback(name) => self.ffi_callback_name(name).to_string(),
            _ => self.ffi_type_label(ffi_type),
        }
    }

    /// FFI type name to use inside structs
    ///
    /// The main requirement here is that all types must have default values or else the struct
    /// won't work in some JNA contexts.
    fn ffi_type_label_for_ffi_struct(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            // Make callbacks function pointers nullable. This matches the semantics of a C
            // function pointer better and allows for `null` as a default value.
            // NOTE: Type any used here, as native and jvm types differ.
            FfiType::Callback(_name) => "Any?".into(), // format!("{}?", self.ffi_callback_name(name)),
            _ => self.ffi_type_label_by_value(ffi_type),
        }
    }

    /// FFI type name to use inside structs
    ///
    /// The main requirement here is that all types must have default values or else the struct
    /// won't work in some JNA contexts.
    fn ffi_type_label_for_ffi_struct_inner(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            // Make callbacks function pointers nullable. This matches the semantics of a C
            // function pointer better and allows for `null` as a default value.
            // NOTE: Type any used here, as native and jvm types differ.
            FfiType::Callback(name) => format!("{}?", self.ffi_callback_name(name)),
            _ => self.ffi_type_label_by_value(ffi_type),
        }
    }

    fn callback_label_name(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            // Make callbacks function pointers nullable. This matches the semantics of a C
            // function pointer better and allows for `null` as a default value.
            // NOTE: Type any used here, as native and jvm types differ.
            FfiType::Callback(name) => format!("{}?", self.ffi_callback_name(name)),
            _ => "".into(),
        }
    }

    /// Default values for FFI
    ///
    /// This is used to:
    ///   - Set a default return value for error results
    ///   - Set a default for structs, which JNA sometimes requires
    fn ffi_default_value(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::UInt8 | FfiType::Int8 => "0.toByte()".to_owned(),
            FfiType::UInt16 | FfiType::Int16 => "0.toShort()".to_owned(),
            FfiType::UInt32 | FfiType::Int32 => "0".to_owned(),
            FfiType::UInt64 | FfiType::Int64 => "0.toLong()".to_owned(),
            FfiType::Float32 => "0.0f".to_owned(),
            FfiType::Float64 => "0.0".to_owned(),
            // NOTE: NullPointer is the same as Pointer.NULL
            FfiType::RustArcPtr(_) => "NullPointer".to_owned(),
            FfiType::RustBuffer(_) => "RustBufferHelper.allocValue()".to_owned(),
            FfiType::Callback(_) => "null".to_owned(),
            FfiType::RustCallStatus => "UniffiRustCallStatusHelper.allocValue()".to_owned(),
            _ => unimplemented!("ffi_default_value: {ffi_type:?}"),
        }
    }

    fn ffi_type_label_by_reference(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::Int8
            | FfiType::UInt8
            | FfiType::Int16
            | FfiType::UInt16
            | FfiType::Int32
            | FfiType::UInt32
            | FfiType::Int64
            | FfiType::UInt64
            | FfiType::Float32
            | FfiType::Float64 => format!("{}ByReference", self.ffi_type_label(ffi_type)),
            FfiType::RustArcPtr(_) => "PointerByReference".to_owned(),
            // JNA structs default to ByReference
            FfiType::RustBuffer(_) | FfiType::Struct(_) => self.ffi_type_label(ffi_type),
            _ => panic!("{ffi_type:?} by reference is not implemented"),
        }
    }

    fn ffi_type_label_by_reference_header(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::Int8
            | FfiType::UInt8
            | FfiType::Int16
            | FfiType::UInt16
            | FfiType::Int32
            | FfiType::UInt32
            | FfiType::Int64
            | FfiType::UInt64
            | FfiType::Float32
            | FfiType::Float64 => format!("{} *", self.ffi_type_label_header(ffi_type)),
            FfiType::RustArcPtr(_) => "void **".to_owned(),
            // JNA structs default to ByReference
            FfiType::RustBuffer(_) | FfiType::Struct(_) => {
                format!("{} *", self.ffi_type_label_header(ffi_type))
            }
            _ => panic!("{ffi_type:?} by reference is not implemented"),
        }
    }

    fn ffi_type_label(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            // Note that unsigned integers in Kotlin are currently experimental, but java.nio.ByteBuffer does not
            // support them yet. Thus, we use the signed variants to represent both signed and unsigned
            // types from the component API.
            FfiType::Int8 | FfiType::UInt8 => "Byte".to_string(),
            FfiType::Int16 | FfiType::UInt16 => "Short".to_string(),
            FfiType::Int32 | FfiType::UInt32 => "Int".to_string(),
            FfiType::Int64 | FfiType::UInt64 => "Long".to_string(),
            FfiType::Float32 => "Float".to_string(),
            FfiType::Float64 => "Double".to_string(),
            FfiType::Handle => "Long".to_string(),
            FfiType::RustArcPtr(_) => "Pointer?".to_string(),
            FfiType::RustBuffer(maybe_external) => match maybe_external {
                Some(external_meta) => format!("RustBuffer{}", external_meta.name),
                None => "RustBuffer".to_string(),
            },
            FfiType::RustCallStatus => "UniffiRustCallStatusByValue".to_string(),
            FfiType::ForeignBytes => "ForeignBytesByValue".to_string(),
            FfiType::Callback(_) => "Any".to_string(),
            FfiType::Struct(name) => self.ffi_struct_name(name),
            FfiType::Reference(inner) => self.ffi_type_label_by_reference(inner),
            FfiType::VoidPointer => "Pointer".to_string(),
        }
    }

    fn ffi_type_label_header(&self, ffi_type: &FfiType) -> String {
        match ffi_type {
            // Note that unsigned integers in Kotlin are currently experimental, but java.nio.ByteBuffer does not
            // support them yet. Thus, we use the signed variants to represent both signed and unsigned
            // types from the component API.
            FfiType::Int8 | FfiType::UInt8 => "int8_t".to_string(),
            FfiType::Int16 | FfiType::UInt16 => "int16_t".to_string(),
            FfiType::Int32 | FfiType::UInt32 => "int32_t".to_string(),
            FfiType::Int64 | FfiType::UInt64 => "int64_t".to_string(),
            FfiType::Float32 => "float".to_string(),
            FfiType::Float64 => "double".to_string(),
            FfiType::Handle => "int64_t".to_string(),
            FfiType::RustArcPtr(_) => "void *".to_string(),
            FfiType::RustBuffer(maybe_external) => match maybe_external {
                Some(external_meta) => format!("RustBuffer{}", external_meta.name),
                None => "RustBuffer".to_string(),
            },
            FfiType::RustCallStatus => "UniffiRustCallStatus".to_string(),
            FfiType::ForeignBytes => "ForeignBytes".to_string(),
            FfiType::Callback(name) => self.ffi_callback_name_header(name),
            FfiType::Struct(name) => self.ffi_struct_name_header(name),
            FfiType::Reference(inner) => self.ffi_type_label_by_reference_header(inner),
            FfiType::VoidPointer => "void *".to_string(),
        }
    }

    /// Get the name of the interface and class name for an object.
    ///
    /// If we support callback interfaces, the interface name is the object name, and the class name is derived from that.
    /// Otherwise, the class name is the object name and the interface name is derived from that.
    ///
    /// This split determines what types `FfiConverter.lower()` inputs.  If we support callback
    /// interfaces, `lower` must lower anything that implements the interface.  If not, then lower
    /// only lowers the concrete class.
    fn object_names(&self, ci: &ComponentInterface, obj: &Object) -> (String, String) {
        let class_name = self.class_name(ci, obj.name());
        if obj.has_callback_interface() {
            let impl_name = format!("{class_name}Impl");
            (class_name, impl_name)
        } else {
            (format!("{class_name}Interface"), class_name)
        }
    }
}

trait AsCodeType {
    fn as_codetype(&self) -> Box<dyn CodeType>;
}

impl<T: AsType> AsCodeType for T {
    fn as_codetype(&self) -> Box<dyn CodeType> {
        // Map `Type` instances to a `Box<dyn CodeType>` for that type.
        //
        // There is a companion match in `templates/Types.kt` which performs a similar function for the
        // template code.
        //
        //   - When adding additional types here, make sure to also add a match arm to the `Types.kt` template.
        //   - To keep things manageable, let's try to limit ourselves to these 2 mega-matches
        match self.as_type() {
            Type::UInt8 => Box::new(primitives::UInt8CodeType),
            Type::Int8 => Box::new(primitives::Int8CodeType),
            Type::UInt16 => Box::new(primitives::UInt16CodeType),
            Type::Int16 => Box::new(primitives::Int16CodeType),
            Type::UInt32 => Box::new(primitives::UInt32CodeType),
            Type::Int32 => Box::new(primitives::Int32CodeType),
            Type::UInt64 => Box::new(primitives::UInt64CodeType),
            Type::Int64 => Box::new(primitives::Int64CodeType),
            Type::Float32 => Box::new(primitives::Float32CodeType),
            Type::Float64 => Box::new(primitives::Float64CodeType),
            Type::Boolean => Box::new(primitives::BooleanCodeType),
            Type::String => Box::new(primitives::StringCodeType),
            Type::Bytes => Box::new(primitives::BytesCodeType),

            Type::Timestamp => Box::new(miscellany::TimestampCodeType),
            Type::Duration => Box::new(miscellany::DurationCodeType),

            Type::Enum { name, .. } => Box::new(enum_::EnumCodeType::new(name)),
            Type::Object { name, imp, .. } => Box::new(object::ObjectCodeType::new(name, imp)),
            Type::Record { name, .. } => Box::new(record::RecordCodeType::new(name)),
            Type::CallbackInterface { name, .. } => {
                Box::new(callback_interface::CallbackInterfaceCodeType::new(name))
            }
            Type::Optional { inner_type } => {
                Box::new(compounds::OptionalCodeType::new(*inner_type))
            }
            Type::Sequence { inner_type } => {
                Box::new(compounds::SequenceCodeType::new(*inner_type))
            }
            Type::Map {
                key_type,
                value_type,
            } => Box::new(compounds::MapCodeType::new(*key_type, *value_type)),
            Type::External { name, .. } => Box::new(external::ExternalCodeType::new(name)),
            Type::Custom { name, .. } => Box::new(custom::CustomCodeType::new(name)),
        }
    }
}

mod filters {
    pub use uniffi_bindgen::backend::filters::*;
    use uniffi_meta::LiteralMetadata;
    use variant::VariantCodeType;

    use super::*;

    pub(super) fn type_name(
        as_ct: &impl AsCodeType,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().type_label(ci))
    }

    // Workaround problem with impl AsCodeType for &Variant (see variant.rs).
    pub fn variant_type_name(
        v: &Variant,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(VariantCodeType { v: v.clone() }.type_label(ci))
    }

    pub(super) fn canonical_name(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().canonical_name())
    }

    pub(super) fn ffi_converter_name(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().ffi_converter_name())
    }

    pub(super) fn lower_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!(
            "{}.lower",
            as_ct.as_codetype().ffi_converter_name()
        ))
    }

    pub(super) fn allocation_size_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!(
            "{}.allocationSize",
            as_ct.as_codetype().ffi_converter_name()
        ))
    }

    pub(super) fn write_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!(
            "{}.write",
            as_ct.as_codetype().ffi_converter_name()
        ))
    }

    pub(super) fn lift_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.lift", as_ct.as_codetype().ffi_converter_name()))
    }

    pub(super) fn read_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.read", as_ct.as_codetype().ffi_converter_name()))
    }

    pub fn render_literal(
        literal: &Literal,
        as_ct: &impl AsType,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().literal(literal, ci))
    }

    // Get the idiomatic Kotlin rendering of an integer.
    fn int_literal(t: &Option<Type>, base10: String) -> Result<String, askama::Error> {
        if let Some(t) = t {
            match t {
                Type::Int8 | Type::Int16 | Type::Int32 | Type::Int64 => Ok(base10),
                Type::UInt8 | Type::UInt16 | Type::UInt32 | Type::UInt64 => Ok(base10 + "u"),
                _ => Err(askama::Error::Custom(Box::new(UniFFIError::new(
                    "Only ints are supported.".to_string(),
                )))),
            }
        } else {
            Err(askama::Error::Custom(Box::new(UniFFIError::new(
                "Enum hasn't defined a repr".to_string(),
            ))))
        }
    }

    // Get the idiomatic Kotlin rendering of an individual enum variant's discriminant
    pub fn variant_discr_literal(e: &Enum, index: &usize) -> Result<String, askama::Error> {
        let literal = e.variant_discr(*index).expect("invalid index");
        match literal {
            // Kotlin doesn't convert between signed and unsigned by default
            // so we'll need to make sure we define the type as appropriately
            LiteralMetadata::UInt(v, _, _) => int_literal(e.variant_discr_type(), v.to_string()),
            LiteralMetadata::Int(v, _, _) => int_literal(e.variant_discr_type(), v.to_string()),
            _ => Err(askama::Error::Custom(Box::new(UniFFIError::new(
                "Only ints are supported.".to_string(),
            )))),
        }
    }

    pub fn ffi_type_name_by_value(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_by_value(type_))
    }

    pub fn ffi_type_name_for_ffi_function(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_for_ffi_function(type_))
    }

    pub fn ffi_type_name(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label(type_))
    }

    pub fn is_callback(type_: &FfiType) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::Callback(_)))
    }

    pub fn is_rustbuffer(type_: &FfiType) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::RustBuffer(_)))
    }

    pub fn is_foreignbytes(type_: &FfiType) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::ForeignBytes))
    }

    /// Append a `_` if the name is a valid c/c++ keyword
    pub fn header_escape_name(nm: &str) -> Result<String, askama::Error> {
        if CPP_KEYWORDS.contains(&nm) {
            Ok(format!("{nm}_"))
        } else {
            Ok(nm.to_owned())
        }
    }

    /// Append a `_` if the name is a valid c/c++ keyword
    pub fn header_noescape_name(nm: &str) -> Result<String, askama::Error> {
        Ok(nm.to_owned())
    }

    pub fn header_ffi_type_name(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_header(type_))
    }

    pub fn ffi_type_name_for_ffi_struct(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_for_ffi_struct(type_))
    }

    pub fn ffi_type_name_for_ffi_struct_inner(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_for_ffi_struct_inner(type_))
    }

    pub fn is_pointer_type(type_: &FfiType) -> Result<bool, askama::Error> {
        Ok(match type_ {
            FfiType::RustArcPtr(_) | FfiType::VoidPointer => true,
            _ => false,
        })
    }

    pub fn ffi_type_name_for_ffi_callback(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.callback_label_name(type_))
    }

    pub fn ffi_default_value(type_: FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_default_value(&type_))
    }

    /// Get the idiomatic Kotlin rendering of a function name.
    pub fn class_name(nm: &str, ci: &ComponentInterface) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.class_name(ci, nm))
    }

    /// Get the idiomatic Kotlin rendering of a function name.
    pub fn fn_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.fn_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    pub fn var_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name(nm))
    }

    /// Check if type is Option
    pub fn is_optional(as_ct: &impl AsCodeType) -> Result<bool, askama::Error> {
        Ok(as_ct.as_codetype().is_optional())
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    pub fn var_name_raw_noescape(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name_raw_noescape(nm))
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    pub fn var_name_raw(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name_raw(nm))
    }

    /// Get a String representing the name used for an individual enum variant.
    pub fn variant_name(v: &Variant) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.enum_variant_name(v.name()))
    }

    pub fn error_variant_name(v: &Variant) -> Result<String, askama::Error> {
        let name = v.name().to_string().to_upper_camel_case();
        Ok(KotlinCodeOracle.convert_error_suffix(&name))
    }

    /// Get the idiomatic Kotlin rendering of an FFI callback function name
    pub fn ffi_callback_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_callback_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of an FFI struct name
    pub fn ffi_struct_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_struct_name(nm))
    }

    pub fn object_names(
        obj: &Object,
        ci: &ComponentInterface,
    ) -> Result<(String, String), askama::Error> {
        Ok(KotlinCodeOracle.object_names(ci, obj))
    }

    pub fn async_poll(
        callable: impl Callable,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_poll(ci);
        Ok(format!(
            "{{ future, callback, continuation -> UniffiLib.INSTANCE.{ffi_func}(future, callback, continuation)!! }}"
        ))
    }

    pub fn async_complete(
        callable: impl Callable,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_complete(ci);
        let call = format!("UniffiLib.INSTANCE.{ffi_func}(future, continuation)");
        let call = match callable.return_type() {
            Some(Type::External {
                kind: ExternalKind::DataClass,
                name,
                ..
            }) => {
                // Need to convert the RustBuffer from our package to the RustBuffer of the external package
                let suffix = KotlinCodeOracle.class_name(ci, &name);
                format!("{call}.let {{ RustBuffer{suffix}ByValue(it.capacity, it.len, it.data) }}")
            }
            _ => call,
        };
        Ok(format!("{{ future, continuation -> {call} }}"))
    }

    pub fn async_free(
        callable: impl Callable,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_free(ci);
        Ok(format!(
            "{{ future -> UniffiLib.INSTANCE.{ffi_func}(future) }}"
        ))
    }

    pub fn async_cancel(
        callable: impl Callable,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_cancel(ci);
        Ok(format!(
            "{{ future -> UniffiLib.INSTANCE.{ffi_func}(future) }}"
        ))
    }

    /// Remove the "`" chars we put around function/variable names
    ///
    /// These are used to avoid name clashes with kotlin identifiers, but sometimes you want to
    /// render the name unquoted.  One example is the message property for errors where we want to
    /// display the name for the user.
    pub fn unquote(nm: &str) -> Result<String, askama::Error> {
        Ok(nm.trim_matches('`').to_string())
    }

    /// Get the idiomatic Kotlin rendering of docstring
    pub fn docstring(docstring: &str, spaces: &i32) -> Result<String, askama::Error> {
        let middle = textwrap::indent(&textwrap::dedent(docstring), " * ");
        let wrapped = format!("/**\n{middle}\n */");

        let spaces = usize::try_from(*spaces).unwrap_or_default();
        Ok(textwrap::indent(&wrapped, &" ".repeat(spaces)))
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_kotlin_version() {
        assert_eq!(
            KotlinVersion::parse("1.2.3").unwrap(),
            KotlinVersion::new(1, 2, 3)
        );
        assert_eq!(
            KotlinVersion::parse("2.3").unwrap(),
            KotlinVersion::new(2, 3, 0),
        );
        assert_eq!(
            KotlinVersion::parse("2").unwrap(),
            KotlinVersion::new(2, 0, 0),
        );
        assert!(KotlinVersion::parse("2.").is_err());
        assert!(KotlinVersion::parse("").is_err());
        assert!(KotlinVersion::parse("A.B.C").is_err());
        assert!(KotlinVersion::new(1, 2, 3) > KotlinVersion::new(0, 1, 2));
        assert!(KotlinVersion::new(1, 2, 3) > KotlinVersion::new(0, 100, 0));
        assert!(KotlinVersion::new(10, 0, 0) > KotlinVersion::new(1, 10, 0));
    }
}
