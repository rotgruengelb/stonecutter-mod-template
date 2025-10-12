@file:Suppress("unused")

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

fun NamedDomainObjectContainer<PublishingDependency>.add(slug: String) {
	create(slug) {
		modrinth.set(slug)
		curseforge.set(slug)
	}
}

fun NamedDomainObjectContainer<PublishingDependency>.add(modrinthSlug: String?, curseforgeSlug: String?) {
	val id = modrinthSlug ?: curseforgeSlug ?: error("At least one slug (Modrinth or CurseForge) must be provided")

	create(id) {
		if (modrinthSlug != null) modrinth.set(modrinthSlug)
		if (curseforgeSlug != null) curseforge.set(curseforgeSlug)
	}
}

interface ModPlatformExtension {
	val loader: Property<String>
	val jarTask: Property<String>
	val sourcesJarTask: Property<String>
	val publishing: PublishingConfig
	fun publishing(action: Action<PublishingConfig>)
}

interface PublishingConfig {
	val dependencies: DependencyContainer
	fun dependencies(action: Action<DependencyContainer>)
	fun required(modrinthSlug: String, curseforgeSlug: String? = modrinthSlug)
	fun optional(modrinthSlug: String, curseforgeSlug: String? = modrinthSlug)
	fun incompatible(modrinthSlug: String, curseforgeSlug: String? = modrinthSlug)
	fun embeds(modrinthSlug: String, curseforgeSlug: String? = modrinthSlug)
}

interface DependencyContainer {
	val required: NamedDomainObjectContainer<PublishingDependency>
	val optional: NamedDomainObjectContainer<PublishingDependency>
	val incompatible: NamedDomainObjectContainer<PublishingDependency>
	val embeds: NamedDomainObjectContainer<PublishingDependency>
}

interface PublishingDependency {
	val modrinth: Property<String>
	val curseforge: Property<String>
}

abstract class PublishingDependencyImpl @Inject constructor(
	val name: String
) : PublishingDependency {

	@get:Inject
	abstract val objects: ObjectFactory

	override val modrinth: Property<String> = objects.property(String::class.java)
	override val curseforge: Property<String> = objects.property(String::class.java)
}

abstract class ModPlatformExtensionImpl @Inject constructor(project: Project) : ModPlatformExtension {
	private val objects = project.objects
	override val loader: Property<String> = objects.property(String::class.java)
	override val jarTask: Property<String> = objects.property(String::class.java)
	override val sourcesJarTask: Property<String> = objects.property(String::class.java)
	override val publishing: PublishingConfig = objects.newInstance(PublishingConfigImpl::class.java, project)
	override fun publishing(action: Action<PublishingConfig>) = action.execute(publishing)
}

abstract class PublishingConfigImpl @Inject constructor(project: Project) : PublishingConfig {
	private val objects = project.objects
	override val dependencies: DependencyContainer = objects.newInstance(DependencyContainerImpl::class.java, project)
	override fun dependencies(action: Action<DependencyContainer>) = action.execute(dependencies)

	override fun required(modrinthSlug: String, curseforgeSlug: String?) =
		dependencies.required.add(modrinthSlug, curseforgeSlug)

	override fun optional(modrinthSlug: String, curseforgeSlug: String?) =
		dependencies.optional.add(modrinthSlug, curseforgeSlug)

	override fun incompatible(modrinthSlug: String, curseforgeSlug: String?) =
		dependencies.incompatible.add(modrinthSlug, curseforgeSlug)

	override fun embeds(modrinthSlug: String, curseforgeSlug: String?) =
		dependencies.embeds.add(modrinthSlug, curseforgeSlug)
}

@Suppress("UNCHECKED_CAST")
abstract class DependencyContainerImpl @Inject constructor(project: Project) : DependencyContainer {
	override val required: NamedDomainObjectContainer<PublishingDependency> =
		project.container(PublishingDependencyImpl::class.java) as NamedDomainObjectContainer<PublishingDependency>
	override val optional: NamedDomainObjectContainer<PublishingDependency> =
		project.container(PublishingDependencyImpl::class.java) as NamedDomainObjectContainer<PublishingDependency>
	override val incompatible: NamedDomainObjectContainer<PublishingDependency> =
		project.container(PublishingDependencyImpl::class.java) as NamedDomainObjectContainer<PublishingDependency>
	override val embeds: NamedDomainObjectContainer<PublishingDependency> =
		project.container(PublishingDependencyImpl::class.java) as NamedDomainObjectContainer<PublishingDependency>
}
