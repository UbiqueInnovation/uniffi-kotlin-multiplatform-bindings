use external_lib::MyStruct;

#[uniffi::export]
pub fn add_one(x: i32) -> i32 {
    x + 1
}

#[uniffi::export]
pub fn add_one_to_my_struct(s: MyStruct) -> MyStruct {
    MyStruct { value: s.value + 1 }
}

uniffi::setup_scaffolding!("simple");
