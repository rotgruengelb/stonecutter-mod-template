plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	jarTask = "jar"
	sourcesJarTask = "sourcesJar"
}

neoForge {
	version = property("deps.neoforge") as String
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	runs {
		register("neoforge-client") {
			gameDirectory = file("run/")
			client()
		}
		register("neoforge-server") {
			gameDirectory = file("run/")
			server()
		}
	}

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir("src/main/generated")
}

dependencies {

}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
