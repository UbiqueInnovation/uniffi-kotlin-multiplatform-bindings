#[uniffi::export]
pub fn greet_from_rust(name: String) -> String {
    format!("Hello {name}, from Rust!")
}

uniffi::setup_scaffolding!();
