use uniffi::setup_scaffolding;

#[uniffi::export]
pub fn hello() -> String {
    "Hello, World!".to_string()
}

#[derive(uniffi::Record)]
pub struct Person {
    pub name: String,
    pub age: i64,
}

#[uniffi::export]
pub fn greet(person: &Person) -> String {
    format!(
        "Hello {name}, you're {age} years old!",
        name = person.name,
        age = person.age
    )
}

setup_scaffolding!();
