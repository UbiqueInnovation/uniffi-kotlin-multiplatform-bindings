use uniffi_kmm_fixture_multi_rust_common::{TestCallback, TestObject, TestRecord};

#[uniffi::export]
pub fn create_test_record(int: i64, str: String, vec: Vec<i64>) -> TestRecord {
    TestRecord { int, str, vec }
}

#[uniffi::export]
pub fn create_test_object(name: String) -> TestObject {
    TestObject::new(name)
}

/// The same `rust_common` trait `module_a` takes, reached through a second library.
/// Each module statically links its own copy of `rust_common`, so this call goes
/// through a different vtable cell than `module_a`'s.
#[uniffi::export]
fn mod_b_call_callback(c: &dyn TestCallback) -> String {
    c.callback()
}

uniffi::setup_scaffolding!("module_b");
