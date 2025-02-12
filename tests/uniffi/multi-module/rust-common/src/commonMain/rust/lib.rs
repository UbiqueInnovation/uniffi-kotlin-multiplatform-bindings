#[derive(uniffi::Record)]
pub struct TestRecord {
    pub int: i64,
    pub str: String,
    pub vec: Vec<i64>,
}

#[derive(uniffi::Object)]
pub struct TestObject {
    pub name: String,
}

#[uniffi::export]
impl TestObject {
    #[uniffi::constructor]
    pub fn new(name: String) -> Self {
        Self { name }
    }

    pub fn get_name(&self) -> String {
        self.name.clone()
    }
}

#[uniffi::export(with_foreign)]
pub trait TestCallback: Send + Sync {
    fn callback(&self) -> String;
}

#[uniffi::export]
fn same_crate_call_callback(c: &dyn TestCallback) -> String {
    c.callback()
}

uniffi::setup_scaffolding!("rust_common");
