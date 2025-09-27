plugins {
	id("dev.kikugie.stonecutter")
	id("co.uzzu.dotenv.gradle") version "4.0.0"
	id("fabric-loom") version "1.10-SNAPSHOT" apply false
	id("net.neoforged.moddev") version "2.0.95" apply false
	id("dev.kikugie.postprocess.jsonlang") version "2.1-beta.4" apply false
	id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false
}

stonecutter active "1.21.7-fabric"

stonecutter parameters {
	constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge")
	filters.include("**/*.fsh", "**/*.vsh")
}

stonecutter tasks {
	order("publishModrinth")
	order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
	group = "publishing"
	dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}
