use std::sync::{Arc, Mutex};

#[uniffi::export]
pub fn hello() -> String {
    "Hello".to_string()
}

#[uniffi::export]
pub fn vec_length(vec: Vec<i64>) -> u64 {
    vec.len() as u64
}

#[derive(uniffi::Object)]
pub struct ObjB {
    counter: Arc<Mutex<i64>>,
}

#[uniffi::export]
impl ObjB {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            counter: Arc::new(Mutex::new(0)),
        }
    }

    pub fn get(&self) -> i64 {
        *self.counter.lock().unwrap()
    }

    pub fn inc(&self) {
        let mut x = self.counter.lock().unwrap();
        *x += 1;
    }
}

#[uniffi::export]
pub fn consume(obj: &ObjB) -> i64 {
    obj.get()
}

uniffi::setup_scaffolding!("mod_b");
