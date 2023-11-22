{%- let inner_type_name = inner_type|type_name %}

object {{ ffi_converter_name }}: FfiConverterRustBuffer<List<{{ inner_type_name }}>> {
    override fun read(source: NoCopySource): List<{{ inner_type_name }}> {
        val len = source.readInt()
        return List<{{ inner_type_name }}>(len) {
            {{ inner_type|read_fn }}(source)
        }
    }

    override fun allocationSize(value: List<{{ inner_type_name }}>): kotlin.Int {
        val sizeForLength = 4
        val sizeForItems = value.map { {{ inner_type|allocation_size_fn }}(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<{{ inner_type_name }}>, buf: Buffer) {
        buf.writeInt(value.size)
        value.forEach {
            {{ inner_type|write_fn }}(it, buf)
        }
    }
}