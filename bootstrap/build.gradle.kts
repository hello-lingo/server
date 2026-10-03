plugins {
	kotlin("plugin.spring")
	id("org.springframework.boot")
}

dependencies {
	implementation(project(":common"))
	implementation(project(":domain"))
	implementation(project(":application"))
	implementation(project(":adapter:in-web"))
	implementation(project(":adapter:out-persistence"))
	implementation(project(":adapter:out-external"))
	implementation("org.springframework.boot:spring-boot-micrometer-tracing-brave")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("io.micrometer:micrometer-tracing-bridge-brave")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
	testImplementation("org.springframework:spring-jdbc")
	testRuntimeOnly("com.h2database:h2")
}
