use external_lib::MyStruct;

pub fn add_one(x: i32) -> i32 {
    x + 1
}

pub fn add_one_to_my_struct(s: MyStruct) -> MyStruct {
    MyStruct { value: s.value + 1 }
}

uniffi::include_scaffolding!("simple");
