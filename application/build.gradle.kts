plugins {
	kotlin("plugin.spring")
	`java-library`
}

dependencies {
	api(project(":domain"))
	implementation(project(":common"))
	implementation("org.springframework:spring-context")
	implementation("org.springframework:spring-tx")
}
