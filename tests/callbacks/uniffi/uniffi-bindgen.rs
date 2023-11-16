use std::env;
use std::env::consts::DLL_EXTENSION;
use camino::Utf8Path;

use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    let path = env::current_dir().unwrap();
    let test_directory = path.parent().unwrap();
    let test_directory_name = test_directory.file_name().unwrap();
    let test = test_directory_name.to_str().unwrap();

    let udl_file_path = format!("./src/{}.udl", test);
    let udl_file = Utf8Path::new(&udl_file_path);

    let library_file_path = format!("./target/debug/lib{}.{}", test, DLL_EXTENSION);
    let library_file = Utf8Path::new(&library_file_path);
    let out_dir = Utf8Path::new("./target/bindings");
    uniffi_bindgen::generate_external_bindings(
        KotlinBindingGenerator {},
        udl_file,
        None::<&Utf8Path>,
        Some(out_dir),
        Some(library_file),
        None::<&str>,
    ).unwrap();
}
