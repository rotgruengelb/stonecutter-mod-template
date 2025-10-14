#!/usr/bin/env bash

PROTECTED_FILES=(".sc_active_version")
STAGED=$(git diff --cached --name-only)

for file in "${PROTECTED_FILES[@]}"; do
	if echo "$STAGED" | grep -q "^$file$"; then
		echo "‼️ Attempted to modify a protected file: $file"
		echo "‼️ .sc_active_version is managed by Stonecutter."
		echo "‼️ The Stonecutter active version should always be reset to match the VSC version!"
		echo "‼️ If this change is intentional, use: git commit --no-verify"
		exit 1
	fi
done

exit 0
