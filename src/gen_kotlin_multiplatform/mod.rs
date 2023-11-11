use std::borrow::Borrow;
use std::collections::HashMap;

use anyhow::{Context, Result};
use askama::Template;
use heck::{ToLowerCamelCase, ToShoutySnakeCase, ToUpperCamelCase};
use uniffi_bindgen::backend::{CodeType};
use uniffi_bindgen::interface::{Callable, AsType, CallbackInterface, Enum, FfiType, Object, ObjectImpl, Record, Type};
use uniffi_bindgen::ComponentInterface;

use crate::{Config, KotlinMultiplatformBindings};

mod callback_interface;
mod compounds;
mod custom;
mod enum_;
mod error;
mod executor;
mod external;
mod miscellany;
mod object;
mod primitives;
mod record;

macro_rules! kotlin_template {
    ($KotlinTemplate:ident, $source_file:literal) => {
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        pub struct $KotlinTemplate<'ci> {
            config: Config,
            ci: &'ci ComponentInterface,
        }

        impl<'ci> $KotlinTemplate<'ci> {
            pub fn new(config: Config, ci: &'ci ComponentInterface) -> Self {
                Self { config, ci }
            }

            pub fn initialization_fns(&self) -> Vec<String> {
                self.ci
                    .iter_types()
                    .map(|t| KotlinCodeOracle.find(t))
                    .filter_map(|ct| ct.initialization_fn())
                    .collect()
            }
        }
    };
}

// Dummy templates are copied as is. They are useful to reuse the existing logic
macro_rules! kotlin_dummy_template {
    ($KotlinTemplate:ident, $source_file:literal) => {
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        pub struct $KotlinTemplate {}

        impl $KotlinTemplate {
            pub fn new() -> Self {
                Self { }
            }
        }
    };
}

macro_rules! kotlin_callback_interface_template {
    ($KotlinTemplate:ident, $source_file:literal) => {
        #[derive(Template)]
        #[template(syntax = "kt", escape = "none", path = $source_file)]
        pub struct $KotlinTemplate<'cbi> {
            cbi: &'cbi CallbackInterface,
            type_name: String,
            foreign_callback_name: String,
            ffi_converter_name: String,
        }

        impl<'cbi> $KotlinTemplate<'cbi> {
            pub fn new(
                cbi: &'cbi CallbackInterface,
                type_name: String,
                foreign_callback_name: String,
                ffi_converter_name: String,
            ) -> Self {
                Self {
                    cbi,
                    type_name,
                    foreign_callback_name,
                    ffi_converter_name,
                }
            }
        }
    };
}

#[derive(Template)]
#[template(
syntax = "kt",
escape = "none",
path = "common/CustomTypeTemplate.kt.j2"
)]
pub struct CustomTypeTemplateCommon {
    config: Config,
    name: String,
    ffi_converter_name: String,
    builtin: Box<Type>,
}

