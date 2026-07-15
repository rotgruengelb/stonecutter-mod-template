@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.dotenv)
	alias(libs.plugins.loom.back.compat).apply(false)
	alias(libs.plugins.neoforged.moddev).apply(false)
	alias(libs.plugins.jsonlang.postprocess).apply(false)
	alias(libs.plugins.mod.publish.plugin).apply(false)
	alias(libs.plugins.kotlin.jvm).apply(false)
	alias(libs.plugins.devtools.ksp).apply(false)
	alias(libs.plugins.fletching.table).apply(false)
	alias(libs.plugins.legacyforge.moddev).apply(false)
}

stonecutter active file(".sc_active_version")

tasks.register("runActiveClient") {
	group = "stonecutter"
	description = "Run client of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runClient")
}

tasks.register("runActiveServer") {
	group = "stonecutter"
	description = "Run server of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runServer")
}

stonecutter parameters {
	constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge", "forge")
	swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
	swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
	swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
	swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
	swaps["minecraft"] = "\"${current.version}\";"
	constants["release"] = properties.get<String>("mod.id") != "modtemplate"
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
	group = "publishing"
	dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}
