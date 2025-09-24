#!/usr/bin/env python3

import os
import sys
from typing import Callable
import re
import shutil

PACKAGE_PART_PATTERN = re.compile(r'^[a-z_][a-z0-9_]*$')


def require_input(prompt: str, default_value: str = None, validator: Callable[[str], tuple[bool, str]] = None) -> str:
    while True:
        value = input(prompt).strip()
        if default_value and not value:
            value = default_value
        if validator is not None:
            result, error = validator(value)
            if result:
                return value
            print(error, file=sys.stderr)
        elif value:
            return value
        print("Value cannot be blank.", file=sys.stderr)


def mod_id_validator(value: str) -> tuple[bool, str]:
    if value.isascii() and all(c.islower() or c.isdigit() or c == '_' for c in value):
        return True, ''
    return False, 'Only a-z, 0-9, and _ are allowed. (Unlike Fabric, Neoforge does not allow \'-\')'


def confirm_validator(value: str) -> tuple[bool, str]:
    if len(value) == 1 and value in 'YyNn':
        return True, ''
    else:
        return False, "Only 'Y','N','y' and 'n' are allowed."


def package_name_validator(value: str) -> tuple[bool, str]:
    if not value:
        return False, "Package name cannot be empty."
    parts = value.strip().split('.')
    for part in parts:
        if not PACKAGE_PART_PATTERN.fullmatch(part):
            return False, (
                f"Invalid package part: '{part}' — must start with a letter or underscore and contain only a-z, 0-9, or _")
    return True, ''


def remove_sample_content(file_name, value = "sample_content"):
    with open(file_name, 'r') as f:
        lines = f.readlines()
    with open(file_name, 'w') as f:
        for line in lines:
            if value.lower() not in line.lower():
                f.write(line)
        f.write('\n')


class MassReplace:
    @staticmethod
    def __should_exclude(path: str) -> bool:
        path = os.path.normpath(path)
        parts = path.split(os.sep)
        for part in parts:
            if part.startswith('.'):
                return True
            if part in {'build', '.gradle'}:
                return True
        if os.path.isdir(path) and os.path.basename(path) == 'gradle':
            return True
        return False

    @staticmethod
    def _replace_in_file(filepath: str, search: str, replace: str):
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
        except UnicodeDecodeError:
            return
        if search not in content:
            return
        new_content = content.replace(search, replace)
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)

    @staticmethod
    def replace(root: str, search: str, replace: str):
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if not MassReplace.__should_exclude(os.path.join(dirpath, d))]
            for filename in filenames:
                if filename in {'README.md', os.path.basename(__file__)}:
                    continue
                full_path = os.path.join(dirpath, filename)
                if MassReplace.__should_exclude(full_path):
                    continue
                MassReplace._replace_in_file(full_path, search, replace)


def move_package_group(root: str, old_group: str, new_group: str):
    if old_group == new_group:
        print(f"No group change needed: {old_group}")
        return
    old_path = os.path.join(root, *old_group.split('.'))
    new_path = os.path.join(root, *new_group.split('.'))
    if not os.path.exists(old_path):
        print(f"Old group path does not exist: {old_path}")
        return
    os.makedirs(new_path, exist_ok=True)
    print(f"Moving contents from {old_path} -> {new_path}")
    for dirpath, dirnames, filenames in os.walk(old_path, topdown=False):
        rel_path = os.path.relpath(dirpath, old_path)
        target_dir = os.path.join(new_path, rel_path)
        os.makedirs(target_dir, exist_ok=True)
        for filename in filenames:
            src_file = os.path.join(dirpath, filename)
            dst_file = os.path.join(target_dir, filename)
            if os.path.exists(dst_file):
                print(f"Skipping existing file: {dst_file}")
                continue
            shutil.move(src_file, dst_file)
    print(f"Removing old group directory: {old_path}")
    shutil.rmtree(old_path)
    top_level = os.path.join(root, 'dev')
    if os.path.isdir(top_level) and not os.listdir(top_level):
        print(f"Removing now-empty directory: {top_level}")
        os.rmdir(top_level)


def rename_modtemplate_dirs(root: str, new_mod_id: str):
    if new_mod_id == 'modtemplate':
        return
    for dirpath, dirnames, _ in os.walk(root, topdown=False):
        for dirname in dirnames:
            if dirname == 'modtemplate':
                src = os.path.join(dirpath, dirname)
                dst = os.path.join(dirpath, new_mod_id)
                print(f"Renaming {src} -> {dst}")
                os.rename(src, dst)


