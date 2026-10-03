plugins {
	kotlin("plugin.spring")
}

dependencies {
	implementation(project(":application"))
	implementation(project(":common"))
	implementation("org.springframework:spring-context")
	implementation("org.springframework.security:spring-security-crypto")
}
