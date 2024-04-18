/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

#[no_mangle]
pub extern "system" fn Java_CargoOnlyLibrary_getHelloWorld<'env>(
    env: jni::JNIEnv<'env>,
    _class: jni::objects::JClass,
) -> jni::objects::JObject<'env> {
    env.new_string("Hello, world!").unwrap().into()
}

#[no_mangle]
pub extern "system" fn Java_CargoOnlyLibrary_wait(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    seconds: jni::sys::jlong,
    nanoseconds: jni::sys::jint,
    callback: jni::objects::JObject,
) {
    use std::thread;
    use std::time::Duration;

    if seconds < 0 || nanoseconds < 0 {
        return;
    }
    let duration = Duration::new(seconds as _, nanoseconds as _);
    let java_vm = env.get_java_vm().unwrap();
    let callback = env.new_global_ref(callback).unwrap();
    thread::spawn(move || {
        thread::sleep(duration);
        let mut attach_guard = java_vm.attach_current_thread().unwrap();
        let env = &mut *attach_guard;
        let callback = &*callback;
        env.call_method(callback, "invoke", "()V", &[]).unwrap();
    });
}

#[no_mangle]
pub extern "system" fn Java_CargoOnlyLibrary_getOptLevel(
    _env: jni::JNIEnv,
    _class: jni::objects::JClass,
) -> jni::sys::jint {
    env!("OPT_LEVEL").parse().unwrap()
}

#[no_mangle]
pub extern "system" fn Java_CargoOnlyLibrary_getFeatures<'env>(
    mut env: jni::JNIEnv<'env>,
    _class: jni::objects::JClass,
) -> jni::objects::JObjectArray<'env> {
    use jni::objects::JObject;

    let features = env!("FEATURES")
        .split(',')
        .filter(|s| !s.is_empty())
        .collect::<Vec<_>>();
    let string_class = env.find_class("java/lang/String").unwrap();
    let array = env
        .new_object_array(features.len() as _, string_class, JObject::null())
        .unwrap();

    for (idx, feature) in features.into_iter().enumerate() {
        let feature = env.new_string(feature).unwrap();
        env.set_object_array_element(&array, idx as _, feature)
            .unwrap();
    }

    array
}

#[no_mangle]
pub extern "C" fn CargoOnlyLibrary_getHelloWorld() -> *const std::ffi::c_char {
    b"Hello, world!\0".as_ptr() as *const _
}

#[no_mangle]
pub extern "C" fn CargoOnlyLibrary_wait(
    seconds: u64,
    nanoseconds: u32,
    callback_context: *mut std::ffi::c_void,
    callback: extern "C" fn(*mut std::ffi::c_void),
) {
    use std::ffi::c_void;
    use std::thread;
    use std::time::Duration;

    struct Callback(*mut c_void, extern "C" fn(*mut c_void));

    impl Callback {
        fn new(callback_context: *mut c_void, callback: extern "C" fn(*mut c_void)) -> Self {
            Self(callback_context, callback)
        }

        fn invoke(self) {
            (self.1)(self.0);
        }
    }

    unsafe impl Send for Callback {}

    let callback = Callback::new(callback_context, callback);

    thread::spawn(move || {
        thread::sleep(Duration::new(seconds, nanoseconds));
        callback.invoke();
    });
}

#[no_mangle]
pub extern "C" fn CargoOnlyLibrary_getOptLevel() -> std::ffi::c_int {
    env!("OPT_LEVEL").parse().unwrap()
}

#[no_mangle]
pub extern "C" fn CargoOnlyLibrary_getFeatures() -> *const std::ffi::c_char {
    concat!(env!("FEATURES"), "\0").as_ptr() as *const _
}
