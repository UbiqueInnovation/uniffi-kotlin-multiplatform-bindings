
{%- let inner_type_name = inner_type|type_name(ci) %}

public object {{ ffi_converter_name }}: FfiConverterRustBuffer<Set<{{ inner_type_name }}>> {
    override fun read(buf: ByteBuffer): Set<{{ inner_type_name }}> {
        val len = buf.getInt()
        return buildSet<{{ inner_type_name }}>(len) {
            repeat(len) {
                add({{ inner_type|read_fn }}(buf))
            }
        }
    }

    override fun allocationSize(value: Set<{{ inner_type_name }}>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { {{ inner_type|allocation_size_fn }}(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: Set<{{ inner_type_name }}>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach {
            {{ inner_type|write_fn }}(it, buf)
        }
    }
}