impl CustomTypeTemplateCommon {
    pub fn new(
        config: Config,
        name: String,
        ffi_converter_name: String,
        builtin: Box<Type>,
    ) -> Self {
        Self {
            config,
            ffi_converter_name,
            name,
            builtin,
        }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/EnumTemplate.kt.j2")]
pub struct EnumTemplateCommon<'e> {
    e: &'e Enum,
    type_name: String,
    contains_object_references: bool,
}

impl<'e> EnumTemplateCommon<'e> {
    pub fn new(e: &'e Enum, type_name: String, contains_object_references: bool) -> Self {
        Self {
            e,
            type_name,
            contains_object_references,
        }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/ErrorTemplate.kt.j2")]
pub struct ErrorTemplateCommon<'e> {
    e: &'e Enum,
    type_name: String,
    contains_object_references: bool,
}

impl<'e> ErrorTemplateCommon<'e> {
    pub fn new(e: &'e Enum, type_name: String, contains_object_references: bool) -> Self {
        Self {
            e,
            type_name,
            contains_object_references,
        }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/MapTemplate.kt.j2")]
pub struct MapTemplateCommon {
    key_type: Box<Type>,
    value_type: Box<Type>,
    ffi_converter_name: String,
}

impl MapTemplateCommon {
    pub fn new(key_type: Box<Type>, value_type: Box<Type>, ffi_converter_name: String) -> Self {
        Self {
            key_type,
            value_type,
            ffi_converter_name,
        }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/ObjectTemplate.kt.j2")]
pub struct ObjectTemplateCommon<'e> {
    obj: &'e Object,
    type_name: String,
}

impl<'e> ObjectTemplateCommon<'e> {
    pub fn new(obj: &'e Object, type_name: String) -> Self {
        Self { obj, type_name }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/OptionalTemplate.kt.j2")]
pub struct OptionalTemplateCommon {
    ffi_converter_name: String,
    inner_type_name: String,
    inner_type: Box<Type>,
}

impl OptionalTemplateCommon {
    pub fn new(ffi_converter_name: String, inner_type_name: String, inner_type: Box<Type>) -> Self {
        Self {
            ffi_converter_name,
            inner_type_name,
            inner_type,
        }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/RecordTemplate.kt.j2")]
pub struct RecordTemplateCommon<'rec> {
    rec: &'rec Record,
    type_name: String,
    contains_object_references: bool,
}

impl<'rec> RecordTemplateCommon<'rec> {
    pub fn new(rec: &'rec Record, type_name: String, contains_object_references: bool) -> Self {
        Self {
            rec,
            type_name,
            contains_object_references,
        }
    }
}

#[derive(Template)]
#[template(syntax = "kt", escape = "none", path = "common/SequenceTemplate.kt.j2")]
pub struct SequenceTemplateCommon {
    ffi_converter_name: String,
    inner_type_name: String,
    inner_type: Box<Type>,
}

impl SequenceTemplateCommon {
    pub fn new(ffi_converter_name: String, inner_type_name: String, inner_type: Box<Type>) -> Self {
        Self {
            ffi_converter_name,
            inner_type_name,
            inner_type,
        }
    }
}

#[derive(Template)]
#[template(
syntax = "c",
escape = "none",
path = "headers/BridgingHeaderTemplate.h.j2"
)]
pub struct BridgingHeader<'ci> {
    _config: Config,
    ci: &'ci ComponentInterface,
}

impl<'ci> BridgingHeader<'ci> {
    pub fn new(config: Config, ci: &'ci ComponentInterface) -> Self {
        Self {
            _config: config,
            ci,
        }
    }
}

macro_rules! render_kotlin_template {
    ($template:ident, $file_name:literal, $map:ident) => {
        let file_name = $file_name.to_string();
        let context = format!("failed to render kotlin binding {}", stringify!($T));
        $map.insert(file_name, $template.render().context(context).unwrap());
    };

    ($template:ident, $file_name:ident, $map:ident) => {
        let file_name = $file_name;
        let context = format!("failed to render kotlin binding {}", stringify!($T));
        $map.insert(file_name, $template.render().context(context).unwrap());
    };
}

kotlin_dummy_template!(
    FfiConverterForeignExecutorTemplateCommon,
    "common/FfiConverterForeignExecutor.kt.j2"
);
kotlin_dummy_template!(
    UniFfiForeignExecutorCallbackTemplateCommon,
    "common/UniFfiForeignExecutorCallback.kt.j2"
);
// kotlin_template!(AsyncTypesTemplateCommon, "common/AsyncTypesTemplate.kt.j2");
kotlin_template!(TopLevelFunctionsTemplateCommon,"common/TopLevelFunctionsTemplate.kt.j2");
kotlin_template!(UniFFILibTemplateCommon, "common/UniFFILibTemplate.kt.j2");
kotlin_callback_interface_template!(
    CallbackInterfaceTemplateCommon,
    "common/CallbackInterfaceTemplate.kt.j2"
);

kotlin_dummy_template!(
    UniFfiForeignExecutorCallbackTemplateJvm,
    "jvm/UniFfiForeignExecutorCallback.kt.j2"
);
// kotlin_template!(AsyncTypesTemplateJvm, "jvm/AsyncTypesTemplate.kt.j2");
kotlin_template!(RustBufferTemplateJvm, "jvm/RustBufferTemplate.kt.j2");
kotlin_template!(UniFFILibTemplateJvm, "jvm/UniFFILibTemplate.kt.j2");
kotlin_callback_interface_template!(
    CallbackInterfaceTemplateJvm,
    "jvm/CallbackInterfaceTemplate.kt.j2"
);

kotlin_dummy_template!(
    UniFfiForeignExecutorCallbackTemplateNative,
    "native/UniFfiForeignExecutorCallback.kt.j2"
);
// kotlin_template!(AsyncTypesTemplateNative, "native/AsyncTypesTemplate.kt.j2");
kotlin_template!(ForeignBytesTemplateNative, "native/ForeignBytesTemplate.kt.j2");
kotlin_template!(RustBufferTemplateNative, "native/RustBufferTemplate.kt.j2");
kotlin_template!(RustCallStatusTemplateNative, "native/RustCallStatusTemplate.kt.j2");
kotlin_template!(UniFFILibTemplateNative, "native/UniFFILibTemplate.kt.j2");
kotlin_callback_interface_template!(
    CallbackInterfaceTemplateNative,
    "native/CallbackInterfaceTemplate.kt.j2"
);

pub fn generate_bindings(
    config: &Config,
    ci: &ComponentInterface,
) -> Result<KotlinMultiplatformBindings> {
    let mut common_wrapper: HashMap<String, String> = HashMap::new();
    // let async_types_template_common = AsyncTypesTemplateCommon::new(
    //     config.clone(),
    //     ci,
    // );
    // render_kotlin_template!(async_types_template_common, "AsyncTypes.kt", common_wrapper);
    let top_level_functions_template_common =
        TopLevelFunctionsTemplateCommon::new(config.clone(), ci);
    render_kotlin_template!(
        top_level_functions_template_common,
        "TopLevelFunctions.kt",
        common_wrapper
    );
    let uniffilib_template_common = UniFFILibTemplateCommon::new(config.clone(), ci);
    render_kotlin_template!(uniffilib_template_common, "UniFFILib.kt", common_wrapper);
    for type_ in ci.iter_types() {
        let canonical_type_name = filters::canonical_name(type_).unwrap();
        let ffi_converter_name = filters::ffi_converter_name(type_).unwrap();
        let contains_object_references = ci.item_contains_object_references(type_);
        match type_ {
            Type::CallbackInterface { name, .. } => {
                let cbi: &CallbackInterface = ci.get_callback_interface_definition(name).unwrap();
                let type_name = filters::type_name(cbi).unwrap();
                let template = CallbackInterfaceTemplateCommon::new(
                    cbi,
                    type_name.clone(),
                    format!("ForeignCallback{}", canonical_type_name),
                    ffi_converter_name,
                );
                let file_name = format!("{}.kt", type_name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }

            Type::Custom { name, builtin, .. } => {
                let template = CustomTypeTemplateCommon::new(
                    config.clone(),
                    name.clone(),
                    ffi_converter_name,
                    builtin.clone(),
                );
                let file_name = format!("{}.kt", name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }

            Type::Enum { name, .. } => {
                if !ci.is_name_used_as_error(name) {
                    let e: &Enum = ci.get_enum_definition(name).unwrap();
                    let type_name = filters::type_name(type_).unwrap();
                    let template =
                        EnumTemplateCommon::new(e, type_name.clone(), contains_object_references);
                    let file_name = format!("{}.kt", type_name);
                    render_kotlin_template!(template, file_name, common_wrapper);
                } else {
                    let e: &Enum = ci.get_enum_definition(name).unwrap();
                    let type_name = filters::error_type_name(type_).unwrap();
                    let template =
                        ErrorTemplateCommon::new(e, type_name.clone(), contains_object_references);
                    let file_name = format!("{}.kt", type_name);
                    render_kotlin_template!(template, file_name, common_wrapper);
                }
            }

            Type::External { name, .. } => {
                // TODO this need specific imports in some classes.
            }

            Type::ForeignExecutor => {
                // The presence of the ForeignExecutor type indicates that we need to add the async infrastructure
                let executor_template_common = FfiConverterForeignExecutorTemplateCommon::new();
                render_kotlin_template!(executor_template_common, "FfiConverterForeignExecutor.kt", common_wrapper);
                let callback_template_common = UniFfiForeignExecutorCallbackTemplateCommon::new();
                render_kotlin_template!(callback_template_common, "UniFfiForeignExecutorCallback.kt", common_wrapper);
            }

            Type::Map { key_type, value_type } => {
                let template = MapTemplateCommon::new(
                    key_type.clone(),
                    value_type.clone(),
                    ffi_converter_name.clone(),
                );
                let file_name = format!("{}.kt", ffi_converter_name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }

            Type::Object { name, .. } => {
                let obj: &Object = ci.get_object_definition(name).unwrap();
                let type_name = filters::type_name(type_).unwrap();
                let template = ObjectTemplateCommon::new(
                    obj, type_name.clone(),
                );
                let file_name = format!("{}.kt", type_name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }

            Type::Optional { inner_type } => {
                let inner_type_name = filters::type_name(inner_type).unwrap();
                let template = OptionalTemplateCommon::new(
                    ffi_converter_name.clone(), inner_type_name, inner_type.clone(),
                );
                let file_name = format!("{}.kt", ffi_converter_name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }

            Type::Record { name, .. } => {
                let rec: &Record = ci.get_record_definition(name).unwrap();
                let type_name = filters::type_name(type_).unwrap();
                let template = RecordTemplateCommon::new(
                    rec, type_name.clone(), contains_object_references,
                );
                let file_name = format!("{}.kt", type_name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }

            Type::Sequence { inner_type } => {
                let inner_type_name = filters::type_name(inner_type).unwrap();
                let template = SequenceTemplateCommon::new(
                    ffi_converter_name.clone(), inner_type_name, inner_type.clone(),
                );
                let file_name = format!("{}.kt", ffi_converter_name);
                render_kotlin_template!(template, file_name, common_wrapper);
            }
            _ => {}
        }
    }

    let mut jvm_wrapper: HashMap<String, String> = HashMap::new();
    // let async_types_template_jvm = AsyncTypesTemplateJvm::new(config.clone(), ci);
    // render_kotlin_template!(async_types_template_jvm, "AsyncTypes.kt", jvm_wrapper);
    let rust_buffer_template_jvm = RustBufferTemplateJvm::new(config.clone(), ci);
    render_kotlin_template!(rust_buffer_template_jvm, "RustBuffer.kt", jvm_wrapper);
    let uniffilib_template_jvm = UniFFILibTemplateJvm::new(config.clone(), ci);
    render_kotlin_template!(uniffilib_template_jvm, "UniFFILib.kt", jvm_wrapper);
    for type_ in ci.iter_types() {
        let canonical_type_name = filters::canonical_name(type_).unwrap();
        let ffi_converter_name = filters::ffi_converter_name(type_).unwrap();
        match type_ {
            Type::CallbackInterface { name, .. } => {
                let cbi: &CallbackInterface = ci.get_callback_interface_definition(name).unwrap();
                let type_name = filters::type_name(cbi).unwrap();
                let template = CallbackInterfaceTemplateJvm::new(
                    cbi,
                    type_name.clone(),
                    format!("ForeignCallback{}", canonical_type_name),
                    ffi_converter_name,
                );
                let file_name = format!("{}.kt", type_name);
                render_kotlin_template!(template, file_name, jvm_wrapper);
            }

            Type::ForeignExecutor => {
                // The presence of the ForeignExecutor type indicates that we need to add the async infrastructure
                let template = UniFfiForeignExecutorCallbackTemplateJvm::new();
                render_kotlin_template!(template, "UniFfiForeignExecutorCallback.kt", jvm_wrapper);
            }

            _ => {}
        }
    }

    let mut native_wrapper: HashMap<String, String> = HashMap::new();
    // let async_types_template_native = AsyncTypesTemplateNative::new(config.clone(), ci);
    // render_kotlin_template!(async_types_template_native, "AsyncTypes.kt", native_wrapper);
    let foreign_bytes_template_native = ForeignBytesTemplateNative::new(config.clone(), ci);
    render_kotlin_template!(foreign_bytes_template_native, "ForeignBytes.kt", native_wrapper);
    let rust_buffer_template_native = RustBufferTemplateNative::new(config.clone(), ci);
    render_kotlin_template!(rust_buffer_template_native, "RustBuffer.kt", native_wrapper);
    let rust_call_status_template_native = RustCallStatusTemplateNative::new(config.clone(), ci);
    render_kotlin_template!(rust_call_status_template_native, "RustCallStatus.kt", native_wrapper);
    let uniffilib_template_native = UniFFILibTemplateNative::new(config.clone(), ci);
    render_kotlin_template!(uniffilib_template_native, "UniFFILib.kt", native_wrapper);
    for type_ in ci.iter_types() {
        let canonical_type_name = filters::canonical_name(type_).unwrap();
        let ffi_converter_name = filters::ffi_converter_name(type_).unwrap();
        match type_ {
            Type::CallbackInterface { name, .. } => {
                let cbi: &CallbackInterface = ci.get_callback_interface_definition(name).unwrap();
                let type_name = filters::type_name(cbi).unwrap();
                let template = CallbackInterfaceTemplateNative::new(
                    cbi,
                    type_name.clone(),
                    format!("ForeignCallback{}", canonical_type_name),
                    ffi_converter_name,
                );
                let file_name = format!("{}.kt", type_name);
                render_kotlin_template!(template, file_name, native_wrapper);
            }

            Type::ForeignExecutor => {
                // The presence of the ForeignExecutor type indicates that we need to add the async infrastructure
                let template = UniFfiForeignExecutorCallbackTemplateNative::new();
                render_kotlin_template!(template, "UniFfiForeignExecutorCallback.kt", native_wrapper);
            }

            _ => {}
        }
    }

    let header = BridgingHeader::new(config.clone(), ci)
        .render()
        .context("failed to render Kotlin/Native bridging header")?;

    Ok(KotlinMultiplatformBindings {
        common: common_wrapper,
        jvm: jvm_wrapper,
        native: native_wrapper,
        header,
    })
}

#[derive(Clone)]
pub struct KotlinCodeOracle;

impl KotlinCodeOracle {
    fn find(&self, type_: &Type) -> Box<dyn CodeType> {
        type_.clone().as_type().as_code_type()
    }

    fn find_as_error(&self, type_: &Type) -> Box<dyn CodeType> {
        match type_ {
            Type::Enum { name, .. } => Box::new(error::ErrorCodeType::new(name.clone())),
            // XXX - not sure how we are supposed to return askama::Error?
            _ => panic!("unsupported type for error: {type_:?}"),
        }
    }

    /// Get the idiomatic Kotlin rendering of a class name (for enums, records, errors, etc).
    fn class_name(&self, nm: &str) -> String {
        nm.to_string().to_upper_camel_case()
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

    /// Get the idiomatic Kotlin rendering of an exception name
    ///
    /// This replaces "Error" at the end of the name with "Exception".  Rust code typically uses
    /// "Error" for any type of error but in the Java world, "Error" means a non-recoverable error
    /// and is distinguished from an "Exception".
    fn error_name(&self, nm: &str) -> String {
        // errors are a class in kotlin.
        let name = self.class_name(nm);
        match name.strip_suffix("Error") {
            None => name,
            Some(stripped) => format!("{stripped}Exception"),
        }
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

            // FfiType::ForeignExecutorHandle => "ULong".to_string(),
            // FfiType::ForeignExecutorCallback => "UniFfiForeignExecutorCallback".to_string(),
            // FfiType::FutureCallback { return_type } => {
            //     format!("UniFfiFutureCallback{}", return_type.canonical_name())
            // }
            // FfiType::FutureCallbackData => "Pointer".to_string(),

            FfiType::ForeignExecutorHandle => "Pointer".to_string(),
            FfiType::ForeignExecutorCallback => "UniFfiForeignExecutorCallback".to_string(),
            FfiType::RustFutureHandle => "Pointer".to_string(),
            FfiType::RustFutureContinuationCallback => {
                "Pointer".to_string()
            }
            FfiType::RustFutureContinuationData => "Pointer".to_string(),
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

            // FfiType::ForeignExecutorHandle => "size_t".into(),
            // FfiType::ForeignExecutorCallback => "UniFfiForeignExecutorCallback _Nonnull".into(),
            // FfiType::FutureCallback { return_type } => format!(
            //     "UniFfiFutureCallback{} _Nonnull",
            //     return_type.canonical_name()
            // ),
            // FfiType::FutureCallbackData => "void* _Nonnull".into(),

            FfiType::ForeignExecutorHandle => "void*".into(),
            FfiType::ForeignExecutorCallback => "UniFfiForeignExecutorCallback _Nonnull".into(),
            FfiType::RustFutureHandle => "void*".into(),
            FfiType::RustFutureContinuationCallback => { "void*".into() }
            FfiType::RustFutureContinuationData => "void* _Nonnull".to_string(),
        }
    }

    /// Get the name of the interface and class name for an object.
    ///
    /// This depends on the `ObjectImpl`:
    ///
    /// For struct impls, the class name is the object name and the interface name is derived from that.
    /// For trait impls, the interface name is the object name, and the class name is derived from that.
    ///
    /// This split is needed because of the `FfiConverter` interface.  For struct impls, `lower`
    /// can only lower the concrete class.  For trait impls, `lower` can lower anything that
    /// implement the interface.
    fn object_names(&self, obj: &Object) -> (String, String) {
        let class_name = self.class_name(obj.name());
        match obj.imp() {
            ObjectImpl::Struct => (format!("{class_name}Interface"), class_name),
            ObjectImpl::Trait => {
                let interface_name = format!("{class_name}Impl");
                (class_name, interface_name)
            }
        }
    }
}

pub trait AsCodeType {
    fn as_code_type(&self) -> Box<dyn CodeType>;
}

impl<T: AsType> AsCodeType for T {
    fn as_code_type(&self) -> Box<dyn CodeType> {
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
            Type::Object { name, imp, .. } => Box::new(object::ObjectCodeType::new(name)),
            Type::Record { name, .. } => Box::new(record::RecordCodeType::new(name)),
            Type::CallbackInterface { name, .. } => {
                Box::new(callback_interface::CallbackInterfaceCodeType::new(name))
            }
            Type::ForeignExecutor => Box::new(executor::ForeignExecutorCodeType),
            Type::Optional { inner_type } => Box::new(compounds::OptionalCodeType::new(*inner_type)),
            Type::Sequence { inner_type } => Box::new(compounds::SequenceCodeType::new(*inner_type)),
            Type::Map { key_type, value_type } => Box::new(compounds::MapCodeType::new(*key_type, *value_type)),
            Type::External { name, .. } => Box::new(external::ExternalCodeType::new(name)),
            Type::Custom { name, .. } => Box::new(custom::CustomCodeType::new(name)),
        }
    }
}

pub mod filters {
    use super::*;
    pub use uniffi_bindgen::backend::filters::*;
    use uniffi_bindgen::backend::Literal;
    use uniffi_bindgen::interface::ResultType;

    pub fn type_name(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(as_ct.as_code_type().type_label())
    }

    pub fn canonical_name(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(as_ct.as_code_type().canonical_name())
    }

    pub fn ffi_converter_name(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(as_ct.as_code_type().ffi_converter_name())
    }

    /// Some of the above filters have different versions to help when the type
    /// is used as an error.
    pub fn error_type_name(as_type: &impl AsType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle
            .find_as_error(&as_type.as_type())
            .type_label())
    }

    pub fn error_canonical_name(as_type: &impl AsType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle
            .find_as_error(&as_type.as_type())
            .canonical_name())
    }

    pub fn error_ffi_converter_name(as_type: &impl AsType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle
            .find_as_error(&as_type.as_type())
            .ffi_converter_name())
    }

    pub fn lower_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.lower", as_ct.as_code_type().ffi_converter_name()))
    }

    pub fn allocation_size_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.allocationSize", as_ct.as_code_type().ffi_converter_name()))
    }

    pub fn write_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.write", as_ct.as_code_type().ffi_converter_name()))
    }

    pub fn lift_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.lift", as_ct.as_code_type().ffi_converter_name()))
    }

    pub fn read_fn(as_ct: &impl AsCodeType) -> Result<String, askama::Error> {
        Ok(format!("{}.read", as_ct.as_code_type().ffi_converter_name()))
    }

    pub fn error_handler(result_type: &ResultType) -> Result<String, askama::Error> {
        match &result_type.throws_type {
            Some(error_type) => type_name(error_type),
            None => Ok("NullCallStatusErrorHandler".into()),
        }
    }

    pub fn future_callback_handler(result_type: &ResultType) -> Result<String, askama::Error> {
        let return_component = match &result_type.return_type {
            Some(return_type) => return_type.as_code_type().canonical_name(),
            None => "Void".into(),
        };
        let throws_component = match &result_type.throws_type {
            Some(throws_type) => format!("_{}", throws_type.as_code_type().canonical_name()),
            None => "".into(),
        };
        Ok(format!(
            "UniFfiFutureCallbackHandler{return_component}{throws_component}"
        ))
    }

    pub fn future_continuation_type(result_type: &ResultType) -> Result<String, askama::Error> {
        let return_type_name = match &result_type.return_type {
            Some(t) => type_name(t)?,
            None => "Unit".into(),
        };
        Ok(format!("Continuation<{return_type_name}>"))
    }

    pub fn render_literal(
        literal: &Literal,
        as_ct: &impl AsCodeType,
    ) -> Result<String, askama::Error> {
        Ok(as_ct.as_code_type().literal(literal))
    }

    /// Get the Kotlin syntax for representing a given low-level `FfiType`.
    pub fn ffi_type_name(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle::ffi_type_label(type_))
    }

