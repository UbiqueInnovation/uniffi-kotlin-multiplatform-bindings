
{%- let fully_qualified_type_name = "{}.{}"|format(package_name, name|class_name(ci)) %}
{{- self.add_import(fully_qualified_type_name) }}