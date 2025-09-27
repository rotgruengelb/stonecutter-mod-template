@file:Suppress("UnstableApiUsage")

import me.modmuss50.mpp.ReleaseType
import kotlin.text.replace


plugins {
	id("fabric-loom")
	id("dev.kikugie.postprocess.jsonlang")
	id("me.modmuss50.mod-publish-plugin")
}

tasks.named<ProcessResources>("processResources") {
	fun prop(name: String) = project.property(name) as String

	val props = HashMap<String, String>().apply {
		this["version"] = prop("mod.version")
		this["minecraft"] = prop("deps.minecraft")
		this["id"] = prop("mod.id")
		this["name"] = prop("mod.name")
		this["group"] = prop("mod.group")
		this["authors"] = prop("mod.authors").replace(", ", "\", \"")
		this["contributors"] = prop("mod.contributors").replace(", ", "\", \"")
		this["license"] = prop("mod.license")
		this["description"] = prop("mod.description")
	}

	filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
		expand(props)
	}
}

version = "${property("mod.version")}-${property("mod.channel_tag")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

loom {
	accessWidenerPath = rootProject.file("src/main/resources/${property("mod.id")}.accesswidener")
}

jsonlang {
	languageDirectories = listOf("assets/${property("mod.id")}/lang")
	prettyPrint = true
}

repositories {
	mavenLocal()
	mavenCentral()
	fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
		forRepository { maven(url) { name = alias } }
		filter { groups.forEach(::includeGroup) }
	}
	strictMaven("https://maven.rotgruengelb.net/releases", "Rotgruengelb Releases", "net.rotgruengelb")
	strictMaven("https://maven.rotgruengelb.net/snapshots", "Rotgruengelb Snapshots", "net.rotgruengelb")
	strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
	strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
	strictMaven("https://maven.terraformersmc.com/releases", "TerraformersMC", "com.terraformersmc")
}

dependencies {
	minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
	mappings(loom.layered {
		officialMojangMappings()
		if (hasProperty("deps.parchment")) parchment("org.parchmentmc.data:parchment-${property("deps.parchment")}@zip")
	})
	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")

	modLocalRuntime("com.terraformersmc:modmenu:${property("deps.modmenu")}")

	val modules = listOf("transitive-access-wideners-v1", "registry-sync-v0", "resource-loader-v0")
	for (it in modules) modImplementation(fabricApi.module("fabric-$it", property("deps.fabric-api") as String))
}

fabricApi {
	configureDataGeneration {
		outputDirectory = file("$rootDir/src/main/generated")
		client = true
	}
}

tasks {
	processResources {
		exclude("**/neoforge.mods.toml", "**/mods.toml")
	}

	register<Copy>("buildAndCollect") {
		group = "build"
		from(remapJar.map { it.archiveFile })
		into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
		dependsOn("build")
	}
}

stonecutter {
	swaps["mod_version"] = "\"${property("mod.version")}\";"
	swaps["mod_id"] = "\"${property("mod.id")}\";"
	swaps["mod_name"] = "\"${property("mod.name")}\";"
	swaps["mod_group"] = "\"${property("mod.group")}\";"
}

java {
	withSourcesJar()
	val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.21")) {
		JavaVersion.VERSION_21
	} else {
		JavaVersion.VERSION_17
	}
	sourceCompatibility = javaCompat
	targetCompatibility = javaCompat
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> =
	additionalVersionsStr?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
val channelTag = property("mod.channel_tag") as String
val releaseType: ReleaseType = ReleaseType.of(channelTag.split(".")[0].removePrefix("-").ifEmpty { "stable" })

publishMods {
	file = tasks.remapJar.map { it.archiveFile.get() }
	additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })

	type = releaseType
	displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version} Fabric"
	version = version
	changelog = provider { rootProject.file("CHANGELOG.md").readText() }
	modLoaders.add("fabric")

	modrinth {
		projectId = property("publish.modrinth") as String
		accessToken = env.MODRINTH_API_KEY.orNull()
		minecraftVersions.add(stonecutter.current.version)
		minecraftVersions.addAll(additionalVersions)
		requires("fabric-api")
	}

	curseforge {
		projectId = property("publish.curseforge") as String
		accessToken = env.CURSEFORGE_API_KEY.orNull()
		minecraftVersions.add(stonecutter.current.version)
		minecraftVersions.addAll(additionalVersions)
		requires("fabric-api")
	}
}
