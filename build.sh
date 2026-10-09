#!/bin/bash
# Rebuilds pz-mouse-fix.jar. Needs a JDK 25+ (the game's classes are Java 25).
set -euo pipefail
cd "$(dirname "$0")"
GAME="${PZ_CONTENTS:-$HOME/Library/Application Support/Steam/steamapps/common/ProjectZomboid/Project Zomboid.app/Contents}"
rm -rf build && mkdir build
javac --release 25 -d build -cp "$GAME/Java/projectzomboid.jar" src/pzmousefix/*.java
jar cfm pz-mouse-fix.jar manifest.txt -C build .
rm -rf build
echo "Built pz-mouse-fix.jar"
