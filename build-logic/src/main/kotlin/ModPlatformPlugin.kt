@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.internal.extensions.stdlib.toDefaultLowerCase
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.util.*
import javax.inject.Inject

fun Project.prop(name: String): String = (findProperty(name) ?: "") as String
fun Project.env(variable: String): String? = providers.environmentVariable(variable).orNull
fun Project.envTrue(variable: String): Boolean = env(variable)?.toDefaultLowerCase() == "true"

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val inferredLoader = project.buildFile.name.substringAfter('.').replace(".gradle.kts", "")
		val inferredLoaderIsFabric = inferredLoader == "fabric"

		val extension = extensions.create("platform", ModPlatformExtensionImpl::class.java).apply {
			loader.convention(inferredLoader)
			jarTask.convention(if (inferredLoaderIsFabric) "remapJar" else "jar")
			sourcesJarTask.convention(if (inferredLoaderIsFabric) "remapSourcesJar" else "sourcesJar")
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

		listOf("java", "me.modmuss50.mod-publish-plugin", "idea").forEach { apply(plugin = it) }

		version = "$modVersion$channelTag+$mcVersion-$loader"

		configureJarTask(modId)
		configureIdea()
		configureProcessResources(isFabric, isNeoForge, modId, "$modVersion$channelTag", mcVersion, extension)
		configureJava(stonecutter)
		registerBuildAndCollectTask(extension, "$modVersion$channelTag")
		configurePublishing(extension, loader, stonecutter, "$modVersion$channelTag", channelTag, version.toString())
	}

	private fun Project.configureJarTask(modId: String) {
		tasks.withType<Jar>().configureEach {
			archiveBaseName.set(modId)
		}
	}

	private fun Project.configureProcessResources(
		isFabric: Boolean,
		isNeoForge: Boolean,
		modId: String,
		modVersion: String,
		mcVersion: String,
		extension: ModPlatformExtensionImpl
	) {
		tasks.named<ProcessResources>("processResources") {
			dependsOn(tasks.named("stonecutterGenerate"))
			var contributors = prop("mod.contributors")
			var authors = prop("mod.authors")
			var issuesUrl = prop("mod.issues_url")
			if (issuesUrl == "") issuesUrl = prop("mod.sources_url") + "/issues"

			if (isFabric) {
				contributors = contributors.replace(", ", "\", \"")
				authors = authors.replace(", ", "\", \"")
			}

			val dependencies = buildDependenciesBlock(isFabric, modId, extension.dependencies)

			val props = mapOf(
				"version" to modVersion,
				"minecraft" to mcVersion,
				"id" to modId,
				"name" to prop("mod.name"),
				"group" to prop("mod.group"),
				"authors" to authors,
				"contributors" to contributors,
				"license" to prop("mod.license"),
				"description" to prop("mod.description"),
				"issues_url" to issuesUrl,
				"homepage_url" to prop("mod.homepage_url"),
				"sources_url" to prop("mod.sources_url"),
				"discord_url" to prop("mod.discord_url"),
				"dependencies" to dependencies
			)

			when {
				isFabric -> {
					filesMatching("fabric.mod.json") { expand(props) }
					exclude("META-INF/neoforge.mods.toml", "META-INF/accesstransformer.cfg", ".cache")
				}

				isNeoForge -> {
					filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
					exclude("fabric.mod.json", "${modId}.accesswidener", ".cache")
				}
			}
		}
	}

	private fun buildDependenciesBlock(
		isFabric: Boolean, modId: String, deps: DependenciesConfig
	): String = if (isFabric) {
		buildString {
			fun joinGroup(
				name: String, container: NamedDomainObjectContainer<Dependency>
			): String? {
				if (container.isEmpty()) return null
				val entries = container.joinToString(",\n    ") {
					"\"${it.modid.get()}\": \"${it.versionRange.get()}\""
				}
				return "\n  \"$name\": {\n    $entries\n  }"
			}

			val groups = listOfNotNull(
				joinGroup("depends", deps.required),
				joinGroup("recommends", deps.optional),
				joinGroup("breaks", deps.incompatible)
			)

			append(groups.joinToString(","))
		}
	} else {
		buildString {
			fun appendBlock(container: NamedDomainObjectContainer<Dependency>, type: String) {
				container.forEach {
					appendLine(
						"""

						[[dependencies.$modId]]
						modId = "${it.modid.get()}"
						side = "${it.environment.get().uppercase(Locale.getDefault())}"
                        versionRange = "${it.forgeVersionRange.get()}"
                        type = "$type"
						""".replace("                  ", "").trimIndent()
					)
				}
			}

			appendBlock(deps.required, "required")
			appendBlock(deps.optional, "optional")
			appendBlock(deps.incompatible, "incompatible")
		}
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

	private fun Project.configureIdea() {
		extensions.configure<IdeaModel>("idea") {
			module {
				isDownloadJavadoc = true
				isDownloadSources = true
			}
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
		channelTag: String,
		fullVersion: String,
	) {
		val additionalVersions = (findProperty("publish.additionalVersions") as String?)?.split(',')?.map(String::trim)
			?.filter(String::isNotEmpty).orEmpty()

		val releaseType = ReleaseType.of(
			channelTag.substringAfter('-').substringBefore('.').ifEmpty { "stable" })

		extensions.configure<ModPublishExtension>("publishMods") {
			val mrStaging = envTrue("TEST_PUBLISHING_WITH_MR_STAGING")

			val modrinthAccessToken = env("MODRINTH_API_TOKEN")
			val curseforgeAccessToken = env("CURSEFORGE_API_TOKEN")
			if (!envTrue("ENABLE_PUBLISHING")) {
				dryRun = true
			}

			val jarTask = tasks.named(ext.jarTask.get()).map { it as Jar }
			val srcJarTask = tasks.named(ext.sourcesJarTask.get()).map { it as Jar }
			val currentVersion = stonecutter.current.version
			val deps = ext.dependencies

			file.set(jarTask.flatMap(Jar::getArchiveFile))
			additionalFiles.from(srcJarTask.flatMap(Jar::getArchiveFile))
			type = releaseType
			version = fullVersion
			changelog.set(rootProject.file("CHANGELOG.md").readText())
			modLoaders.add(loader)

			displayName = "${prop("mod.name")} $modVersion ${loader.replaceFirstChar(Char::titlecase)} $currentVersion"

			modrinth(deps, currentVersion, additionalVersions, mrStaging, modrinthAccessToken)
			if (!mrStaging) curseforge(deps, currentVersion, additionalVersions, false, curseforgeAccessToken)
		}
	}

	fun whenNotNull(stringProp: Property<String>, action: (String) -> Unit) {
		if (!stringProp.orNull.isNullOrBlank()) action(stringProp.get())
	}

	private fun ModPublishExtension.modrinth(
		deps: DependenciesConfig,
		currentVersion: String,
		additionalVersions: List<String>,
		staging: Boolean,
		acesssToken: String?
	) = modrinth {
		if (staging) apiEndpoint = "https://staging-api.modrinth.com/v2"
		projectId = project.prop("publish.modrinth")
		accessToken = acesssToken
		minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

		if (!staging) {
			deps.required.forEach { dep -> whenNotNull(dep.modrinth) { requires(it) } }
			deps.optional.forEach { dep -> whenNotNull(dep.modrinth) { optional(it) } }
			deps.incompatible.forEach { dep -> whenNotNull(dep.modrinth) { incompatible(it) } }
			deps.embeds.forEach { dep -> whenNotNull(dep.modrinth) { embeds(it) } }
		}
	}

	private fun ModPublishExtension.curseforge(
		deps: DependenciesConfig,
		currentVersion: String,
		additionalVersions: List<String>,
		staging: Boolean,
		acesssToken: String?
	) = curseforge {
		projectId = project.prop("publish.curseforge")
		accessToken = acesssToken
		minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

		deps.required.forEach { dep -> whenNotNull(dep.curseforge) { requires(it) } }
		deps.optional.forEach { dep -> whenNotNull(dep.curseforge) { optional(it) } }
		deps.incompatible.forEach { dep -> whenNotNull(dep.curseforge) { incompatible(it) } }
		deps.embeds.forEach { dep -> whenNotNull(dep.curseforge) { embeds(it) } }
	}
}
