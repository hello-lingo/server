plugins {
	kotlin("jvm") version "2.3.21" apply false
	kotlin("plugin.spring") version "2.3.21" apply false
	kotlin("plugin.jpa") version "2.3.21" apply false
	id("org.springframework.boot") version "4.1.1" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
	group = "com.lingo"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}
}

subprojects {
	// :adapter는 빌드 파일 없는 그룹 프로젝트
	if (path == ":adapter") return@subprojects

	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "io.spring.dependency-management")

	extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
		imports {
			mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
		}
	}

	extensions.configure<JavaPluginExtension> {
		toolchain {
			languageVersion = JavaLanguageVersion.of(21)
		}
	}

	extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
		compilerOptions {
			freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
		}
	}

	dependencies {
		"testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
		"testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}
