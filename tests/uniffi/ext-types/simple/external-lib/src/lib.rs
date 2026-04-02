#[derive(uniffi::Record)]
pub struct MyStruct {
    pub value: i32,
}

uniffi::setup_scaffolding!("external_lib");
