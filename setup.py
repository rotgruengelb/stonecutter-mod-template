#!/usr/bin/env python3
"""
Setup Script
Run from the repository root: python setup.py
Requires Python 3.8+, no external dependencies.
"""

import re
import shutil
import sys
from datetime import datetime
from pathlib import Path

CYAN = "\033[0;36m"
GREEN = "\033[0;32m"
YELLOW = "\033[1;33m"
RED = "\033[0;31m"
RESET = "\033[0m"

# Disable color on Windows if not supported
if sys.platform == "win32":
    try:
        import ctypes

        ctypes.windll.kernel32.SetConsoleMode(
            ctypes.windll.kernel32.GetStdHandle(-11), 7
        )
    except Exception:
        CYAN = GREEN = YELLOW = RED = RESET = ""


def info(msg):
    print(f"{CYAN}[setup]{RESET} {msg}")


def success(msg):
    print(f"{GREEN}[setup]{RESET} {msg}")


def warn(msg):
    print(f"{YELLOW}[setup]{RESET} {msg}")


def abort(msg):
    sys.exit(f"{RED}[setup]{RESET} {msg}")


def ask(prompt, default="", optional=False) -> str:
    display = f"{CYAN}[setup]{RESET} {prompt}"
    if default:
        display += f" {YELLOW}[{default}]{RESET}"
    if optional:
        display += " (optional)"
    display += ": "
    try:
        value = input(display).strip()
    except (EOFError, KeyboardInterrupt):
        print()
        abort("Interrupted.")
    value = value or default
    if not optional and not value:
        abort(f"A value is required for: {prompt}")
    return value


def ask_list(prompt, optional=False) -> list[str]:
    raw = ask(f"{prompt} (comma-separated)", optional=optional)
    if not raw:
        return []
    return [p.strip() for p in raw.split(",") if p.strip()]


def to_toml_array(items: list[str]) -> str:
    if not items:
        return "[]"
    return "[" + ", ".join(f'"{item}"' for item in items) + "]"


def validate_mod_id(value: str):
    if not re.fullmatch(r"[a-z][a-z0-9_]*", value):
        abort(
            f"mod.id must be lowercase letters, digits, and underscores, starting with a letter. Got: '{value}'"
        )


def validate_group(value: str):
    if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*", value):
        abort(
            f"mod.group must be a valid Java package (e.g. com.example). Got: '{value}'"
        )


def validate_environment(value: str):
    if value.lower() not in ["client", "server", "both"]:
        abort("Environment must be 'client', 'server', or 'both'.")


# file helpers


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str):
    path.write_text(content, encoding="utf-8")


def replace_in_file(path: Path, old: str, new: str):
    write(path, read(path).replace(old, new))


def regex_replace_in_file(path: Path, pattern: str, replacement: str):
    write(path, re.sub(pattern, replacement, read(path)))


ROOT = Path(__file__).parent.resolve()
PROPS_FILE = ROOT / "stonecutter.properties.toml"

if not PROPS_FILE.exists():
    abort("Run this script from the root of the stonecutter-mod-template repository.")

if 'mod.id = "modtemplate"' not in read(PROPS_FILE):
    warn("stonecutter.properties.toml no longer contains the default mod.id.")
    confirm = ask(
        "Setup may have already been run. Continue anyway? [y/N]", default="N"
    )
    if confirm.lower() != "y":
        info("Aborted.")
        sys.exit(0)

print(f"""
{CYAN}======================================={RESET}
{CYAN}  Stonecutter Mod Template Setup{RESET}
{CYAN}======================================={RESET}
""")
info("Answer the following questions to configure your mod.")
info("Press Enter to accept the default shown in [brackets].")
print()

mod_id = ask("Mod ID (lowercase letters/digits/underscores)")
validate_mod_id(mod_id)

mod_name = ask("Mod display name")
mod_group = ask("Java group (e.g. com.example)", default="com.example")
validate_group(mod_group)

mod_version = ask("Mod version", default="0.1.0")
channel_tag = ask("Release channel tag (e.g. -alpha.1)", optional=True)
description = ask("Short mod description")

authors = ask_list("Author(s)")
if not authors:
    abort("At least one author is required.")
contributors = ask_list("Contributor(s)", optional=True)

license_name = ask("License name", default="MIT")
license_url = ask("License URL", default="https://opensource.org/licenses/MIT")

sources_url = ask("Source repository URL")
homepage_url = ask("Homepage URL", default=sources_url, optional=True) or sources_url
issues_url = (
    ask("Issues URL", default=f"{sources_url}/issues", optional=True)
    or f"{sources_url}/issues"
)
discord_url = ask("Discord invite URL", optional=True)

modrinth_id = ask("Modrinth project ID", optional=True)
curseforge_id = ask("CurseForge project ID", optional=True)

