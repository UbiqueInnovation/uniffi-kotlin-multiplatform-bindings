rootProject.name = "tests"
include(
    "callbacks",
    "coverall",
    "external_types",
    "futures",
//    "keywords", // fails on native: https://youtrack.jetbrains.com/issue/KT-55154/cinterop-function-paramter-is-not-backquoted
)