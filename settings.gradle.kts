plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lingo"

include(
	"common",
	"domain",
	"application",
	":adapter:in-web",
	":adapter:out-persistence",
	":adapter:out-external",
	"bootstrap",
)
