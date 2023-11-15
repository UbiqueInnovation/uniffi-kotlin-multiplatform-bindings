typedef struct RustCallStatus {
    int8_t code;
    RustBuffer errorBuf;
} RustCallStatus;

{# TODO move this include to CallbackInterfaceImpl #}
{% include "CallbackInterfaceRuntime.h" %}

typedef void (*UniFfiRustFutureContinuation)(uint64_t, int16_t);
