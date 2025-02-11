use uniffi_kmm_fixture_multi_rust_common::TestRecord;

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

uniffi::setup_scaffolding!("mod_a");
