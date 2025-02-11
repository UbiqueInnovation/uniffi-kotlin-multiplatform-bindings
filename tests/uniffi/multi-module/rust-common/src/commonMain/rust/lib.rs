#[derive(uniffi::Record)]
pub struct TestRecord {
    pub int: i64,
    pub str: String,
    pub vec: Vec<i64>,
}

uniffi::setup_scaffolding!("rust_common");
