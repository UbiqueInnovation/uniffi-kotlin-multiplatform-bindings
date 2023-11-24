use std::borrow::Borrow;
use std::cell::RefCell;
use std::collections::{BTreeSet, HashMap, HashSet};
use std::fmt::Debug;

use anyhow::{Context, Result};
use askama::Template;
use heck::{ToLowerCamelCase, ToShoutySnakeCase, ToUpperCamelCase};
use serde::{Deserialize, Serialize};
use uniffi_bindgen::{BindingsConfig, ComponentInterface};
use uniffi_bindgen::backend::TemplateExpression;
use uniffi_bindgen::interface::*;

use crate::KotlinMultiplatformBindings;
use crate::gen_kotlin_multiplatform::variant::VariantCodeType;

mod callback_interface;
mod compounds;
mod custom;
mod enum_;
mod executor;
mod external;
mod miscellany;
mod object;
mod primitives;
mod record;
mod variant;

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
    package_name: Option<String>,
    cdylib_name: Option<String>,
    #[serde(default)]
    custom_types: HashMap<String, CustomTypeConfig>,
    #[serde(default)]
    external_packages: HashMap<String, String>,
}

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct CustomTypeConfig {
    imports: Option<Vec<String>>,
    type_name: Option<String>,
    into_custom: TemplateExpression,
    from_custom: TemplateExpression,
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

// Generate kotlin bindings for the given ComponentInterface, as a string.
pub fn generate_bindings(
    config: &Config,
    ci: &ComponentInterface,
) -> Result<KotlinMultiplatformBindings> {
    let common = KotlinCommonWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Common bindings")?;

    let jvm = KotlinJvmWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Jvm bindings")?;

    let native = KotlinNativeWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Native bindings")?;

    let header = KotlinHeaderWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Native header")?;

    Ok(KotlinMultiplatformBindings {
        common,
        jvm,
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

macro_rules! kotlin_type_renderer_template {
    ($KotlinTemplate:ident, $source_file:literal) => {
        /// Renders Kotlin helper code for all types
        ///
        /// This template is a bit different than others in that it stores internal state from the render
        /// process.  Make sure to only call `render()` once.
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        pub struct $KotlinTemplate<'a> {
            #[allow(dead_code)]
            config: &'a Config,
            ci: &'a ComponentInterface,
            // Track included modules for the `include_once()` macro
            include_once_names: RefCell<HashSet<String>>,
            // Track imports added with the `add_import()` macro
            imports: RefCell<BTreeSet<ImportRequirement>>,
        }

        impl<'a> $KotlinTemplate<'a> {
            fn new(config: &'a Config, ci: &'a ComponentInterface) -> Self {
                Self {
                    config,
                    ci,
                    include_once_names: RefCell::new(HashSet::new()),
                    imports: RefCell::new(BTreeSet::new()),
                }
            }

            // Get the package name for an external type
            #[allow(dead_code)]
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
            #[allow(dead_code)]
            fn add_import(&self, name: &str) -> &str {
                self.imports.borrow_mut().insert(ImportRequirement::Import {
                    name: name.to_owned(),
                });
                ""
            }

            // Like add_import, but arranges for `import name as as_name`
            #[allow(dead_code)]
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

macro_rules! kotlin_wrapper_template {
    ($KotlinWrapperTemplate:ident, $KotlinTypeRenderer:ident, $source_file:literal) => {
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        pub struct $KotlinWrapperTemplate<'ci> {
            config: Config,
            ci: &'ci ComponentInterface,
            type_helper_code: String,
            type_imports: BTreeSet<ImportRequirement>,
        }

        impl<'ci> $KotlinWrapperTemplate<'ci> {
            pub fn new(config: Config, ci: &'ci ComponentInterface) -> Self {
                let type_renderer = $KotlinTypeRenderer::new(&config, ci);
                let type_helper_code = type_renderer.render().unwrap();
                let type_imports = type_renderer.imports.into_inner();
                Self {
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
                    .chain(
                        self.ci.has_async_fns()
                            .then(|| "registerUniffiRustFutureContinuationCallback".into()),
                    )
                    .collect()
            }

            pub fn imports(&self) -> Vec<ImportRequirement> {
                self.type_imports.iter().cloned().collect()
            }
        }
    };
}

kotlin_type_renderer_template!(KotlinCommonTypeRenderer, "common/Types.kt");
kotlin_wrapper_template!(KotlinCommonWrapper, KotlinCommonTypeRenderer, "common/wrapper.kt");

kotlin_type_renderer_template!(KotlinJvmTypeRenderer, "jvm/Types.kt");
kotlin_wrapper_template!(KotlinJvmWrapper, KotlinJvmTypeRenderer, "jvm/wrapper.kt");

kotlin_type_renderer_template!(KotlinNativeTypeRenderer, "native/Types.kt");
kotlin_wrapper_template!(KotlinNativeWrapper, KotlinNativeTypeRenderer, "native/wrapper.kt");

#[derive(Template)]
#[template(syntax = "c", escape = "none", path = "headers/wrapper.h")]
pub struct KotlinHeaderWrapper<'ci> {
    #[allow(dead_code)]
    config: Config,
    ci: &'ci ComponentInterface,
}

impl<'ci> KotlinHeaderWrapper<'ci> {
    pub fn new(config: Config, ci: &'ci ComponentInterface) -> Self {
        Self {
            config,
            ci,
        }
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

    /// Get the idiomatic Kotlin rendering of an exception name
    ///
    /// This replaces "Error" at the end of the name with "Exception".  Rust code typically uses
    /// "Error" for any type of error but in the Java world, "Error" means a non-recoverable error
    /// and is distinguished from an "Exception".
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
        format!("`{}`", nm.to_string().to_lower_camel_case())
    }

    /// Get the idiomatic Kotlin rendering of an individual enum variant.
    fn enum_variant_name(&self, nm: &str) -> String {
        nm.to_string().to_shouty_snake_case()
    }

    fn ffi_type_label(ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::Int8 => "Byte".to_string(),
            FfiType::UInt8 => "UByte".to_string(),
            FfiType::Int16 => "Short".to_string(),
            FfiType::UInt16 => "UShort".to_string(),
            FfiType::Int32 => "Int".to_string(),
            FfiType::UInt32 => "UInt".to_string(),
            FfiType::Int64 => "Long".to_string(),
            FfiType::UInt64 => "ULong".to_string(),
            FfiType::Float32 => "Float".to_string(),
            FfiType::Float64 => "Double".to_string(),
            FfiType::RustArcPtr(_) => "Pointer".to_string(),
            FfiType::RustBuffer(_) => "RustBuffer".to_string(),
            FfiType::ForeignBytes => "ForeignBytes".to_string(),
            FfiType::ForeignCallback => "ForeignCallback".to_string(),
            FfiType::ForeignExecutorHandle => "ULong".to_string(), // TODO USize
            FfiType::ForeignExecutorCallback => "UniFfiForeignExecutorCallback".to_string(),
            FfiType::RustFutureHandle => "Pointer".to_string(),
            FfiType::RustFutureContinuationCallback => {
                "UniFfiRustFutureContinuationCallbackType".to_string()
            }
            FfiType::RustFutureContinuationData => "ULong".to_string(), // TODO USize
        }
    }

    fn ffi_header_type_label(ffi_type: &FfiType) -> String {
        match ffi_type {
            FfiType::Int8 => "int8_t".into(),
            FfiType::UInt8 => "uint8_t".into(),
            FfiType::Int16 => "int16_t".into(),
            FfiType::UInt16 => "uint16_t".into(),
            FfiType::Int32 => "int32_t".into(),
            FfiType::UInt32 => "uint32_t".into(),
            FfiType::Int64 => "int64_t".into(),
            FfiType::UInt64 => "uint64_t".into(),
            FfiType::Float32 => "float".into(),
            FfiType::Float64 => "double".into(),
            FfiType::RustArcPtr(_) => "void*_Nonnull".into(),
            FfiType::RustBuffer(_) => "RustBuffer".into(),
            FfiType::ForeignBytes => "ForeignBytes".into(),
            FfiType::ForeignCallback => "ForeignCallback  _Nonnull".into(),
            FfiType::ForeignExecutorHandle => "size_t".into(),
            FfiType::ForeignExecutorCallback => "UniFfiForeignExecutorCallback _Nonnull".into(),
            FfiType::RustFutureHandle => "void*".into(),
            FfiType::RustFutureContinuationCallback => { "UniFfiRustFutureContinuation _Nonnull".into() }
            FfiType::RustFutureContinuationData => "size_t".to_string(),
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
            Type::Object { name, .. } => Box::new(object::ObjectCodeType::new(name)),
            Type::Record { name, .. } => Box::new(record::RecordCodeType::new(name)),
            Type::CallbackInterface { name, .. } => {
                Box::new(callback_interface::CallbackInterfaceCodeType::new(name))
            }
            Type::ForeignExecutor => Box::new(executor::ForeignExecutorCodeType),
            Type::Optional { inner_type } => {
                Box::new(compounds::OptionalCodeType::new(*inner_type))
            }
            Type::Sequence { inner_type } => {
                Box::new(compounds::SequenceCodeType::new(*inner_type))
            }
            Type::Map { key_type, value_type } => {
                Box::new(compounds::MapCodeType::new(*key_type, *value_type))
            }
            Type::External { name, .. } => Box::new(external::ExternalCodeType::new(name)),
            Type::Custom { name, .. } => Box::new(custom::CustomCodeType::new(name)),
        }
    }
}

pub mod filters {
    pub use uniffi_bindgen::backend::filters::*;

    use super::*;

    /// List the definitions of all FFI functions in the interface.
    ///
    /// The set of FFI functions is derived automatically from the set of higher-level types
    /// along with the builtin FFI helper functions.
    pub fn iter_ffi_function_definitions(
        ci: &ComponentInterface
    ) -> Result<Box<dyn Iterator<Item = FfiFunction> + '_>, askama::Error> {
        if ci.has_async_fns() {
            Ok(Box::new(ci.iter_ffi_function_definitions()))
        } else {
            Ok(Box::new(ci.iter_ffi_function_definitions_non_async()))
        }
    }

    pub(super) fn type_name(
        as_ct: &impl AsCodeType,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().type_label(ci))
    }

    // Workaround problem with impl AsCodeType for &Variant (see variant.rs).
    pub fn variant_type_name(
        v: &Variant,
        ci: &ComponentInterface
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
        Ok(format!("{}.lower", as_ct.as_codetype().ffi_converter_name()))
    }

    pub(super) fn allocation_size_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.allocationSize", as_ct.as_codetype().ffi_converter_name()))
    }

    pub(super) fn write_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.write", as_ct.as_codetype().ffi_converter_name()))
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

    /// Get the Kotlin syntax for representing a given low-level `FfiType`.
    pub fn ffi_type_name(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle::ffi_type_label(type_))
    }

    /// Get the C syntax for representing a given low-level `FfiType`.
    pub fn ffi_header_type_name(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle::ffi_header_type_label(type_))
    }

    /// Get the idiomatic Kotlin rendering of a function name.
    pub fn fn_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.fn_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    pub fn var_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name(nm))
    }

    /// Get a String representing the name used for an individual enum variant.
    pub fn variant_name(v: &Variant) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.enum_variant_name(v.name()))
    }

    pub fn error_variant_name(v: &Variant) -> Result<String, askama::Error> {
        let name = v.name().to_string().to_upper_camel_case();
        Ok(KotlinCodeOracle.convert_error_suffix(&name))
    }

    pub fn async_poll(
        callable: impl Callable,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_poll(ci);
        Ok(format!(
            "{{ future, continuation -> UniFFILib.{ffi_func}(future, continuation) }}"
        ))
    }

    pub fn async_complete(
        callable: impl Callable,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_complete(ci);
        let call = format!("UniFFILib.{ffi_func}(future, continuation)");
        let call = match callable.return_type() {
            Some(Type::External {
                 kind: ExternalKind::DataClass,
                 name,
                 ..
             }) => {
                // Need to convert the RustBuffer from our package to the RustBuffer of the external package
                let suffix = KotlinCodeOracle.class_name(ci, &name);
                format!("{call}.let {{ RustBuffer{suffix}.create(it.capacity, it.len, it.data) }}")
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
            "{{ future -> UniFFILib.{ffi_func}(future) }}"
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
}
