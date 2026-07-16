import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

@OptIn(StonecutterExperimentalAPI::class)
class Context(
	val project: Project,
	val extension: ModPlatformExtension,
	val loader: Loader,
	val stonecutter: StonecutterBuildExtension
) {
	private fun require(key: String): String =
		runCatching { project.sc.properties.get<String>(key) }.getOrNull()?.takeIf { it.isNotBlank() }
			?: error("Missing required property '$key' in stonecutter.properties.toml")

	private fun optional(key: String, fallback: String = ""): String =
		runCatching { project.sc.properties.get<String>(key) }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback

	val currentMcVersion: String by lazy {
		stonecutter.current.version
	}

	val modId: String by lazy { require("mod.id") }
	val modName: String by lazy { require("mod.name") }
	val modGroup: String by lazy { require("mod.group") }
	val modVersion: String by lazy { require("mod.version") }
	val channelTag: String by lazy { optional("mod.channel_tag") }
	val description: String by lazy { optional("mod.description") }
	val licenseName: String by lazy { require("mod.license.name") }
	val licenseUrl: String by lazy { require("mod.license.url") }
	val licenseDist: String by lazy { optional("mod.license.dist", "repo") }
	val inceptionYear: String by lazy { optional("mod.inception_year") }

	val authors: List<String> by lazy {
		runCatching {
			project.sc.properties.raw("mod", "authors").asList().map { it.toString() }
		}.getOrElse { error("Missing or malformed 'mod.authors' in stonecutter.properties.toml") }
	}

	val contributors: List<String> by lazy {
		runCatching {
			project.sc.properties.raw("mod", "contributors").asList().map { it.toString() }
		}.getOrElse { emptyList() }
	}

	val sourcesUrl: String by lazy { require("mod.sources_url") }
	val homepageUrl: String by lazy { require("mod.homepage_url") }
	val discordUrl: String by lazy { optional("mod.discord_url") }
	val issuesUrl: String by lazy { optional("mod.issues_url", "$sourcesUrl/issues") }

	val isSnapshot: Boolean by lazy { !project.envTrue("MOD_IS_RELEASE") }
	val baseVersion: String by lazy { "$modVersion$channelTag" }
	val snapshotSuffix: String by lazy { if (isSnapshot) "-SNAPSHOT" else "" }
	val fullVersion: String by lazy { "$baseVersion-${loader.id}+$currentMcVersion$snapshotSuffix" }
	val basicVersion: String by lazy { "$baseVersion$snapshotSuffix" }

	val publishAdditionalVersions: List<String> by lazy {
		project.sc.properties.rawOrNull("publish", "additionalVersions")?.to<List<String>>().orEmpty()
	}

	val javaVersion: JavaVersion by lazy {
		when {
			stonecutter.eval(currentMcVersion, ">=26") -> JavaVersion.VERSION_25
			stonecutter.eval(currentMcVersion, ">=1.20.5") -> JavaVersion.VERSION_21
			stonecutter.eval(currentMcVersion, ">=1.18") -> JavaVersion.VERSION_17
			stonecutter.eval(currentMcVersion, ">=1.17") -> JavaVersion.VERSION_16
			else -> JavaVersion.VERSION_1_8
		}
	}
}
