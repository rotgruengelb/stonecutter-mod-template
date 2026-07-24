@file:Suppress("unused", "DuplicatedCode")

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign

fun Project.configureMavenPublishing(ctx: Context) {
	env("PUB_SIGNING_KEY")?.let { extensions.extraProperties["signing.key"] = it }
	env("PUB_SIGNING_ID")?.let { extensions.extraProperties["signing.keyId"] = it }
	env("PUB_SIGNING_PASSWORD")?.let { extensions.extraProperties["signing.password"] = it }
	env("PUB_MAVEN_CENTRAL_USERNAME")?.let { extensions.extraProperties["mavenCentralUsername"] = it }
	env("PUB_MAVEN_CENTRAL_PASSWORD")?.let { extensions.extraProperties["mavenCentralPassword"] = it }

	extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
		if (envTrue("PUB_MAVEN_CENTRAL_ENABLE")) {
			if (!ctx.isSnapshot || envTrue("PUB_MAVEN_CENTRAL_SNAPSHOTS")) {
				publishToMavenCentral()
			}
		}
		signAllPublications()

		coordinates(ctx.modGroup, ctx.modId, version as String)
		pom {
			name.set(ctx.modName)
			description.set(ctx.description)
			inceptionYear.set(ctx.inceptionYear)
			url.set(ctx.homepageUrl)
			licenses {
				license {
					name.set(ctx.licenseName)
					url.set(ctx.licenseUrl)
					distribution.set(ctx.licenseDist)
				}
			}
			developers {
				project.sc.properties.raw("mod.pom.developers").asList().forEach { devNode ->
					val dev = devNode.asMap()
					developer {
						id.set(dev["id"]?.toString())
						name.set(dev["name"]?.toString())
						url.set(dev["url"]?.toString())
					}
				}
			}
			scm {
				url.set(ctx.sourcesUrl)
				connection.set(ctx.sourcesUrl.replace("https://", "scm:git:git://").removeSuffix("/") + ".git")
				developerConnection.set(
					ctx.sourcesUrl.replace("https://", "scm:git:ssh://git@").removeSuffix("/") + ".git"
				)
			}
		}
	}
}

fun Project.configureModPublishing(ctx: Context) {
	val releaseType = ReleaseType.of(
		ctx.channelTag.substringAfter('-').substringBefore('.').ifEmpty { "stable" })

	extensions.configure<ModPublishExtension>("publishMods") {
		val mrStaging = envTrue("PUB_MODRINTH_STAGING")
		val modrinthAccessToken = env("PUB_MODRINTH_TOKEN")
		val curseforgeAccessToken = env("PUB_CURSEFORGE_TOKEN")

		if (envTrue("PUB_DRY_RUN") || !envTrue("PUB_MODS_ENABLE")) {
			dryRun = true
		}

		val jarTask = tasks.named(ctx.extension.jarTask.get()).map { it as Jar }
		val srcJarTask = tasks.named(ctx.extension.sourcesJarTask.get()).map { it as Jar }

		file.set(jarTask.flatMap(Jar::getArchiveFile))
		additionalFiles.from(srcJarTask.flatMap(Jar::getArchiveFile))
		type = releaseType
		version = ctx.fullVersion
		changelog.set(rootProject.file("CHANGELOG.md").readText())
		modLoaders.add(ctx.loader.id)

		displayName =
			"${ctx.modName} ${ctx.basicVersion} ${ctx.loader.id.replaceFirstChar(Char::titlecase)} ${ctx.currentMcVersion}"

		val deps = ctx.extension.dependencies

		modrinth(ctx, ctx.publishAdditionalVersions, mrStaging, modrinthAccessToken, deps)
		if (!mrStaging) curseforge(ctx, ctx.publishAdditionalVersions, curseforgeAccessToken, deps)
	}
}

private fun ModPublishExtension.modrinth(
	ctx: Context, additionalVersions: List<String>, staging: Boolean, accessToken: String?, deps: DependenciesConfig
) = modrinth {
	if (staging) apiEndpoint = "https://staging-api.modrinth.com/v2"

	projectId = project.env("PUB_MODRINTH_PROJECT_ID")
	environment = when (ctx.environment.lowercase()) {
		"client" -> ModrinthEnvironment.CLIENT_ONLY
		"server" -> ModrinthEnvironment.SERVER_ONLY
		else -> ModrinthEnvironment.CLIENT_AND_SERVER
	}

	this.accessToken = accessToken
	minecraftVersions.addAll(listOf(ctx.currentMcVersion) + additionalVersions)

	if (!staging) {
		deps.required.forEach { dep -> whenNotNull(dep.modrinth) { requires(it) } }
		deps.optional.forEach { dep -> whenNotNull(dep.modrinth) { optional(it) } }
		deps.incompatible.forEach { dep -> whenNotNull(dep.modrinth) { incompatible(it) } }
		deps.embeds.forEach { dep -> whenNotNull(dep.modrinth) { embeds(it) } }
	}
}

private fun ModPublishExtension.curseforge(
	ctx: Context, additionalVersions: List<String>, accessToken: String?, deps: DependenciesConfig
) = curseforge {
	projectId = project.env("PUB_CURSEFORGE_PROJECT_ID")
	client = ctx.environment.lowercase() in setOf("client", "both")
	server = ctx.environment.lowercase() in setOf("server", "both")

	this.accessToken = accessToken
	minecraftVersions.addAll(listOf(ctx.currentMcVersion) + additionalVersions)

	deps.required.forEach { dep -> whenNotNull(dep.curseforge) { requires(it) } }
	deps.optional.forEach { dep -> whenNotNull(dep.curseforge) { optional(it) } }
	deps.incompatible.forEach { dep -> whenNotNull(dep.curseforge) { incompatible(it) } }
	deps.embeds.forEach { dep -> whenNotNull(dep.curseforge) { embeds(it) } }
}

private fun whenNotNull(stringProp: Property<String>, action: (String) -> Unit) {
	if (!stringProp.orNull.isNullOrBlank()) action(stringProp.get())
}
