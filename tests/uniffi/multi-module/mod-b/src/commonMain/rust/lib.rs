use uniffi_kmm_fixture_multi_rust_common::{TestObject, TestRecord};

#[uniffi::export]
pub fn create_test_record(int: i64, str: String, vec: Vec<i64>) -> TestRecord {
    TestRecord { int, str, vec }
}

#[uniffi::export]
pub fn create_test_object(name: String) -> TestObject {
    TestObject::new(name)
}

uniffi::setup_scaffolding!("module_b");
