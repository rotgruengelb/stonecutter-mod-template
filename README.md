# Stonecutter Mod Template

A multi-platform Minecraft mod template for **Fabric**, **NeoForge**, and **Forge**,
using [Stonecutter](https://stonecutter.kikugie.dev/) for
multiversion and multiloader code.
This is the Java-only version adapted from KikuGie's Elytra Trims
rewrite following major Stonecutter feature updates.

This template is as "batteries included" as possible.
If you don't like this, it's not the right template for
you ([Alternative Templates](https://stonecutter.kikugie.dev/wiki/tips/multiloader)).

## Features

* Single codebase for Fabric, NeoForge, and Forge
* Single codebase for multiple Minecraft versions
* CI/CD with GitHub Actions for automated builds and releases
* Separate build scripts for each platform

## Getting Started

### Prerequisites

* Knowledge of Fabric, NeoForge, and/or Forge
* Suitable IDE
* Java 25 or higher
* Git

### Initial Setup

#### 1. **Clone or use this template**

```bash
git clone https://github.com/rotgruengelb/stonecutter-mod-template.git
cd stonecutter-mod-template
```

#### 2. **Open in your IDE**

Import the project as a Gradle project
in your preferred IDE (e.g., IntelliJ IDEA, Eclipse).

#### 3. **Stonecutter IntelliJ plugin**

The IntelliJ plugin adds comment syntax highlighting and completion,
a button to switch the active version, alongside other utilities.

#### 4. **Configure your mod**

Edit `stonecutter.properties.toml` to set your mod's metadata:

| Property           | Description                                  | Example                                                           |
|--------------------|----------------------------------------------|-------------------------------------------------------------------|
| `mod.id`           | Your mod's identifier (lowercase, no spaces) | `modtemplate`                                                     |
| `mod.name`         | Display name of your mod                     | `Mod Template`                                                    |
| `mod.group`        | Java package group                           | `com.example`                                                     |
| `mod.version`      | Mod version number                           | `0.1.0`                                                           |
| `mod.channel_tag`  | Optional release channel tag                 | `-alpha.0`                                                        |
| `mod.authors`      | Name of the author(s), as a TOML array       | `["AuthorName"]`                                                  |
| `mod.contributors` | Contributor names, as a TOML array           | `["ContributorName", "AnotherContributorName"]`                   |
| `mod.license.name` | License type                                 | `MIT`                                                             |
| `mod.description`  | Short mod description                        | `Example Description`                                             |
| `mod.sources_url`  | Link to your source code repository          | `https://github.com/rotgruengelb/stonecutter-mod-template`        |
| `mod.homepage_url` | Mod homepage or info page                    | `https://github.com/rotgruengelb/stonecutter-mod-template`        |
| `mod.issues_url`   | Link to issue tracker                        | `https://github.com/rotgruengelb/stonecutter-mod-template/issues` |
| `mod.discord_url`  | Link to a Discord invite                     | `https://discord.gg/aunYJB4wz9`                                   |

Dependencies and properties that are specific to a version/loader
are defined in `stonecutter.properties.toml` under their respective `[loader."version"]` table,
e.g. `[fabric."1.21.7"]`.

#### 5. **Rename package structure**

Rename the `com.example.modtemplate` package in
`src/main/java/` to match your `mod.group` and `mod.id`.

#### 6. **Update resource files**

Rename these files to match your `mod.id`:

* `src/main/resources/modtemplate.mixins.json`

Replace `src/main/resources/assets/icon.png` and `.idea/icon.png` with your mod's icon.

## Development

### Stonecutter

[Stonecutter](https://stonecutter.kikugie.dev/) allows multiple Minecraft versions and loaders in a single codebase.
Configure Stonecutter in `stonecutter.gradle.kts` and `settings.gradle.kts`.

Example of platform-specific code using Stonecutter comments:

```java
//? fabric {
fabricOnlyCode();
//?} else {
/*neoforgeOnlyCode();*/
//?}
```

Version-specific code works similarly:

```java
//? 1.21.7 {
LOGGER.info("hello 1.21.7!");
//?} else {
/*LOGGER.info("hello from any other version!");
 *///?}
```

For more details, read the [Stonecutter documentation](https://stonecutter.kikugie.dev/wiki/).

### Access Wideners/Transformers

* Fabric Access Wideners: `src/main/resources/aw/*.accesswidener` (one per supported Minecraft version)
* (Neo)Forge Access Transformers: `src/main/resources/aw/*.cfg` (one per supported Minecraft version)

### Running in Development

The Gradle plugins of the respective platform should provide run configurations.
If not, you can run the server and client with the respective Gradle tasks.
Be careful to run the correct task for the selected Stonecutter platform and Minecraft version.

### Platform Abstraction

The template uses a platform abstraction pattern to keep shared code loader-agnostic:

* **Shared code** goes in `com.example.modtemplate` (no platform dependencies)
* **Platform-specific code** goes in `com.example.modtemplate.platform.{fabric|neoforge|forge}`
* The `Platform` interface provides loader-specific functionality to shared code

### Adding Dependencies

To add dependencies for a specific platform, modify the `platform` block in the respective `build.gradle.kts` file.
The declared dependencies are automatically added to the metadata file for the loader and when publishing the mod to
mod hosting platforms.
**Important:** This does not replace the `dependencies` block!

```kotlin
platform {
  loader = "fabric" // or "neoforge" / "forge"
  dependencies {
    required("my-lib") {
      slug("my-lib") // Mod hosting platform slug (here the slug is the same on both Modrinth and CurseForge)
      versionRange = ">=${prop("deps.my-lib")}" // version range (for fabric.mod.json)
      forgeVersionRange =
        "[${prop("deps.my-lib")},)" // version range (for neoforge mods.toml), uses Maven version range syntax
    }
  }
}
```

### Data Generation

Run Fabric data generation to create recipes, tags, and other data:

```bash
./gradlew :1.21.7-fabric:runDatagen
```

Generated files appear in `versions/datagen/{mc_version}/src/main/generated/`.
The current setup uses Fabric data generation for all platforms to keep everything consistent.

### Environment Variables

Copy `.env.template` to `.env` and fill in the values for local publishing. The `.env` file is loaded automatically by
the `dotenv-gradle` plugin. When using the CI, set the corresponding repository secrets and variables instead
(see [Using the CI](#using-the-ci)).

Note: When using the CI `MOD_IS_RELEASE` is managed automatically, it is set by the CI based on the workflow trigger and
does not need to be set manually.

| Variable                      | Description                                                                | CI type  |
|-------------------------------|----------------------------------------------------------------------------|----------|
| `PUB_DRY_RUN`                 | Set to `true` to simulate publishing without actually uploading            | Variable |
| `PUB_MODS_ENABLE`             | Set to `true` to enable publishing to Modrinth and CurseForge              | Variable |
| `PUB_MAVEN_ENABLE`            | Set to `true` to enable Maven publishing                                   | Variable |
| `PUB_MAVEN_CENTRAL_ENABLE`    | Set to `true` to publish to Maven Central                                  | Variable |
| `PUB_MAVEN_CENTRAL_SNAPSHOTS` | Set to `true` to also publish snapshot versions to Maven Central           | Variable |
| `PUB_MODRINTH_STAGING`        | Set to `true` to publish to the Modrinth staging API instead of production | Variable |
| `PUB_MODRINTH_PROJECT_ID`     | Your Modrinth project ID                                                   | Variable |
| `PUB_CURSEFORGE_PROJECT_ID`   | Your CurseForge project ID                                                 | Variable |
| `PUB_GITHUB_RELEASES`         | Set to `true` to publish GitHub releases on tag push (CI only!)            | Variable |
| `PUB_MODRINTH_TOKEN`          | Your Modrinth personal access token                                        | Secret   |
| `PUB_CURSEFORGE_TOKEN`        | Your CurseForge API token                                                  | Secret   |
| `PUB_MAVEN_CENTRAL_USERNAME`  | Your Maven Central (Sonatype) username                                     | Secret   |
| `PUB_MAVEN_CENTRAL_PASSWORD`  | Your Maven Central (Sonatype) password                                     | Secret   |
| `PUB_SIGNING_KEY`             | ASCII-armored PGP private key for artifact signing                         | Secret   |
| `PUB_SIGNING_ID`              | PGP key ID                                                                 | Secret   |
| `PUB_SIGNING_PASSWORD`        | Passphrase for the PGP signing key                                         | Secret   |

### Using the CI

The template includes two GitHub Actions workflows.

**`build.yml`** runs on every push and pull request. It builds all versions and uploads the jars as artifacts. No
configuration needed beyond having a working build.

**`release.yml`** runs when a tag is pushed. It validates that the tag matches `mod.version` + `mod.channel_tag` in
`stonecutter.properties.toml`, builds all versions, generates a changelog via [`git-cliff`](https://git-cliff.org/), and
then publishes to
whichever platforms you have enabled.

To set up the CI for publishing:

1. Go to your repository on GitHub and open **Settings > Secrets and variables > Actions**.
2. Under **Secrets**, add any credentials you need (tokens, signing key, Maven credentials).
3. Under **Variables**, add the toggles and IDs for your platforms. At minimum, you likely want:
   <br/>`PUB_MODS_ENABLE = true`
   <br/>`PUB_GITHUB_RELEASES = true`
   <br/>`PUB_MODRINTH_PROJECT_ID = <your id>`
   <br/>`PUB_CURSEFORGE_PROJECT_ID = <your id>`

4. To trigger a release, make sure `mod.version` and `mod.channel_tag` in `stonecutter.properties.toml` reflect the
   version you want to release, then push a tag that matches the combined value:
   ```bash
   git tag 0.1.0-alpha.2
   git push origin 0.1.0-alpha.2
   ```
   If the tag does not match the version in the properties file the workflow will delete the tag and fail early without
   building.

## Dependency Updates with Renovate

The file `renovate.json.example` contains a minimal [Renovate](https://docs.renovatebot.com/) configuration.
If you want automated dependency update PRs, rename it to `renovate.json` and enable the Renovate GitHub App on your
repository. The default config (`config:recommended`) is enough to get started.

## Resources and Links

- [Stonecutter Documentation](https://stonecutter.kikugie.dev/wiki/)
- [NeoForge Documentation](https://docs.neoforged.net/docs/gettingstarted/)
- [Fabric Documentation](https://docs.fabricmc.net/develop/)
- [Pre-commit](https://pre-commit.com/)
- [Git Source Control](https://git-scm.com/doc)
- [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
- [Semantic Versioning](https://semver.org/)
  - [How to denote a pre-release version](https://semver.org/#spec-item-9)
- [Your Modrinth PAT](https://modrinth.com/settings/pats)
- [Your CurseForge API Tokens](https://legacy.curseforge.com/account/api-tokens)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Documentation](https://docs.gradle.org/current/userguide/userguide.html)
- [Git Cliff (Automatic Changelogs)](https://git-cliff.org/)

### Help and Support

For help and support, consider the following places:

- ["Kiku's Realm" Discord Server](https://discord.kikugie.dev/) for Stonecutter-related questions.
- ["Cascading Colors" (My) Discord Server](https://discord.gg/aunYJB4wz9) for questions about this template and its
  setup.
- ["The NeoForge Project" Discord Server](https://github.com/neoforged) for NeoForge-related questions.
- ["The Fabric Project" Discord Server](https://discord.gg/v6v4pMv) for Fabric-related questions.

## License/Credits

This template is provided under the MIT License.
Check `LICENSE` for details.

* Based on [murderspagurder/mod-template-java](https://github.com/murderspagurder/mod-template-java)
  * Adapted from [KikuGie's Elytra Trims](https://github.com/kikugie/elytra-trims) setup
* Uses [Stonecutter](https://stonecutter.kikugie.dev/) by KikuGie
