use uniffi::setup_scaffolding;

#[uniffi::export]
pub fn add(a: i32, b: i32) -> Result<i32, i32> {
    if a == 0 {
        Err(-1)
    } else {
        Ok(a + b)
    }
}

#[derive(uniffi::Object)]
pub struct MyStruct {
    pub a: i32,
    pub b: i32,
}

#[uniffi::export]
pub fn create(a: i32, b: i32) -> MyStruct {
    MyStruct { a, b }
}

#[uniffi::export]
impl MyStruct {
    pub fn sum(&self) -> i32 {
        self.a + self.b
    }
}

setup_scaffolding!();
