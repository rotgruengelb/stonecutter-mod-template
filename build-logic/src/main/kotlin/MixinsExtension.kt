@file:Suppress("unused")

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class MixinsExtension @Inject constructor(objects: ObjectFactory) {

	val common: MixinsSideConfig = objects.newInstance(MixinsSideConfig::class.java)
	val client: MixinsSideConfig = objects.newInstance(MixinsSideConfig::class.java)
	val server: MixinsSideConfig = objects.newInstance(MixinsSideConfig::class.java)

	fun common(action: Action<MixinsSideConfig>) {
		action.execute(common)
	}

	fun client(action: Action<MixinsSideConfig>) {
		action.execute(client)
	}

	fun server(action: Action<MixinsSideConfig>) {
		action.execute(server)
	}
}

abstract class MixinsSideConfig @Inject constructor() {

	internal val definitions = mutableListOf<MixinGroup>()

	fun always(vararg mixins: String) {
		definitions.add(MixinGroup.Always(mixins.toList()))
	}

	fun minVersion(minVersion: String, vararg mixins: String) {
		definitions.add(MixinGroup.MinVersion(minVersion, mixins.toList()))
	}

	fun maxVersion(maxVersion: String, vararg mixins: String) {
		definitions.add(MixinGroup.MaxVersion(maxVersion, mixins.toList()))
	}

	fun versionRange(minVersion: String, maxVersion: String, vararg mixins: String) {
		definitions.add(MixinGroup.VersionRange(minVersion, maxVersion, mixins.toList()))
	}

	fun exactVersion(version: String, vararg mixins: String) {
		definitions.add(MixinGroup.ExactVersion(version, mixins.toList()))
	}
}

sealed class MixinGroup {
	abstract val mixins: List<String>

	data class Always(override val mixins: List<String>) : MixinGroup()
	data class MinVersion(val min: String, override val mixins: List<String>) : MixinGroup()
	data class MaxVersion(val max: String, override val mixins: List<String>) : MixinGroup()
	data class VersionRange(val min: String, val max: String, override val mixins: List<String>) : MixinGroup()
	data class ExactVersion(val version: String, override val mixins: List<String>) : MixinGroup()
}
