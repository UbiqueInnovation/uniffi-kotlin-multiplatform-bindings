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

#[derive(uniffi::Object)]
pub struct Car;

#[uniffi::export]
impl Car {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {}
    }

    pub fn drive(&self) -> String {
        "vroom".to_string()
    }
}

#[uniffi::export(with_foreign)]
pub trait MyCallback: Send + Sync {
    fn callback(&self) -> String;
}

#[uniffi::export]
pub fn call_callback(c: &dyn MyCallback) -> String {
    c.callback()
}

#[uniffi::export(async_runtime = "tokio")]
pub async fn async_hello() -> String {
    "Hello".to_string()
}

#[uniffi::export(with_foreign)]
#[async_trait::async_trait]
pub trait MyAsyncCallback: Send + Sync {
    async fn callback(&self) -> String;
}

#[uniffi::export(async_runtime = "tokio")]
pub async fn call_async_callback(c: &dyn MyAsyncCallback) -> String {
    c.callback().await
}

setup_scaffolding!("runtime_test");