print("Rotgruengelb's Stonecutter Mod Template Scaffolder")
print("Note: This script assumes certain files have not been modified. "
      "Running this on a fresh, unmodified template is recommended.")

mod_name = require_input("Mod Name: ")

mod_id = ''.join(mod_name.lower().split())
mod_id = require_input(f"Mod ID [{mod_id}]: ", mod_id, mod_id_validator)

print("This should be a valid Java class name. I do not validate this.")
mod_class = input("Mod Class [ModTemplate]: ").strip() or 'ModTemplate'

print("The mod group should also be updated. If you are not rotgruengelb, please change this.")
print("Note: Be careful not to include reserved keywords! They are not validated!")
mod_group = require_input(f"Mod Group: [net.rotgruneglb]: ", 'net.rotgruneglb', package_name_validator)

print("Who is you? One entry only! More Authors can be added later in accordance to the loaders metadata format. Be careful with quotes and escaping.")
mod_author = input("Mod Author []: ").strip()

print("The mod description. A short (1-3 sentence) decription of your mod. Not validated. Be careful with quotes and escaping.")
mod_description = input("Mod Description []: ").strip()

print("This template includes some sample content for testing/example purposes, including:")
print("    - Random AW/AT")
print("    - A test mixin")
print("    - Datagen Lava Chicken Recipe")
remove_samples = require_input("Remove this sample content? (Y/n) [Y]: ", 'Y', confirm_validator).lower() == 'y'

delete_script = require_input("Delete myself when done? (Y/n) [Y]: ", 'Y', confirm_validator).lower() == 'y'

print(f'Mod Name:        {mod_name}\n'
      f'Mod ID:          {mod_id}\n'
      f'Mod Class:       {mod_class}\n'
      f'Mod Group:       {mod_group}\n'
      f'Mod Author:      {mod_author}\n'
      f'Mod Description: {mod_description}\n'
      f'Remove Samples:  {remove_samples}\n'
      f'Delete Script:   {delete_script}')
confirm_settings = require_input("Proceed with these settings? (Y/n) [Y]: ", 'Y', confirm_validator).lower() == 'y'
if not confirm_settings:
    print('Quitting.')
    sys.exit(0)

if remove_samples:
    print("Removing samples...")
    # Mixins
    os.remove('src/main/java/com/example/modtemplate/mixin/ExampleMixin.java')
    remove_sample_content('src/main/resources/modtemplate.mixins.json', 'ExampleMixin')
    # Datagen
    shutil.rmtree('src/main/generated', ignore_errors=True)
    os.remove('src/main/java/com/example/modtemplate/fabric/datagen/ModRecipeProvider.java')
    remove_sample_content('src/main/java/com/example/modtemplate/fabric/datagen/FabricDataGeneratorEntrypoint.java',
              'sample_content')
    # Event/AWs/ATs
    os.remove('src/main/java/com/example/modtemplate/ExampleEventHandler.java')
    remove_sample_content('src/main/java/com/example/modtemplate/fabric/FabricEntrypoint.java', 'sample_content')
    remove_sample_content('src/main/java/com/example/modtemplate/neoforge/NeoforgeEntrypoint.java', 'sample_content')
    remove_sample_content('src/main/resources/modtemplate.accesswidener', 'sample_content')
    remove_sample_content('src/main/resources/META-INF/accesstransformer.cfg', 'sample_content')

if mod_class != 'ModTemplate':
    print('Renaming Mod Class File...')
    os.rename('src/main/java/com/example/modtemplate/ModTemplate.java',
              f'src/main/java/com/example/modtemplate/{mod_class}.java')

print('Renaming Resource Files...')
os.rename('src/main/resources/modtemplate.accesswidener', f'src/main/resources/{mod_id}.accesswidener')
os.rename('src/main/resources/modtemplate.mixins.json', f'src/main/resources/{mod_id}.mixins.json')

print('Performing mass search and replace...')
MassReplace.replace('.', 'ModTemplate', mod_class)
MassReplace.replace('.', 'Mod Template', mod_name)
MassReplace.replace('.', 'modtemplate', mod_id)
MassReplace.replace('.', 'com.example', mod_group)
MassReplace.replace('.', 'A Mod Author', mod_author)
MassReplace.replace('.', 'A Mod Description', mod_description)

print('Renaming directories/packages')
move_package_group('src/main/java/', 'com.example', mod_group)
rename_modtemplate_dirs('src/main/java/', mod_id)

print("All done!")

if delete_script:
    print("Deleting myself. It was nice knowing you... (╥﹏╥)")
    os.remove(__file__)
