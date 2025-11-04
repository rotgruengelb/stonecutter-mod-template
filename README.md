# Stonecutter Mod Template

A multi-platform Minecraft mod template for **Fabric** and **NeoForge**
using [Stonecutter](https://stonecutter.kikugie.dev/) for 
multiversion and multiloader code.
This is the Java-only version adapted from KikuGie's Elytra Trims 
rewrite following major Stonecutter feature updates.

This Template is as "Batteries included" as possible. 
If you don't like this, it is not the rigt template for you ([Alternative Templates](https://stonecutter.kikugie.dev/wiki/tips/multiloader))

## Features

- Single codebase for both Fabric and NeoForge
- Single codebase for multiple Minecraft versions
- CI/CD with GitHub Actions for automated builds and releases
- Seperate buildscripts for each platform

## Getting Started

### Prerequisites

- Knowlege of Fabric and NeoForge
- Suitable IDE
- Java 21 or higher
- Git

### Initial Setup

#### 1. **Clone or use this template**

```bash
git clone https://github.com/rotgruengelb/stonecutter-mod-template.git
cd stonecutter-mod-template
```

#### 2. **Open in your IDE**

Import the project as a Gradle project 
in your prefered IDE (e.g., IntelliJ IDEA, Eclipse).

#### 3. Stonecutter IntelliJ plugin

The IntelliJ plugin adds comment syntax highlighting and completion, 
a button to switch the active version, alongside other utilities.

#### 4. **Configure your mod**

Edit `gradle.properties` to set your mod's metadata:

- `mod.id` - Your mod's identifier (lowercase, no spaces)
- `mod.name` - Display name
- `mod.version` - Version number
- `mod.group` - Java package group (e.g., `com.yourname`)
- `mod.authors` - Name of the author(s) (comma-separated)
- `mod.description`: Short description

#### 5. **Rename package structure**

Rename the `com.example.modtemplate` package in 
`src/main/java/` to match your `mod.group` and `mod.id`.

#### 6. **Update resource files**

Rename these files to match your `mod.id`:

- `src/main/resources/modtemplate.accesswidener`
- `src/main/resources/modtemplate.mixins.json`

### Development

## Stonecutter

[Stonecutter](https://stonecutter.kikugie.dev/) allows multiple Minecraft versions and loaders in a single codebase.
Configure versions in `stonecutter.gradle.kts`.

Example of platform-specific code using Stonecutter comments:

```java
//? if fabric {
/*fabricOnlyCode();*/
//?} else {
neoforgeOnlyCode();
//?}
```

For more details, read the [Stonecutter documentation](https://stonecutter.kikugie.dev/wiki/).

## Running in Development

The gradel plugins of the respective platform sould provide run configurations.
If not you can run server and client with the respective Gradle tasks.
Be carefull to run the correct task for the selected stonecutter platform and Minecraft version.

## Platform Abstraction

The template uses a platform abstraction pattern to keep shared code loader-agnostic:

- **Shared code** goes in `com.example.modtemplate` (no platform dependencies)
- **Platform-specific code** goes in `com.example.modtemplate.platform.{fabric|neoforge}`
- The `Platform` interface provides loader-specific functionality to shared code

## Adding Dependencies

To add dependencies for a specific platform, modify the `platform` block in the respective `build.gradle.kts` file.
The declared dependencies are automatically added to the metadata file for the loader. And when publishing the mod to
mod hosting platforms.
Important: This does not replace the `dependencies` block!

```kotlin
platform {
  loader = "fabric" // or "neoforge"
  dependencies {
    required("my-lib") {
      slug("my-lib") // Mod hosting platform slug (here the slug is the same on both Modrinth and CurseForge)
      versionRange = ">=${prop("deps.my-lib")}" // version range (for fabric.mod.json)
      forgeVersionRange =
        "[${prop("deps.my-lib")},)" // version range (for neoforge mods.toml) uses maven version range syntax
    }
  }
}
```

## Data Generation

Run Fabric datagen to generate recipes, tags, and other data:

```bash
./gradlew :1.21.7-fabric:runDatagen
```

Generated files appear in `src/main/generated/`.
The current setup uses the fabric data generation for both platforms.

## License/Credits

This template is provided under the MIT License.
Check `LICENSE` for details.

- Based on [murderspagurder/mod-template-java](https://github.com/murderspagurder/mod-template-java)
  - Adapted from [KikuGie's Elytra Trims](https://github.com/kikugie/elytra-trims) setup
- Uses [Stonecutter](https://stonecutter.kikugie.dev/) by KikuGie