    pub fn ffi_header_type_name(type_: &FfiType) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle::ffi_header_type_label(type_))
    }

    /// Get the idiomatic Kotlin rendering of a class name (for enums, records, errors, etc).
    pub fn class_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.class_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of a function name.
    pub fn fn_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.fn_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of a variable name.
    pub fn var_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.var_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of an individual enum variant.
    pub fn enum_variant(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.enum_variant_name(nm))
    }

    /// Get the idiomatic Kotlin rendering of an exception name, replacing
    /// `Error` with `Exception`.
    pub fn exception_name(nm: &str) -> Result<String, askama::Error> {
        Ok(KotlinCodeOracle.error_name(nm))
    }

    /// Remove the "`" chars we put around function/variable names
    ///
    /// These are used to avoid name clashes with kotlin identifiers, but sometimes you want to
    /// render the name unquoted.  One example is the message property for errors where we want to
    /// display the name for the user.
    pub fn unquote(nm: &str) -> Result<String, askama::Error> {
        Ok(nm.trim_matches('`').to_string())
    }

    /// Transforms Pointer types to nullable pointer to work around cinterop not respecting
    /// nullability annotations in header files. This leads to type mismatches.
    pub fn nullify_pointer(type_name: &str) -> Result<String, askama::Error> {
        Ok(match type_name {
            "Pointer" => "Pointer?".to_string(),
            _ => type_name.to_string(),
        })
    }
}
