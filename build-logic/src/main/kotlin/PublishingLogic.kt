@file:Suppress("unused", "DuplicatedCode")

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import me.modmuss50.mpp.ModPublishExtension
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
	val releaseType = releaseTypeFromChannelTag(ctx.channelTag)

	extensions.configure<ModPublishExtension>("publishMods") {
		val mrStaging = envTrue("PUB_MODRINTH_STAGING")
		val modrinthAccessToken = env("PUB_MODRINTH_TOKEN")
		val curseforgeAccessToken = env("PUB_CURSEFORGE_TOKEN")

		val githubEnabled = envTrue("PUB_GITHUB_ENABLE")
		if (envTrue("PUB_DRY_RUN") || !envTrue("PUB_MODS_ENABLE")) {
			dryRun = true
		}

		val jarTask = ctx.extension.jarTask.flatMap { name -> tasks.named(name).map { it as Jar } }
		val srcJarTask = ctx.extension.sourcesJarTask.flatMap { name -> tasks.named(name).map { it as Jar } }

		file.set(jarTask.flatMap(Jar::getArchiveFile))
		additionalFiles.from(srcJarTask.flatMap(Jar::getArchiveFile))
		type = releaseType
		version = ctx.fullVersion
		changelog.set(rootProject.file("CHANGELOG.md").readText())
		modLoaders.add(ctx.loader.id)

		displayName =
			"${ctx.modName} ${ctx.basicVersion} ${ctx.loader.id.replaceFirstChar(Char::titlecase)} ${ctx.currentMcVersion}"

		if (githubEnabled) {
			github {
				accessToken = env("GITHUB_TOKEN")
				parent(rootProject.tasks.named("publishGithub"))
			}

			// The root task creates the release and writes the result file that children read at
			// execution time, so children must run after it. MPP's parent() only copies the
			// result provider without declaring a dependency, which would race under `org.gradle.parallel`.
			project.tasks.named("publishGithub").configure {
				dependsOn(rootProject.tasks.named("publishGithub"))
			}
		}

		modrinth(ctx, ctx.publishAdditionalVersions, mrStaging, modrinthAccessToken)
		if (!mrStaging) curseforge(ctx, ctx.publishAdditionalVersions, curseforgeAccessToken)
	}
}

private fun ModPublishExtension.modrinth(
	ctx: Context, additionalVersions: List<String>, staging: Boolean, accessToken: String?
) = modrinth {
	if (staging) apiEndpoint = "https://staging-api.modrinth.com/v2"

	environment = ctx.environment
	projectId = project.env("PUB_MODRINTH_PROJECT_ID")

	this.accessToken = accessToken
	minecraftVersions.addAll(listOf(ctx.currentMcVersion) + additionalVersions)

	if (!staging) {
		val platform = this
		project.afterEvaluate {
			val deps = ctx.extension.dependencies
			deps.required.forEach { dep -> whenNotNull(dep.modrinth) { platform.requires(it) } }
			deps.optional.forEach { dep -> whenNotNull(dep.modrinth) { platform.optional(it) } }
			deps.incompatible.forEach { dep -> whenNotNull(dep.modrinth) { platform.incompatible(it) } }
			deps.embeds.forEach { dep -> whenNotNull(dep.modrinth) { platform.embeds(it) } }
		}
	}
}

private fun ModPublishExtension.curseforge(
	ctx: Context, additionalVersions: List<String>, accessToken: String?
) = curseforge {
	projectId = project.env("PUB_CURSEFORGE_PROJECT_ID")

	client = ctx.environmentPhysicalClient
	server = ctx.environmentPhysicalServer

	this.accessToken = accessToken
	minecraftVersions.addAll(listOf(ctx.currentMcVersion) + additionalVersions)

	val platform = this
	project.afterEvaluate {
		val deps = ctx.extension.dependencies
		deps.required.forEach { dep -> whenNotNull(dep.curseforge) { platform.requires(it) } }
		deps.optional.forEach { dep -> whenNotNull(dep.curseforge) { platform.optional(it) } }
		deps.incompatible.forEach { dep -> whenNotNull(dep.curseforge) { platform.incompatible(it) } }
		deps.embeds.forEach { dep -> whenNotNull(dep.curseforge) { platform.embeds(it) } }
	}
}

private fun whenNotNull(stringProp: Property<String>, action: (String) -> Unit) {
	if (!stringProp.orNull.isNullOrBlank()) action(stringProp.get())
}
