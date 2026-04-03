plugins {
    id("uniffi-tests-from-library")
}

uniffi {
	generateBindingsForExternalCrates = true
}
