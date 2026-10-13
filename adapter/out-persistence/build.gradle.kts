plugins {
	kotlin("plugin.spring")
	kotlin("plugin.jpa")
}

dependencies {
	implementation(project(":application"))
	implementation(project(":common"))
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testRuntimeOnly("com.h2database:h2")
}
