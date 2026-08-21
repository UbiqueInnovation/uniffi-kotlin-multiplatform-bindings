/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use std::borrow::Borrow;
use std::cell::RefCell;
use std::collections::{BTreeSet, HashMap, HashSet};
use std::fmt::Debug;

use anyhow::{anyhow, bail, Context, Result};
use askama::Template;
use heck::{ToLowerCamelCase, ToShoutySnakeCase, ToUpperCamelCase};
use serde::{Deserialize, Serialize};
use uniffi_bindgen::interface::*;

mod backend;
mod callback_interface;
mod compounds;
mod custom;
mod enum_;
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

/// Append a `_` if the name is a c/c++ keyword.
///
/// Plain helper so `KotlinCodeOracle` can call this directly; the askama filter of
/// the same name wraps it (filters are no longer ordinary callable fns in askama 0.14+).
fn header_escape_name_str(nm: &str) -> String {
    if CPP_KEYWORDS.contains(&nm) {
        format!("{nm}_")
    } else {
        nm.to_owned()
    }
}

trait CodeType: Debug {
    /// The language specific label used to reference this type. This will be used in
    /// method signatures and property declarations.
    #[cfg_attr(feature = "runtime", allow(dead_code))]
    fn type_label(&self, ci: &ComponentInterface) -> String;

    /// A representation of this type label that can be used as part of another
    /// identifier. e.g. `read_foo()`, or `FooInternals`.
    ///
    /// This is especially useful when creating specialized objects or methods to deal
    /// with this type only.

    #[cfg_attr(feature = "runtime", allow(dead_code))]
    fn canonical_name(&self) -> String;

    /// Render a default value.
    ///
    /// uniffi 0.30 made `#[uniffi(default)]` literals optional, so a default is now
    /// either an explicit literal or "whatever this type's own default is".
    ///
    /// This base impl only covers named types - records and objects - where "own
    /// default" is a no-argument constructor call and a literal is meaningless. Every
    /// type whose Kotlin rendering has no such constructor (primitives, sequences,
    /// maps, options, enums, custom types) overrides this.
    #[cfg_attr(feature = "runtime", allow(dead_code))]
    fn default(
        &self,
        default: &DefaultValue,
        ci: &ComponentInterface,
        _config: &Config,
    ) -> Result<String> {
        match default {
            DefaultValue::Default => Ok(format!("{}()", self.type_label(ci))),
            DefaultValue::Literal(_) => {
                bail!(
                    "Literals are not supported as a default for {}",
                    self.type_label(ci)
                )
            }
        }
    }

    #[cfg_attr(feature = "runtime", allow(dead_code))]
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
    #[cfg_attr(feature = "runtime", allow(dead_code))]
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
    #[serde(default)]
    mutable_records: HashSet<String>,
    #[serde(default)]
    omit_checksums: bool,
    generate_serializable_records: Option<bool>,
    skip_serializer_for: Option<Vec<String>>,
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
#[serde(default)]
pub struct CustomTypeConfig {
    imports: Option<Vec<String>>,
    type_name: Option<String>,
    // uniffi 0.29.1 replaced `TemplateExpression` with plain strings in which a
    // literal `{}` is substituted, and added `lift`/`lower` as the preferred
    // spelling of `into_custom`/`from_custom`. Both spellings stay supported.
    into_custom: String,
    lift: String,
    from_custom: String,
    lower: String,
}

impl CustomTypeConfig {
    fn lift(&self, name: &str) -> String {
        let converter = if self.lift.is_empty() {
            &self.into_custom
        } else {
            &self.lift
        };
        converter.replace("{}", name)
    }

    fn lower(&self, name: &str) -> String {
        let converter = if self.lower.is_empty() {
            &self.from_custom
        } else {
            &self.lower
        };
        converter.replace("{}", name)
    }
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

    /// The Kotlin package another crate's types live in.
    ///
    /// Config overrides are keyed by the crate name, and since uniffi 0.31 `module_path`
    /// is a full path rather than just the crate name. The fallback is unreachable in
    /// library mode - all deps are in our config with the correct namespace.
    pub fn external_package_name(&self, module_path: &str, namespace: &str) -> String {
        let crate_name = module_path.split("::").next().unwrap_or(module_path);
        match self.external_packages.get(crate_name) {
            Some(name) => name.clone(),
            None => format!("uniffi.{namespace}"),
        }
    }

    /// Whether to generate immutable records (`val` instead of `var`).
    fn generate_immutable_records(&self) -> bool {
        self.generate_immutable_records.unwrap_or(false)
    }

    /// Whether one specific record gets immutable fields.
    ///
    /// A record is immutable only if `generate_immutable_records` is on and the record is
    /// not listed in `mutable_records`, which is keyed by the record's name as Rust or the
    /// UDL declares it.
    pub fn is_record_immutable(&self, name: &str) -> bool {
        self.generate_immutable_records() && !self.mutable_records.contains(name)
    }

    /// Whether to leave the per-function API checksum check out of the generated
    /// `UniffiLib` initialiser.
    ///
    /// The check compares a checksum baked into the bindings against one the library
    /// reports, so that bindings and scaffolding built from different versions of the
    /// same API fail loudly at load time instead of corrupting arguments. Omitting it
    /// costs one FFI call per exported function at startup, and is only safe where the
    /// two are built together from the same source. The contract version check is not
    /// affected and always runs.
    pub fn omit_checksums(&self) -> bool {
        self.omit_checksums
    }
    /// Whether to use kotlinx Serializable annotation on the data class
    pub fn generate_serializable_records(&self) -> bool {
        self.generate_serializable_records.unwrap_or(false)
    }
    pub fn skip_serializer_for(&self) -> Vec<String> {
        self.skip_serializer_for
            .as_ref()
            .cloned()
            .unwrap_or_default()
    }
    /// Whether to use kotlinx Serializable annotation on the data class
    pub fn has_import_helpers(&self) -> bool {
        self.import_pointer_from.is_some()
    }
    pub fn import_helper_namespace(&self) -> Vec<String> {
        self.import_pointer_from
            .as_ref()
            .cloned()
            .unwrap_or_default()
    }

