import { readFile } from 'fs/promises';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';
import { combine } from '@ubique-innovation/wasm-share-memory';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// RUSTFLAGS="-C link-arg=--import-memory -Crelocation-model=pic" cargo +nightly build -Z build-std="core,std,alloc,panic_abort" --target wasm32-unknown-unknown --release --package uniffi-runtime
const runtimeBytes = new Uint8Array(
    await readFile(resolve(__dirname, '../../../../../target/wasm32-unknown-unknown/release/uniffi_runtime.wasm'))
);

// RUSTFLAGS="-C link-arg=--import-memory -Crelocation-model=pic" cargo +nightly build -Z build-std="core,std,alloc,panic_abort" --target wasm32-unknown-unknown --release --package uniffi-kmm-js-example
const libraryBytes = new Uint8Array(
    await readFile(resolve(__dirname, '../../../../../target/wasm32-unknown-unknown/release/uniffi_kmm_js_example.wasm'))
);

const { modules, neededPages } = await combine([runtimeBytes, libraryBytes]);

const memory = new WebAssembly.Memory({
    initial: neededPages,
});

const { instance: runtime } = await WebAssembly.instantiate(modules[0], {
    env: { memory: memory }
});

const { instance: library } = await WebAssembly.instantiate(modules[1], {
    env: { memory: memory }
});

const rustCall = (func, args) => {
    const malloc = runtime.exports.malloc;
    const free = runtime.exports.free;

    const ptr = malloc(32);

    const result = func(...args, ptr);

    const view = new DataView(memory.buffer);
    const code = view.getInt8(ptr, true);
    console.log(`> Code: ${code}`);
    if (code != 0) {
        const cap = view.getBigInt64(ptr + 8, true)
        const len = view.getBigInt64(ptr + 16, true)
        const err = view.getInt32(ptr + 24, true)
        console.log(`> Capacity: ${cap}`);
        console.log(`> Length: ${len}`);
        console.log(`> Pointer: ${ptr}`);
        return view.getInt32(err);
    }

    return result;
}

// Calling a function
const sum = rustCall(library.exports.uniffi_uniffi_kmm_js_example_fn_func_add, [9, 10]);
console.log(`9 + 10 = ${sum}`)

// when the first parameter is 0, the function will error
const error = rustCall(library.exports.uniffi_uniffi_kmm_js_example_fn_func_add, [0, 1]);
console.log(`Error (-1 expected): ${error}`)

const object = rustCall(library.exports.uniffi_uniffi_kmm_js_example_fn_func_create, [1337, 42]);
console.log(`Object pointer: ${object}`)

const result = rustCall(library.exports.uniffi_uniffi_kmm_js_example_fn_method_mystruct_sum, [object]);
console.log(`Object(1337, 42).sum() = ${result}`)
