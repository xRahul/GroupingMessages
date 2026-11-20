#!/usr/bin/env python3
"""Pin the next release into the F-Droid recipe.

F-Droid needs the exact version name/code and commit for each build. Keeping
that in the recipe means a store build never depends on git metadata.
"""
import os
import re
import sys


def main():
    if len(sys.argv) != 4:
        print("Usage: update_fdroid_metadata.py <version> <version_code> <tag>")
        return 1

    version, version_code, tag = sys.argv[1:4]
    path = "metadata/in.rahulja.groupingmessages.yml"
    if not os.path.exists(path):
        print(f"Notice: {path} not found; skipping")
        return 0

    with open(path, encoding="utf-8") as stream:
        content = stream.read()

    content = re.sub(r"CurrentVersion:\s*.+", f"CurrentVersion: {version}", content)
    content = re.sub(r"CurrentVersionCode:\s*.+", f"CurrentVersionCode: {version_code}",
                     content)

    build = f"""Builds:
  - versionName: {version}
    versionCode: {version_code}
    commit: {tag}
    gradle:
      - yes
      - -PversionCode={version_code}
      - -PversionName={version}"""

    content = re.sub(r"Builds:[\s\S]*?\n\nAutoUpdateMode:", build + "\n\nAutoUpdateMode:",
                     content)
    with open(path, "w", encoding="utf-8") as stream:
        stream.write(content)
    print(f"Updated {path}: {version} ({version_code}) at {tag}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