    #[cfg_attr(feature = "runtime", allow(dead_code))]
    pub(crate) fn use_enum_entries(&self) -> bool {
        self.get_kotlin_version() >= KotlinVersion::new(1, 9, 0)
    }

    /// Returns a `Version` with the contents of `kotlin_target_version`.
    /// If `kotlin_target_version` is not defined, version `0.0.0` will be used as a fallback.
    /// If it's not valid, this function will panic.
    #[cfg_attr(feature = "runtime", allow(dead_code))]
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
#[cfg_attr(feature = "runtime", allow(dead_code))]
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
    pub headers: NativeHeaderBindings,
}

pub struct NativeHeaderBindings {
    pub namespace_header: String,
    pub common_header: String,
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

    let namespace_header = HeaderKotlinWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Native header")?;

    let common_header = CommonHeaderKotlinWrapper::new(config.clone(), ci)
        .render()
        .context("failed to render common Kotlin header")?;

    let headers = NativeHeaderBindings {
        namespace_header,
        common_header,
    };

    Ok(MultiplatformBindings {
        common,
        jvm,
        android,
        native,
        headers,
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

/// FFI definitions that are identical for every namespace, and so are emitted once into
/// the shared `common.h` rather than into each namespace's own header.
///
/// Several of these were renamed in uniffi 0.29/0.30 (a name change only -- the FFI shape
/// is unchanged). Keeping the list in sync matters: a name that falls off it gets emitted
/// into every namespace header instead, which collides once more than one namespace is
/// compiled into the same cinterop module.
///   `ForeignFutureFree`        -> `ForeignFutureDroppedCallback`
///   `ForeignFuture`            -> `ForeignFutureDroppedCallbackStruct`
///   `ForeignFutureStruct{T}`   -> `ForeignFutureResult{T}`
/// The `Pointer` variants are gone entirely, since objects now lower to a u64 handle.
const FFI_BUILTINS: &'static [&'static str] = &[
    "RustFutureContinuationCallback",
    "ForeignFutureDroppedCallback",
    "CallbackInterfaceFree",
    "CallbackInterfaceClone",
    "ForeignFutureDroppedCallbackStruct",
    "ForeignFutureResultU8",
    "ForeignFutureResultI8",
    "ForeignFutureResultU16",
    "ForeignFutureResultI16",
    "ForeignFutureResultU32",
    "ForeignFutureResultI32",
    "ForeignFutureResultU64",
    "ForeignFutureResultI64",
    "ForeignFutureResultF32",
    "ForeignFutureResultF64",
    "ForeignFutureResultRustBuffer",
    "ForeignFutureResultVoid",
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
                self.config.external_package_name(module_path, namespace)
            }

            // uniffi 0.29 removed `Type::External`, so external types are now ordinary
            // Record/Enum/Object/... values and are iterated separately from local ones.
            // These two helpers give `ExternalTypeTemplate.kt` what the old variant's
            // `name` / `namespace` fields used to provide.

            /// The bare name of an external type.
            fn external_type_name(&self, ty: &Type) -> String {
                ty.name().unwrap_or_default().to_owned()
            }

            /// The Kotlin package an external type lives in.
            fn external_type_package(&self, ty: &Type) -> String {
                let module_path = ty.module_path().unwrap_or_default();
                // The namespace is no longer carried on the type; look it up on the CI.
                let namespace = self
                    .ci
                    .namespace_for_module_path(module_path)
                    .unwrap_or(module_path);
                self.external_type_package_name(module_path, namespace)
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

            fn is_name_serializable(&self, name: &str) -> bool {
                !self
                    .config
                    .skip_serializer_for()
                    .contains(&name.to_string())
            }

            // Helper to check if a record can be serialized
            // We only allow records that store primitive types or other records and enums
            fn is_serializable(&self, rec: &Record) -> bool {
                if !self.is_name_serializable(rec.name()) {
                    return false;
                }
                for f in rec.fields() {
                    for inner_ty in f.iter_types() {
                        // uniffi 0.29 removed `Type::External`; externality is now a
                        // query on the ComponentInterface rather than a Type variant.
                        if self.ci.is_external(inner_ty) {
                            return false;
                        }
                        match inner_ty {
                            Type::Object { .. }
                            | Type::CallbackInterface { .. }
                            | Type::Custom { .. } => return false,
                            _ => return true,
                        }
                    }
                }
                true
            }
            // Helper to check if a enum variant can be serialized
            // We only allow records that store primitive types or other records and enums
            fn is_enum_serializable(&self, rec: &Enum) -> bool {
                self.is_name_serializable(rec.name())
            }

            // Helper to check if a enum variant can be serialized
            // We only allow records that store primitive types or other records and enums
            fn is_variant_serializable(&self, rec: &Variant) -> bool {
                if !self.is_name_serializable(rec.name()) {
                    return false;
                }
                for f in rec.fields() {
                    for inner_ty in f.iter_types() {
                        // uniffi 0.29 removed `Type::External`; externality is now a
                        // query on the ComponentInterface rather than a Type variant.
                        if self.ci.is_external(inner_ty) {
                            return false;
                        }
                        match inner_ty {
                            Type::Object { .. }
                            | Type::CallbackInterface { .. }
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

            /// Statements to run inside `UniffiLib.INSTANCE`'s initialiser, where the
            /// loaded library is bound as `lib`.
            pub fn initialization_fns(&self) -> Vec<String> {
                // uniffi 0.31 replaced `iter_types` with an explicit local/external split.
                let local_init_fns = self
                    .ci
                    .iter_local_types()
                    .map(|t| KotlinCodeOracle.find(t))
                    .filter_map(|ct| ct.initialization_fn())
                    .map(|fn_name| format!("{fn_name}(lib)"));

                // Also initialise every external crate we use, so that its callback
                // interface vtables get registered before Rust can call into one of
                // them (upstream #2343). Each namespace has its own lazy `UniffiLib`,
                // so without this an external crate's vtables stay unregistered until
                // that namespace is first touched from Kotlin - and a Rust-side call
                // through an unset vtable aborts the process rather than throwing.
                let external_init_fns = self
                    .ci
                    .iter_external_types()
                    .filter_map(|ty| ty.module_path())
                    .map(|module_path| {
                        let namespace = self
                            .ci
                            .namespace_for_module_path(module_path)
                            .unwrap_or(module_path);
                        let package_name =
                            self.config.external_package_name(module_path, namespace);
                        format!("{package_name}.uniffiEnsureInitialized()")
                    })
                    // Collect into a btree set to de-dup and order
                    .collect::<BTreeSet<_>>();

                local_init_fns.chain(external_init_fns).collect()
            }

            /// Every crate whose Rust is statically linked into this namespace's library.
            ///
            /// `UniffiVtableRegistry` needs this to decide whether a given vtable's init
            /// symbol can be resolved in a given library at all. In library mode
            /// `all_component_interfaces()` is exactly the set of crates the library was
            /// built from - it matches `nm` on the produced cdylib. A UDL-driven build
            /// leaves that list empty, in which case the only crate we can honestly claim
            /// is our own, and the registry simply installs nothing extra.
            pub fn linked_crates(&self) -> Vec<String> {
                self.ci
                    .all_component_interfaces()
                    .iter()
                    .map(|ci| ci.crate_name().to_owned())
                    .chain([self.ci.crate_name().to_owned()])
                    .collect::<BTreeSet<_>>()
                    .into_iter()
                    .collect()
            }

            /// `(vtable holder object, init symbol)` for every callback interface this
            /// namespace declares - a trait interface with a foreign implementation, or a
            /// plain `callback interface`, which are two different definition lists.
            ///
            /// Walks `iter_local_types()` rather than either list so that it stays exactly
            /// in step with the `uniffiCallbackInterface*` objects `Types.kt` renders.
            pub fn callback_vtables(&self) -> Vec<(String, String)> {
                self.ci
                    .iter_local_types()
                    .filter_map(|type_| match type_ {
                        Type::Object { name, .. } => {
                            let obj = self.ci.get_object_definition(name)?;
                            // `ffi_init_callback()` panics for a plain interface.
                            obj.has_callback_interface()
                                .then(|| (name, obj.ffi_init_callback()))
                        }
                        Type::CallbackInterface { name, .. } => {
                            let cbi = self.ci.get_callback_interface_definition(name)?;
                            Some((name, cbi.ffi_init_callback()))
                        }
                        _ => None,
                    })
                    .map(|(name, init)| {
                        (
                            format!("uniffiCallbackInterface{name}"),
                            init.name().to_owned(),
                        )
                    })
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

#[cfg(not(feature = "runtime"))]
kotlin_type_renderer!(CommonTypeRenderer, "generic/common/Types.kt");
#[cfg(feature = "runtime")]
kotlin_type_renderer!(CommonTypeRenderer, "runtime/common/Types.kt");
#[cfg(not(feature = "runtime"))]
kotlin_wrapper!(
    CommonKotlinWrapper,
    CommonTypeRenderer,
    "generic/common/wrapper.kt"
);
#[cfg(feature = "runtime")]
kotlin_wrapper!(
    CommonKotlinWrapper,
    CommonTypeRenderer,
    "runtime/common/wrapper.kt"
);

#[cfg(not(feature = "runtime"))]
kotlin_type_renderer!(AndroidJvmTypeRenderer, "generic/android+jvm/Types.kt");
#[cfg(feature = "runtime")]
kotlin_type_renderer!(AndroidJvmTypeRenderer, "runtime/android+jvm/Types.kt");
#[cfg(not(feature = "runtime"))]
kotlin_wrapper!(
    AndroidJvmKotlinWrapper,
    AndroidJvmTypeRenderer,
    "generic/android+jvm/wrapper.kt"
);
#[cfg(feature = "runtime")]
kotlin_wrapper!(
    AndroidJvmKotlinWrapper,
    AndroidJvmTypeRenderer,
    "runtime/android+jvm/wrapper.kt"
);

#[cfg(not(feature = "runtime"))]
kotlin_type_renderer!(NativeTypeRenderer, "generic/native/Types.kt");
#[cfg(feature = "runtime")]
kotlin_type_renderer!(NativeTypeRenderer, "runtime/native/Types.kt");
#[cfg(not(feature = "runtime"))]
kotlin_wrapper!(
    NativeKotlinWrapper,
    NativeTypeRenderer,
    "generic/native/wrapper.kt"
);
#[cfg(feature = "runtime")]
kotlin_wrapper!(
    NativeKotlinWrapper,
    NativeTypeRenderer,
    "runtime/native/wrapper.kt"
);

#[derive(Template)]
#[cfg_attr(
    not(feature = "runtime"),
    template(syntax = "c", escape = "none", path = "generic/headers/wrapper.h")
)]
#[cfg_attr(
    feature = "runtime",
    template(syntax = "c", escape = "none", path = "runtime/headers/wrapper.h")
)]
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

    #[cfg_attr(feature = "runtime", allow(dead_code))]
    pub fn ffi_definitions_no_builtins(&self) -> impl Iterator<Item = FfiDefinition> + '_ {
        self.ci
            .ffi_definitions()
            .filter(|d| !FFI_BUILTINS.contains(&d.name()))
    }
}

#[derive(Template)]
#[cfg_attr(
    not(feature = "runtime"),
    template(syntax = "c", escape = "none", path = "generic/headers/common.h")
)]
#[cfg_attr(
    feature = "runtime",
    template(syntax = "c", escape = "none", path = "runtime/headers/common.h")
)]
#[allow(dead_code)]
pub struct CommonHeaderKotlinWrapper<'ci> {
    #[allow(dead_code)]
    config: Config,
    ci: &'ci ComponentInterface,
}

