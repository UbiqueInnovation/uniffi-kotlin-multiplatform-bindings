use uniffi_kmm_fixture_multi_rust_common::{TestCallback, TestObject, TestRecord};

#[uniffi::export]
pub fn hello() -> String {
    "Hello".to_string()
}

#[uniffi::export]
pub fn create_vec() -> Vec<i64> {
    vec![1, 3, 3, 7]
}

#[uniffi::export]
pub fn test_get_int(rec: &TestRecord) -> i64 {
    rec.int
}

#[uniffi::export]
pub fn greet(obj: &TestObject) -> String {
    format!("Hello {name}!", name = obj.get_name())
}

#[uniffi::export]
fn different_crate_call_callback(c: &dyn TestCallback) -> String {
    c.callback()
}

/// A trait declared *here*, so `module_a`'s vtable init symbol exists only in this
/// library. Registering it exercises `UniffiVtableRegistry`'s one silent path: the
/// other modules' libraries do not link `module_a`, so they must be skipped rather
/// than failing to resolve the symbol.
#[uniffi::export(with_foreign)]
pub trait ModACallback: Send + Sync {
    fn callback(&self) -> String;
}

#[uniffi::export]
fn mod_a_call_own_callback(c: &dyn ModACallback) -> String {
    c.callback()
}

uniffi::setup_scaffolding!("module_a");
