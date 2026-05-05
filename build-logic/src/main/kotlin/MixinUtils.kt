@file:Suppress("unused")

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.logging.Logger

fun List<String>.toMixinJsonArray(): String {
	if (isEmpty()) return "[]"
	return joinToString(",\n    ", prefix = "[\n    ", postfix = "\n  ]") { "\"$it\"" }
}

fun resolveMixinsForVersion(
	sideConfig: MixinsSideConfig,
	mcVersion: String,
	stonecutter: StonecutterBuildExtension
): List<String> {
	val result = mutableListOf<String>()

	for (group in sideConfig.definitions) {
		val shouldInclude = when (group) {
			is MixinGroup.Always -> true
			is MixinGroup.MinVersion -> stonecutter.compare(mcVersion, group.min) >= 0
			is MixinGroup.MaxVersion -> stonecutter.compare(mcVersion, group.max) <= 0
			is MixinGroup.VersionRange ->
				stonecutter.compare(mcVersion, group.min) >= 0 &&
					stonecutter.compare(mcVersion, group.max) <= 0
			is MixinGroup.ExactVersion -> stonecutter.compare(mcVersion, group.version) == 0
		}

		if (shouldInclude) {
			result.addAll(group.mixins)
		}
	}

	return result.distinct()
}

fun MixinsExtension.hasAnyMixins(): Boolean {
	return common.definitions.isNotEmpty() ||
		client.definitions.isNotEmpty() ||
		server.definitions.isNotEmpty()
}

fun logMixinConfiguration(
	logger: Logger,
	mcVersion: String,
	commonCount: Int,
	clientCount: Int,
	serverCount: Int
) {
	logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
	logger.lifecycle("🎯 Dynamic Mixin System Detected")
	logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
	logger.lifecycle("  📦 Minecraft Version: $mcVersion")
	logger.lifecycle("  🔧 Common Mixins:     $commonCount")
	logger.lifecycle("  💻 Client Mixins:     $clientCount")
	logger.lifecycle("  🖥️  Server Mixins:     $serverCount")
	logger.lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
}