impl<'ci> CommonHeaderKotlinWrapper<'ci> {
    pub fn new(config: Config, ci: &'ci ComponentInterface) -> Self {
        Self { config, ci }
    }

    #[cfg_attr(feature = "runtime", allow(dead_code))]
    pub fn ffi_definitions_builtins(&self) -> impl Iterator<Item = FfiDefinition> + '_ {
        self.ci
            .ffi_definitions()
            .filter(|d| FFI_BUILTINS.contains(&d.name()))
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
        header_escape_name_str(&nm.to_lower_camel_case())
    }

    pub fn var_name_raw_noescape(&self, nm: &str) -> String {
        nm.to_lower_camel_case()
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

    fn ffi_type_label_by_value(&self, ffi_type: &FfiType, ci: &ComponentInterface) -> String {
        match ffi_type {
            FfiType::RustBuffer(_) => format!("{}ByValue", self.ffi_type_label(ffi_type, ci)),
            FfiType::Struct(name) => format!("{}UniffiByValue", self.ffi_struct_name(name)),
            FfiType::Callback(name) => self.ffi_callback_name(name).to_string(),
            _ => self.ffi_type_label(ffi_type, ci),
        }
    }

    fn ffi_type_label_for_ffi_function(
        &self,
        ffi_type: &FfiType,
        ci: &ComponentInterface,
    ) -> String {
        match ffi_type {
            FfiType::RustBuffer(_) => format!("{}ByValue", self.ffi_type_label(ffi_type, ci)),
            FfiType::Struct(name) => format!("{}UniffiByValue", self.ffi_struct_name(name)),
            // FfiType::Callback(name) => self.ffi_callback_name(name).to_string(),
            _ => self.ffi_type_label(ffi_type, ci),
        }
    }

    /// FFI type name to use inside structs
    ///
    /// The main requirement here is that all types must have default values or else the struct
    /// won't work in some JNA contexts.
    fn ffi_type_label_for_ffi_struct(&self, ffi_type: &FfiType, ci: &ComponentInterface) -> String {
        match ffi_type {
            // Make callbacks function pointers nullable. This matches the semantics of a C
            // function pointer better and allows for `null` as a default value.
            // NOTE: Type any used here, as native and jvm types differ.
            FfiType::Callback(_name) => "Any?".into(), // format!("{}?", self.ffi_callback_name(name)),
            _ => self.ffi_type_label_by_value(ffi_type, ci),
        }
    }

    /// FFI type name to use inside structs
    ///
    /// The main requirement here is that all types must have default values or else the struct
    /// won't work in some JNA contexts.
    fn ffi_type_label_for_ffi_struct_inner(
        &self,
        ffi_type: &FfiType,
        ci: &ComponentInterface,
    ) -> String {
        match ffi_type {
            // Make callbacks function pointers nullable. This matches the semantics of a C
            // function pointer better and allows for `null` as a default value.
            // NOTE: Type any used here, as native and jvm types differ.
            FfiType::Callback(name) => format!("{}?", self.ffi_callback_name(name)),
            _ => self.ffi_type_label_by_value(ffi_type, ci),
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
            // uniffi 0.30: objects cross the FFI as an opaque u64 handle, not a pointer.
            FfiType::Handle => "0.toLong()".to_owned(),
            FfiType::RustBuffer(_) => "RustBufferHelper.allocValue()".to_owned(),
            FfiType::Callback(_) => "null".to_owned(),
            FfiType::RustCallStatus => "UniffiRustCallStatusHelper.allocValue()".to_owned(),
            _ => unimplemented!("ffi_default_value: {ffi_type:?}"),
        }
    }

    fn ffi_type_label_by_reference(&self, ffi_type: &FfiType, ci: &ComponentInterface) -> String {
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
            | FfiType::Float64
            | FfiType::Handle => format!("{}ByReference", self.ffi_type_label(ffi_type, ci)),
            // JNA structs default to ByReference
            FfiType::RustBuffer(_) | FfiType::Struct(_) => self.ffi_type_label(ffi_type, ci),
            _ => panic!("{ffi_type:?} by reference is not implemented"),
        }
    }

    fn ffi_type_label_by_reference_header(
        &self,
        ffi_type: &FfiType,
        ci: &ComponentInterface,
    ) -> String {
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
            | FfiType::Float64
            | FfiType::Handle => format!("{} *", self.ffi_type_label_header(ffi_type, ci)),
            // JNA structs default to ByReference
            FfiType::RustBuffer(_) | FfiType::Struct(_) => {
                format!("{} *", self.ffi_type_label_header(ffi_type, ci))
            }
            _ => panic!("{ffi_type:?} by reference is not implemented"),
        }
    }

    fn ffi_type_label(&self, ffi_type: &FfiType, ci: &ComponentInterface) -> String {
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
            // uniffi 0.32 attaches external metadata to *every* record/enum, so a bare
            // `Some(..)` no longer means "external" -- compare crate names instead.
            FfiType::RustBuffer(maybe_external) => match maybe_external {
                Some(external_meta) if external_meta.crate_name() != ci.crate_name() => {
                    format!("RustBuffer{}", external_meta.name)
                }
                _ => "RustBuffer".to_string(),
            },
            FfiType::RustCallStatus => "UniffiRustCallStatusByValue".to_string(),
            FfiType::ForeignBytes => "ForeignBytesByValue".to_string(),
            FfiType::Callback(_) => "Any".to_string(),
            FfiType::Struct(name) => self.ffi_struct_name(name),
            FfiType::Reference(inner) | FfiType::MutReference(inner) => {
                self.ffi_type_label_by_reference(inner, ci)
            }
            FfiType::VoidPointer => "Pointer".to_string(),
        }
    }

    fn ffi_type_label_header(&self, ffi_type: &FfiType, ci: &ComponentInterface) -> String {
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
            FfiType::RustBuffer(maybe_external) => match maybe_external {
                Some(external_meta) if external_meta.crate_name() != ci.crate_name() => {
                    format!("RustBuffer{}", external_meta.name)
                }
                _ => "RustBuffer".to_string(),
            },
            FfiType::RustCallStatus => "UniffiRustCallStatus".to_string(),
            FfiType::ForeignBytes => "ForeignBytes".to_string(),
            FfiType::Callback(name) => self.ffi_callback_name_header(name),
            FfiType::Struct(name) => self.ffi_struct_name_header(name),
            FfiType::Reference(inner) | FfiType::MutReference(inner) => {
                self.ffi_type_label_by_reference_header(inner, ci)
            }
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
            Type::Custom { name, builtin, .. } => {
                Box::new(custom::CustomCodeType::new(name, builtin.as_codetype()))
            }
            // `Box<T>` (uniffi 0.32) only matters for scaffolding; bindings use the inner type.
            Type::Box { inner_type } => inner_type.as_codetype(),
            // `HashSet` (uniffi 0.32). Like `Sequence` and `Map`, only the FFI source
            // sets declare a converter; the common API just names `Set<T>`.
            Type::Set { inner_type } => Box::new(compounds::SetCodeType::new(*inner_type)),
        }
    }
}

