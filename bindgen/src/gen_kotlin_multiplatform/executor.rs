use uniffi_bindgen::ComponentInterface;

use super::CodeType;

#[derive(Debug)]
pub struct ForeignExecutorCodeType;

impl CodeType for ForeignExecutorCodeType {
    fn type_label(&self, _ci: &ComponentInterface) -> String {
        // Kotlin uses a CoroutineScope for ForeignExecutor
        "CoroutineScope".into()
    }

    fn canonical_name(&self) -> String {
        "ForeignExecutor".into()
    }

    fn initialization_fn(&self) -> Option<String> {
        Some("FfiConverterForeignExecutor.register".into())
    }
}
