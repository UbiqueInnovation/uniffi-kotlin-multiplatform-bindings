use uniffi::setup_scaffolding;

#[uniffi::export]
pub fn hello() -> String {
    "Hello, World!".to_string()
}

setup_scaffolding!();
