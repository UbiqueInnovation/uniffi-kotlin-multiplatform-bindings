{%- for type_ in ci.iter_external_types() %}
{%- match type_.name() %}
{%- when Some(name) %}

typedef RustBuffer RustBuffer{{ name }};
{%- when None %}
{%- endmatch %}
{%- endfor %}
