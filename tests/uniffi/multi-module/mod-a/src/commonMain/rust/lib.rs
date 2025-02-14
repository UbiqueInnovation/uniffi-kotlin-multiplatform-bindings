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

uniffi::setup_scaffolding!("module_a");
