use uniffi_bindgen::backend::{CodeType};

#[derive(Debug)]
pub struct ForeignExecutorCodeType;

impl CodeType for ForeignExecutorCodeType {
    fn type_label(&self) -> String {
        // Kotlin uses a CoroutineScope for ForeignExecutor
        "CoroutineScope".into()
    }

    fn canonical_name(&self) -> String {
        "ForeignExecutor".into()
    }

    fn initialization_fn(&self) -> Option<String> {
        // FfiConverterForeignExecutor is a Kotlin object generated from a template
        // register calls lib.uniffi_foreign_executor_callback_set(UniFfiForeignExecutorCallback) where
        // object UniFfiForeignExecutorCallback : com.sun.jna.Callback
        // but that will not work in Kotlin/Native since we do not have access to JNA
        Some("FfiConverterForeignExecutor.register".into())
    }
}
