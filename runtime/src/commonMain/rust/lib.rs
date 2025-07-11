use std::alloc::{alloc, dealloc, Layout};
use std::ptr::null_mut;

use uniffi::setup_scaffolding;

#[no_mangle]
pub extern "C" fn malloc(size: usize) -> *mut u8 {
    if size == 0 {
        return null_mut();
    }

    // Use default alignment
    let layout = Layout::from_size_align(size, std::mem::align_of::<usize>()).unwrap();
    unsafe { alloc(layout) }
}

#[no_mangle]
pub extern "C" fn free(ptr: *mut u8, size: usize) {
    if ptr.is_null() || size == 0 {
        return;
    }

    let layout = Layout::from_size_align(size, std::mem::align_of::<usize>()).unwrap();
    unsafe { dealloc(ptr, layout) }
}


setup_scaffolding!();
