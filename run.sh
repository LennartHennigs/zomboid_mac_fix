#!/bin/bash
# Launch Project Zomboid (macOS) with the click-latch agent.
# Usage: ./run.sh [fix|log|fix,log|off]   (default: fix,log)
#   log     = record callbacks vs. sampled state to ~/pz-mouse-fix.log, no behaviour change
#   fix     = latch short clicks so the game's per-tick polling can't miss them
# Steam must be running.
MODE="${1:-fix,log}"
HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$HOME/Library/Application Support/Steam/steamapps/common/ProjectZomboid/Project Zomboid.app/Contents"
case "$(uname -m)" in arm64) JRE=jre-aarch64;; *) JRE=jre-x86_64;; esac
AGENT=()
[ "$MODE" != off ] && AGENT=("-javaagent:$HERE/pz-mouse-fix.jar=$MODE")
cd "$APP/Java" || exit 1
exec "$APP/PlugIns/$JRE/Contents/Home/bin/java" \
  "${AGENT[@]}" \
  -XstartOnFirstThread -Djava.awt.headless=true \
  --enable-native-access=ALL-UNNAMED --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
  -Dzomboid.steam=1 -Dzomboid.znetlog=1 -Xmx3072m -XX:+UseZGC -XX:-OmitStackTraceInFastThrow \
  -Djava.library.path="$APP/Java:$APP/MacOS" -Duser.dir="$APP/Java" \
  -cp "$APP/Java/projectzomboid.jar" zombie.gameStates.MainScreenState