mod_environment = ask("Environment (client/server/both)", default="both")
validate_environment(mod_environment)

old_pkg = "com/example/modtemplate"
new_pkg = mod_group.replace(".", "/") + f"/{mod_id}"
old_java_pkg = "com.example.modtemplate"
new_java_pkg = f"{mod_group}.{mod_id}"

print()
info("Summary:")
print(f"  mod.id       = {mod_id}")
print(f"  mod.name     = {mod_name}")
print(f"  mod.group    = {mod_group}")
print(f"  mod.version  = {mod_version}{channel_tag}")
print(f"  authors      = {to_toml_array(authors)}")
print(f"  environment  = {mod_environment}")
print(f"  package path = {new_pkg}")
print()
confirm = ask("Proceed? [Y/n]", default="Y")
if confirm.lower() == "n":
    info("Aborted.")
    sys.exit(0)
print()

# stonecutter.properties.toml

info("Updating stonecutter.properties.toml ...")

props_text = read(PROPS_FILE)

match = re.search(r"^\[fabric\]", props_text, re.MULTILINE)
if not match:
    abort("Could not find [fabric] section in stonecutter.properties.toml.")
versions_section = props_text[match.start() :]

pom_devs = "\n".join(
    f'[[mod.pom.developers]]\nid = "{a.lower().replace(" ", "")}"\nname = "{a}"\nurl = "https://github.com/ghost"'
    for a in authors
)

new_props = f"""\
mod.id = "{mod_id}"
mod.name = "{mod_name}"
mod.group = "{mod_group}"
mod.version = "{mod_version}"
mod.channel_tag = "{channel_tag}"
mod.description = "{description}"
mod.authors = {to_toml_array(authors)}
mod.contributors = {to_toml_array(contributors)}
mod.inception_year = "{datetime.now().year}"
mod.license.name = "{license_name}"
mod.license.url = "{license_url}"
mod.license.dist = "repo"
mod.sources_url = "{sources_url}"
mod.homepage_url = "{homepage_url}"
mod.issues_url = "{issues_url}"
mod.discord_url = "{discord_url}"
mod.environment = "{mod_environment}"
{pom_devs}

{versions_section}"""

write(PROPS_FILE, new_props)

# Mixin config file

mixin_old = ROOT / "src/main/resources/modtemplate.mixins.json"
mixin_new = ROOT / f"src/main/resources/{mod_id}.mixins.json"

if mixin_old.exists():
    info("Renaming mixin config ...")
    mixin_old.rename(mixin_new)
    replace_in_file(
        mixin_new, '"com.example.modtemplate.mixin"', f'"{new_java_pkg}.mixin"'
    )

# pack.mcmeta

info("Updating pack.mcmeta ...")
replace_in_file(
    ROOT / "src/main/resources/pack.mcmeta",
    "stonecutter-mod-template Resources",
    f"{mod_name} Resources",
)

# Java source package

old_java_root = ROOT / "src/main/java" / old_pkg
new_java_root = ROOT / "src/main/java" / new_pkg

if old_pkg != new_pkg and old_java_root.exists():
    info(f"Moving Java sources: {old_pkg} -> {new_pkg} ...")
    new_java_root.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(old_java_root, new_java_root)

    for java_file in new_java_root.rglob("*.java"):
        replace_in_file(java_file, old_java_pkg, new_java_pkg)

    shutil.rmtree(ROOT / "src/main/java/com/example/modtemplate")
    for empty_dir in [ROOT / "src/main/java/com/example", ROOT / "src/main/java/com"]:
        if empty_dir.exists() and not any(empty_dir.iterdir()):
            empty_dir.rmdir()

# .env

env_file = ROOT / ".env"
if not env_file.exists():
    info("Creating .env from .env.template ...")
    shutil.copy(ROOT / ".env.template", env_file)
    if modrinth_id:
        regex_replace_in_file(
            env_file,
            r"PUB_MODRINTH_PROJECT_ID=.*",
            f"PUB_MODRINTH_PROJECT_ID={modrinth_id}",
        )
    if curseforge_id:
        regex_replace_in_file(
            env_file,
            r"PUB_CURSEFORGE_PROJECT_ID=.*",
            f"PUB_CURSEFORGE_PROJECT_ID={curseforge_id}",
        )
else:
    warn(".env already exists, skipping.")

# done

print()
success(f"Your mod '{mod_name}' ({mod_id}) is ready.")
print()
info("Next steps:")
print("  1. Replace src/main/resources/assets/icon.png with your mod's icon")
print("  2. Replace .idea/icon.png if using IntelliJ")
print("  3. Run: ./gradlew project")
print(f"  4. Start coding in src/main/java/{new_pkg}/")
print("  5. Delete setup.py from your repository")
print()
