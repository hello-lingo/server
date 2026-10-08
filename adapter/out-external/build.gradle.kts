plugins {
	kotlin("plugin.spring")
}

dependencies {
	implementation(project(":application"))
	implementation(project(":common"))
	implementation("org.springframework:spring-context")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
}
