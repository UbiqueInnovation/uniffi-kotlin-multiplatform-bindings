{#- External types need a local RustBuffer alias. `Type::External` was removed
 # in uniffi 0.29, so these come from `iter_external_types()` now. -#}
{%- for type_ in ci.iter_external_types() %}
{%- match type_.name() %}
{%- when Some(name) %}

typedef RustBuffer RustBuffer{{ name }};
{%- when None %}
{%- endmatch %}
{%- endfor %}