#[cfg_attr(feature = "runtime", allow(dead_code))]
mod filters {
    // `uniffi_bindgen::backend` was removed in uniffi 0.30; these live in our own
    // vendored copy now.
    pub use super::backend::*;
    use uniffi_bindgen::to_askama_error;
    use uniffi_meta::LiteralMetadata;
    use variant::VariantCodeType;

    use super::*;

    #[askama::filter_fn]
    pub(super) fn type_name(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().type_label(ci))
    }

    // Workaround problem with impl AsCodeType for &Variant (see variant.rs).
    #[askama::filter_fn]
    pub fn variant_type_name(
        v: &Variant,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(VariantCodeType { v: v.clone() }.type_label(ci))
    }

    #[askama::filter_fn]
    pub(super) fn canonical_name(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().canonical_name())
    }

    #[askama::filter_fn]
    pub(super) fn ffi_converter_name(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_codetype().ffi_converter_name())
    }

    #[askama::filter_fn]
    pub(super) fn lower_fn(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!(
            "{}.lower",
            as_ct.as_codetype().ffi_converter_name()
        ))
    }

    /// True for an argument that crosses the FFI as a borrowed `ForeignBytes` rather than
    /// an owned `RustBuffer`: `&[u8]` in Rust, `[ByRef] bytes` in UDL (uniffi 0.32).
    ///
    /// Such an argument cannot be lowered by an expression, because the buffer it points
    /// at has to stay alive and unmoved for the whole call. `macros.kt` therefore wraps
    /// the call in `withForeignBytes { ... }` - see `borrowed_bytes_var_name`.
    #[askama::filter_fn]
    pub(super) fn is_borrowed_bytes(
        arg: &Argument,
        _: &dyn askama::Values,
    ) -> Result<bool, askama::Error> {
        Ok(arg.is_borrowed_bytes())
    }

    /// The name `macros.kt` binds a borrowed-bytes argument's `ForeignBytes` to for the
    /// duration of the call.
    ///
    /// The prefix is what keeps this from needing `var_name`'s backticks: no Kotlin
    /// keyword survives it. Two arguments can only collide here if they already collide
    /// as parameters.
    #[askama::filter_fn]
    pub(super) fn borrowed_bytes_var_name(
        arg: &Argument,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!(
            "uniffiByRefBytes_{}",
            KotlinCodeOracle.var_name_raw(arg.name())
        ))
    }

    /// Per-argument lowering, used for the arguments of an FFI call.
    ///
    /// Never called for a borrowed-bytes argument: `arg_list_lowered` passes the name
    /// bound by `withForeignBytes` instead of lowering anything.
    #[askama::filter_fn]
    pub(super) fn lower_fn_for_arg(
        arg: &Argument,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!("{}.lower", arg.as_codetype().ffi_converter_name()))
    }

    /// Per-argument lifting, used for the arguments of a callback interface vtable method.
    ///
    /// Borrowed bytes are rejected in this direction. `ForeignBytes` implements `Lift` but
    /// not `Lower` in `uniffi_core`, and the vtable shim lowers every argument it passes
    /// out, so a `&[u8]` on a callback or trait-interface method cannot compile on the Rust
    /// side in the first place - upstream has the same hole. Failing here turns what would
    /// be a Kotlin type error into a message naming the argument.
    #[askama::filter_fn]
    pub(super) fn lift_fn_for_arg(
        arg: &Argument,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        if arg.is_borrowed_bytes() {
            return Err(to_askama_error(&format!(
                "borrowed bytes (`&[u8]` in Rust, `[ByRef] bytes` in UDL) cannot be passed \
                 from Rust to a foreign implementation, found one as argument `{}` of a \
                 callback or trait interface method. Take `Vec<u8>` (`bytes`) instead.",
                arg.name()
            )));
        }
        Ok(format!("{}.lift", arg.as_codetype().ffi_converter_name()))
    }

    /// Reject a borrowed-bytes argument on an async callable, rendering nothing otherwise.
    ///
    /// `withForeignBytes` only keeps the caller's bytes alive until the call returns, and an
    /// async call returns a future handle immediately - the Rust future would outlive the
    /// borrow. Today this is unreachable: `rust_future_new` moves the lifted arguments into
    /// an `async move` block and needs them `Send`, which `ForeignBytes` (a raw pointer) is
    /// not, so such a function does not compile in Rust. This is the guard for the day that
    /// changes, because the failure it would otherwise produce is a use-after-free rather
    /// than a compile error.
    #[askama::filter_fn]
    pub(super) fn reject_async_borrowed_bytes(
        callable: impl Callable,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        if callable.is_async() {
            if let Some(arg) = callable
                .arguments()
                .into_iter()
                .find(|arg| arg.is_borrowed_bytes())
            {
                return Err(to_askama_error(&format!(
                    "borrowed bytes (`&[u8]` in Rust, `[ByRef] bytes` in UDL) cannot be \
                     passed to an async function, found one as argument `{}`. The borrow \
                     would end when the call returns its future handle, before Rust is done \
                     reading it. Take `Vec<u8>` (`bytes`) instead.",
                    arg.name()
                )));
            }
        }
        Ok(String::new())
    }

    #[askama::filter_fn]
    pub(super) fn allocation_size_fn(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!(
            "{}.allocationSize",
            as_ct.as_codetype().ffi_converter_name()
        ))
    }

    #[askama::filter_fn]
    pub(super) fn write_fn(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!(
            "{}.write",
            as_ct.as_codetype().ffi_converter_name()
        ))
    }

    #[askama::filter_fn]
    pub(super) fn lift_fn(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!("{}.lift", as_ct.as_codetype().ffi_converter_name()))
    }

    #[askama::filter_fn]
    pub(super) fn read_fn(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(format!("{}.read", as_ct.as_codetype().ffi_converter_name()))
    }

    /// Render a default value.
    ///
    /// Replaces `render_literal`: uniffi 0.30 changed `Field::default_value()` and
    /// `Argument::default_value()` to return `DefaultValue` rather than `Literal`.
    #[askama::filter_fn]
    pub fn render_default<T: AsType>(
        default: &DefaultValue,
        _: &dyn askama::Values,
        as_ct: &T,
        ci: &ComponentInterface,
        config: &Config,
    ) -> Result<String, askama::Error> {
        as_ct
            .as_codetype()
            .default(default, ci, config)
            .map_err(|e| to_askama_error(&e))
    }

    // Get the idiomatic Kotlin rendering of an integer.
    fn int_literal(t: &Option<Type>, base10: String) -> Result<String, askama::Error> {
        if let Some(t) = t {
            match t {
                Type::Int8 | Type::Int16 | Type::Int32 | Type::Int64 => Ok(base10),
                Type::UInt8 | Type::UInt16 | Type::UInt32 | Type::UInt64 => Ok(base10 + "u"),
                _ => Err(to_askama_error(&format!(
                    "Only ints are supported for enum literals: {t:?}"
                ))),
            }
        } else {
            Err(to_askama_error("Enum hasn't defined a repr"))
        }
    }

    // Get the idiomatic Kotlin rendering of an individual enum variant's discriminant
    #[askama::filter_fn]
    pub fn variant_discr_literal(
        e: &Enum,
        _: &dyn askama::Values,
        index: &usize,
    ) -> Result<String, askama::Error> {
        let literal = e.variant_discr(*index).expect("invalid index");
        match literal {
            // Kotlin doesn't convert between signed and unsigned by default
            // so we'll need to make sure we define the type as appropriately
            LiteralMetadata::UInt(v, _, _) => int_literal(e.variant_discr_type(), v.to_string()),
            LiteralMetadata::Int(v, _, _) => int_literal(e.variant_discr_type(), v.to_string()),
            _ => Err(to_askama_error(&format!(
                "Only ints are supported: {literal:?}"
            ))),
        }
    }

    #[askama::filter_fn]
    pub fn ffi_type_name_by_value(
        type_: &FfiType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_by_value(type_, ci))
    }

    #[askama::filter_fn]
    pub fn ffi_type_name_for_ffi_function(
        type_: &FfiType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_for_ffi_function(type_, ci))
    }

    #[askama::filter_fn]
    pub fn ffi_type_name(
        type_: &FfiType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label(type_, ci))
    }

    #[askama::filter_fn]
    pub fn is_callback(type_: &FfiType, _: &dyn askama::Values) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::Callback(_)))
    }

    #[askama::filter_fn]
    pub fn is_rustbuffer(type_: &FfiType, _: &dyn askama::Values) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::RustBuffer(_)))
    }

    #[askama::filter_fn]
    pub fn is_foreignbytes(type_: &FfiType, _: &dyn askama::Values) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::ForeignBytes))
    }

    #[askama::filter_fn]
    pub fn header_ffi_type_name(
        type_: &FfiType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_header(type_, ci))
    }

    #[askama::filter_fn]
    pub fn ffi_type_name_for_ffi_struct(
        type_: &FfiType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_for_ffi_struct(type_, ci))
    }

    #[askama::filter_fn]
    pub fn ffi_type_name_for_ffi_struct_inner(
        type_: &FfiType,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_type_label_for_ffi_struct_inner(type_, ci))
    }

    /// Whether the Kotlin/Native mapping of this FFI type is a raw cinterop pointer
    /// that needs wrapping in `Pointer`.
    ///
    /// uniffi 0.30 turned object references from `RustArcPtr` into an opaque `u64`
    /// `Handle`, which cinterop maps straight to `Long` -- so handles deliberately do
    /// *not* belong here any more.
    #[askama::filter_fn]
    pub fn is_pointer_type(type_: &FfiType, _: &dyn askama::Values) -> Result<bool, askama::Error> {
        Ok(matches!(type_, FfiType::VoidPointer))
    }

    #[askama::filter_fn]
    pub fn ffi_type_name_for_ffi_callback(
        type_: &FfiType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.callback_label_name(type_))
    }

    #[askama::filter_fn]
    pub fn ffi_default_value(
        type_: FfiType,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_default_value(&type_))
    }

    /// Get the idiomatic Kotlin rendering of a class name.
    #[askama::filter_fn]
    pub fn class_name<S: AsRef<str>>(
        nm: S,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.class_name(ci, nm.as_ref()))
    }

    /// Get the idiomatic Kotlin rendering of a function name.
    #[askama::filter_fn]
    pub fn fn_name<S: AsRef<str>>(nm: S, _: &dyn askama::Values) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.fn_name(nm.as_ref()))
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    #[askama::filter_fn]
    pub fn var_name<S: AsRef<str>>(nm: S, _: &dyn askama::Values) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name(nm.as_ref()))
    }

    /// Check if type is Option
    #[askama::filter_fn]
    pub fn is_optional(
        as_ct: &impl AsCodeType,
        _: &dyn askama::Values,
    ) -> Result<bool, askama::Error> {
        Ok(as_ct.as_codetype().is_optional())
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    #[askama::filter_fn]
    pub fn var_name_raw_noescape<S: AsRef<str>>(
        nm: S,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name_raw_noescape(nm.as_ref()))
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    #[askama::filter_fn]
    pub fn var_name_raw<S: AsRef<str>>(
        nm: S,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name_raw(nm.as_ref()))
    }

    /// Get a String representing the name used for an individual enum variant.
    #[askama::filter_fn]
    pub fn variant_name(v: &Variant, _: &dyn askama::Values) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.enum_variant_name(v.name()))
    }

    #[askama::filter_fn]
    pub fn error_variant_name(
        v: &Variant,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        let name = v.name().to_string().to_upper_camel_case();
        Ok(KotlinCodeOracle.convert_error_suffix(&name))
    }

    /// Get the idiomatic Kotlin rendering of an FFI callback function name
    #[askama::filter_fn]
    pub fn ffi_callback_name<S: AsRef<str>>(
        nm: S,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_callback_name(nm.as_ref()))
    }

    /// Get the idiomatic Kotlin rendering of an FFI struct name
    #[askama::filter_fn]
    pub fn ffi_struct_name<S: AsRef<str>>(
        nm: S,
        _: &dyn askama::Values,
    ) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.ffi_struct_name(nm.as_ref()))
    }

    #[askama::filter_fn]
    pub fn object_names(
        obj: &Object,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<(String, String), askama::Error> {
        Ok(KotlinCodeOracle.object_names(ci, obj))
    }

    #[askama::filter_fn]
    pub fn async_poll(
        callable: impl Callable,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_poll(ci);
        Ok(format!(
            "{{ future, callback, continuation -> UniffiLib.INSTANCE.{ffi_func}(future, callback, continuation)!! }}"
        ))
    }

    #[askama::filter_fn]
    pub fn async_complete(
        callable: impl Callable,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_complete(ci);
        let call = format!("UniffiLib.INSTANCE.{ffi_func}(future, continuation)");
        // May need to convert the RustBuffer from our package to the RustBuffer of the
        // external package. `Type::External` was removed in uniffi 0.29, so this is now
        // decided from the lowered FFI type's crate name.
        let call = match callable.return_type() {
            Some(return_type) => match FfiType::from(return_type) {
                FfiType::RustBuffer(Some(external_meta))
                    if external_meta.crate_name() != ci.crate_name() =>
                {
                    // Not `class_name`: the typealias this refers to is declared by
                    // `ExternalTypeTemplate.kt` / `headers/Types.h` as `RustBuffer{name}`,
                    // using the raw name. `UniffiOneUDLTrait` must not become `...UdlTrait`.
                    let suffix = &external_meta.name;
                    format!(
                        "{call}.let {{ RustBuffer{suffix}ByValue(it.capacity, it.len, it.data) }}"
                    )
                }
                _ => call,
            },
            None => call,
        };
        Ok(format!("{{ future, continuation -> {call} }}"))
    }

    #[askama::filter_fn]
    pub fn async_free(
        callable: impl Callable,
        _: &dyn askama::Values,
        ci: &ComponentInterface,
    ) -> Result<String, askama::Error> {
        let ffi_func = callable.ffi_rust_future_free(ci);
        Ok(format!(
            "{{ future -> UniffiLib.INSTANCE.{ffi_func}(future) }}"
        ))
    }

    #[askama::filter_fn]
    pub fn async_cancel(
        callable: impl Callable,
        _: &dyn askama::Values,
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
    #[askama::filter_fn]
    pub fn unquote<S: AsRef<str>>(nm: S, _: &dyn askama::Values) -> Result<String, askama::Error> {
        Ok(nm.as_ref().trim_matches('`').to_string())
    }

    /// Get the idiomatic Kotlin rendering of docstring
    #[askama::filter_fn]
    pub fn docstring<S: AsRef<str>>(
        docstring: S,
        _: &dyn askama::Values,
        spaces: &i32,
    ) -> Result<String, askama::Error> {
        let middle = textwrap::indent(&textwrap::dedent(docstring.as_ref()), " * ");
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

    fn ci_from_udl(udl: &str) -> ComponentInterface {
        ComponentInterface::from_webidl(udl, "probe").unwrap()
    }

    /// The two names `update_component_configs` would normally have filled in.
    fn probe_config() -> Config {
        Config {
            package_name: Some("probe".to_string()),
            cdylib_name: Some("uniffi_probe".to_string()),
            ..Config::default()
        }
    }

    /// A borrowed byte slice is only borrowed until the call returns, and an async call
    /// returns a future handle immediately. Rust rejects such a function first - the lifted
    /// `ForeignBytes` is not `Send` - so this is the guard for the day that changes.
    #[test]
    fn test_async_borrowed_bytes_is_rejected() {
        let ci = ci_from_udl(
            r#"
            namespace probe {
                [Async]
                u64 sum_bytes([ByRef] bytes data);
            };
        "#,
        );
        let err = AndroidJvmKotlinWrapper::new("jvm", probe_config(), &ci)
            .render()
            .expect_err("an async borrowed-bytes argument should not generate");
        let message = err.to_string();
        assert!(message.contains("async"), "{message}");
        assert!(message.contains("data"), "{message}");
    }

    /// Borrowed bytes cannot travel Rust -> Kotlin: `ForeignBytes` implements `Lift` but not
    /// `Lower`, so the vtable shim that would pass one out does not compile in Rust.
    #[test]
    fn test_callback_interface_borrowed_bytes_is_rejected() {
        let ci = ci_from_udl(
            r#"
            namespace probe {};

            callback interface Watcher {
                u64 observe([ByRef] bytes data);
            };
        "#,
        );
        let config = probe_config();
        let err = AndroidJvmTypeRenderer::new("jvm", &config, &ci)
            .render()
            .expect_err("a borrowed-bytes vtable argument should not generate");
        let message = err.to_string();
        assert!(message.contains("callback or trait interface"), "{message}");
        assert!(message.contains("data"), "{message}");
    }

    /// The fields of one record, as they were rendered between the `data class` parens.
    fn rendered_record_fields<'a>(rendered: &'a str, name: &str) -> &'a str {
        let header = format!("data class {name} (");
        let start = rendered
            .find(&header)
            .unwrap_or_else(|| panic!("no `{header}` in:\n{rendered}"))
            + header.len();
        let rest = &rendered[start..];
        &rest[..rest.find(')').expect("unterminated data class")]
    }

    const TWO_RECORDS: &str = r#"
        namespace probe {};

        dictionary Frozen {
            i64 count;
        };

        dictionary Thawed {
            i64 count;
        };
    "#;

    /// `mutable_records` exempts the records it names from `generate_immutable_records`;
    /// every other record in the same binding stays `val`. A name that matches no record is
    /// ignored rather than being an error, the same as upstream.
    #[test]
    fn test_mutable_records_exempts_listed_records() {
        let ci = ci_from_udl(TWO_RECORDS);
        let config = Config {
            generate_immutable_records: Some(true),
            mutable_records: HashSet::from(["Thawed".to_string(), "NoSuchRecord".to_string()]),
            ..probe_config()
        };
        let rendered = CommonKotlinWrapper::new("common", config, &ci)
            .render()
            .unwrap();
        assert!(
            rendered_record_fields(&rendered, "Frozen").contains("val `count`"),
            "{rendered}"
        );
        assert!(
            rendered_record_fields(&rendered, "Thawed").contains("var `count`"),
            "{rendered}"
        );
    }

    /// `mutable_records` only ever exempts - with `generate_immutable_records` turned off
    /// every record is already mutable, and listing one changes nothing.
    #[test]
    fn test_mutable_records_without_immutable_records_is_a_no_op() {
        let ci = ci_from_udl(TWO_RECORDS);
        let config = Config {
            generate_immutable_records: Some(false),
            mutable_records: HashSet::from(["Thawed".to_string()]),
            ..probe_config()
        };
        let rendered = CommonKotlinWrapper::new("common", config, &ci)
            .render()
            .unwrap();
        for name in ["Frozen", "Thawed"] {
            assert!(
                rendered_record_fields(&rendered, name).contains("var `count`"),
                "{rendered}"
            );
        }
    }

    /// Records keep `var` fields unless a binding opts in, matching upstream's default. This
    /// is the baseline `mutable_records` carves an exemption out of.
    #[test]
    fn test_records_are_mutable_by_default() {
        let ci = ci_from_udl(TWO_RECORDS);
        let rendered = CommonKotlinWrapper::new("common", probe_config(), &ci)
            .render()
            .unwrap();
        for name in ["Frozen", "Thawed"] {
            assert!(
                rendered_record_fields(&rendered, name).contains("var `count`"),
                "{rendered}"
            );
        }
    }

    const ONE_FUNCTION: &str = r#"
        namespace probe {
            u64 double(u64 value);
        };
    "#;

    /// The API checksum check is generated by default: one comparison per exported
    /// function, run when the library is loaded.
    #[test]
    fn test_checksums_are_checked_by_default() {
        let ci = ci_from_udl(ONE_FUNCTION);
        let rendered = AndroidJvmKotlinWrapper::new("jvm", probe_config(), &ci)
            .render()
            .unwrap();
        assert!(rendered.contains("uniffiCheckApiChecksums"), "{rendered}");

        assert!(
            rendered.contains(&format!(
                "if (lib.{}()",
                ci.iter_checksums().next().expect("no checksum function").0
            )),
            "{rendered}"
        );
    }

    /// `omit_checksums` drops both the call and the function it would call. The contract
    /// version check is a separate guarantee and stays.
    #[test]
    fn test_omit_checksums_drops_the_check() {
        let ci = ci_from_udl(ONE_FUNCTION);
        let config = Config {
            omit_checksums: true,
            ..probe_config()
        };
        let rendered = AndroidJvmKotlinWrapper::new("jvm", config, &ci)
            .render()
            .unwrap();
        assert!(!rendered.contains("uniffiCheckApiChecksums"), "{rendered}");
        assert!(
            rendered.contains("uniffiCheckContractApiVersion(lib)"),
            "{rendered}"
        );
    }

    /// The checksum functions stay declared on `UniffiLib` either way - they are part of
    /// the library's FFI, and the native source set reads the same list out of the
    /// generated header.
    #[test]
    fn test_omit_checksums_keeps_the_ffi_declarations() {
        let ci = ci_from_udl(ONE_FUNCTION);
        let checksum_fn = ci.iter_checksums().next().expect("no checksum function").0;
        let config = Config {
            omit_checksums: true,
            ..probe_config()
        };
        let rendered = AndroidJvmKotlinWrapper::new("jvm", config, &ci)
            .render()
            .unwrap();
        assert!(
            rendered.contains(&format!("fun {checksum_fn}(")),
            "{rendered}"
        );
    }

    /// The name a borrow is bound to has to be a bare identifier: `var_name` backticks Kotlin
    /// keywords, and `uniffiByRefBytes_` + `` `object` `` would not parse.
    #[test]
    fn test_borrowed_bytes_var_name_is_not_backticked() {
        let ci = ci_from_udl(
            r#"
            namespace probe {
                u64 sum_bytes([ByRef] bytes object);
            };
        "#,
        );
        let rendered = AndroidJvmKotlinWrapper::new("jvm", probe_config(), &ci)
            .render()
            .unwrap();
        assert!(
            rendered.contains("withForeignBytes(`object`) { uniffiByRefBytes_object ->"),
            "{rendered}"
        );
    }
}
