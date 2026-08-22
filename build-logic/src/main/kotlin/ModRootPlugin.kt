import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import me.modmuss50.mpp.ModPublishExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.getByType

class ModRootPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		with(project) {
			val stonecutter = extensions.getByType<StonecutterControllerExtension>()
			val properties = stonecutter.properties
			val modVersion = properties.get<String>("mod.version")
			val channelTag = properties.get<String>("mod.channel_tag")
			val changelogText = file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: ""
			val githubToken = providers.environmentVariable("GITHUB_TOKEN")

			extensions.configure<ModPublishExtension>("publishMods") {
				dryRun = envTrue("PUB_DRY_RUN") || !envTrue("PUB_GITHUB_ENABLE")
				version = modVersion
				changelog.set(changelogText)
				type = releaseTypeFromChannelTag(channelTag)
				if (envTrue("PUB_GITHUB_ENABLE")) {
					github {
						accessToken = githubToken
						repository = providers.environmentVariable("GITHUB_REPOSITORY")
						commitish = providers.environmentVariable("GITHUB_SHA").orElse("main")
						tagName = providers.environmentVariable("GITHUB_REF_NAME")
						allowEmptyFiles = true
					}
				}
			}

			stonecutter.tasks {
				order("publishModrinth")
				order("publishCurseforge")
			}

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

			for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
				group = "publishing"
				dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
			}
		}
	}
}
