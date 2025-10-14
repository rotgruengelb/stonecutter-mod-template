@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import javax.inject.Inject

fun Project.prop(name: String) = property(name) as String

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val extension = extensions.create("platform", ModPlatformExtensionImpl::class.java).apply {
			loader.convention("fabric")
			jarTask.convention("remapJar")
			sourcesJarTask.convention("remapSourcesJar")
		}

		afterEvaluate {
			configureProject(extension)
		}
	}

	private fun Project.configureProject(extension: ModPlatformExtensionImpl) {
		val loader = extension.loader.get()
		val isFabric = loader == "fabric"
		val isNeoForge = loader == "neoforge"

		val modId = prop("mod.id")
		val modVersion = prop("mod.version")
		val channelTag = prop("mod.channel_tag")
		val mcVersion = prop("deps.minecraft")

		val stonecutter = extensions.getByType<StonecutterBuildExtension>()

		listOf(
			"java", "me.modmuss50.mod-publish-plugin"
		).forEach { apply(plugin = it) }

		version = "$modVersion$channelTag+$mcVersion-$loader"

		configureJarTask(modId)
		configureProcessResources(isFabric, isNeoForge, modId, modVersion, mcVersion)
		configureJsonLang(modId)
		configureJava(stonecutter)
		registerBuildAndCollectTask(extension, modVersion)
		configurePublishing(extension, loader, stonecutter, modVersion, channelTag)
	}

	private fun Project.configureJarTask(modId: String) {
		tasks.withType<Jar>().configureEach {
			archiveBaseName.set(modId)
		}
	}

	private fun Project.configureProcessResources(
		isFabric: Boolean, isNeoForge: Boolean, modId: String, modVersion: String, mcVersion: String
	) {
		tasks.named<ProcessResources>("processResources") {
			var contributors = prop("mod.contributors")
			var authors = prop("mod.authors")

			if (isFabric) {
				contributors = contributors.replace(", ", "\", \"")
				authors = authors.replace(", ", "\", \"")
			}

			val props = mapOf(
				"version" to modVersion,
				"minecraft" to mcVersion,
				"id" to modId,
				"name" to prop("mod.name"),
				"group" to prop("mod.group"),
				"authors" to authors,
				"contributors" to contributors,
				"license" to prop("mod.license"),
				"description" to prop("mod.description")
			)

			when {
				isFabric -> filesMatching("fabric.mod.json") { expand(props) }
				isNeoForge -> filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
			}
		}
	}

	private fun Project.configureJsonLang(modId: String) {
//		extensions.configure<JsonLangExtension>("jsonlang") {
//			languageDirectories = listOf("assets/$modId/lang")
//			prettyPrint = true
//		}
	}

	private fun Project.configureJava(stonecutter: StonecutterBuildExtension) {
		extensions.configure<JavaPluginExtension>("java") {
			withSourcesJar()
			val javaVersion = if (stonecutter.eval(
					stonecutter.current.version, ">=1.21"
				)
			) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
			sourceCompatibility = javaVersion
			targetCompatibility = javaVersion
		}
	}

	private fun Project.registerBuildAndCollectTask(extension: ModPlatformExtensionImpl, modVersion: String) {
		tasks.register<Copy>("buildAndCollect") {
			group = "build"
			from(tasks.named(extension.jarTask.get()))
			into(rootProject.layout.buildDirectory.file("libs/$modVersion"))
			dependsOn("build")
		}
	}

	private fun Project.configurePublishing(
		ext: ModPlatformExtensionImpl,
		loader: String,
		stonecutter: StonecutterBuildExtension,
		modVersion: String,
		channelTag: String
	) {
		val additionalVersions = (findProperty("publish.additionalVersions") as String?)?.split(',')?.map(String::trim)
			?.filter(String::isNotEmpty).orEmpty()

		val releaseType = ReleaseType.of(
			channelTag.substringAfter('-').substringBefore('.').ifEmpty { "stable" })

		extensions.configure<ModPublishExtension>("publishMods") {
			val jarTask = tasks.named(ext.jarTask.get()).map { it as Jar }
			val srcJarTask = tasks.named(ext.sourcesJarTask.get()).map { it as Jar }
			val currentVersion = stonecutter.current.version
			val deps = ext.publishing.dependencies
			val testWithStaging = providers.environmentVariable("v").orNull == "true"

			file.set(jarTask.flatMap(Jar::getArchiveFile))
			additionalFiles.from(srcJarTask.flatMap(Jar::getArchiveFile))
			type = releaseType
			version.set(version.toString())
			changelog.set(rootProject.file("CHANGELOG.md").readText())
			modLoaders.add(loader)

			displayName =
				"${prop("mod.name")} $modVersion for $currentVersion ${loader.replaceFirstChar(Char::titlecase)}"

			modrinth(deps, ext, currentVersion, additionalVersions, testWithStaging)
			if (!testWithStaging) curseforge(deps, currentVersion, additionalVersions)
		}
	}

	fun whenNotNull(slug: Property<String>, action: (String) -> Unit) {
		if (!slug.orNull.isNullOrBlank()) action(slug.get())
	}

	private fun ModPublishExtension.modrinth(
		deps: DependencyContainer,
		ext: ModPlatformExtensionImpl,
		currentVersion: String,
		additionalVersions: List<String>,
		staging: Boolean
	) = modrinth {
		if (staging) apiEndpoint = "https://staging-api.modrinth.com/v2"
		projectId = project.prop("publish.modrinth")
		accessToken = project.providers.environmentVariable("MODRINTH_API_KEY").orNull
		minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

		deps.required.forEach { dep -> whenNotNull(dep.modrinth) { requires(it) } }
		deps.optional.forEach { dep -> whenNotNull(dep.modrinth) { optional(it) } }
		deps.incompatible.forEach { dep -> whenNotNull(dep.modrinth) { incompatible(it) } }
		deps.embeds.forEach { dep -> whenNotNull(dep.modrinth) { embeds(it) } }
	}


	private fun ModPublishExtension.curseforge(
		deps: DependencyContainer, currentVersion: String, additionalVersions: List<String>
	) = curseforge {
		projectId = project.prop("publish.curseforge")
		accessToken = project.providers.environmentVariable("CURSEFORGE_API_KEY").orNull
		minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

		deps.required.forEach { dep -> whenNotNull(dep.curseforge) { requires(it) } }
		deps.optional.forEach { dep -> whenNotNull(dep.curseforge) { optional(it) } }
		deps.incompatible.forEach { dep -> whenNotNull(dep.curseforge) { incompatible(it) } }
		deps.embeds.forEach { dep -> whenNotNull(dep.curseforge) { embeds(it) } }
	}
}
