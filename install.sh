#!/bin/bash
# Installs the click-latch agent into the Project Zomboid game folder.
#   ./install.sh [mode]     mode: fix (default) | fix,log
#   ./install.sh --uninstall
# After installing, start the game normally from Steam. Re-run after a game update
# (Steam restores Info.plist on update). Quit the game first.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
CONTENTS="${PZ_CONTENTS:-$HOME/Library/Application Support/Steam/steamapps/common/ProjectZomboid/Project Zomboid.app/Contents}"
PLIST="$CONTENTS/Info.plist"
JAR_DST="$CONTENTS/Java/pz-mouse-fix.jar"
BAK="$PLIST.pz-mouse-fix.bak"
PB=/usr/libexec/PlistBuddy

[ -f "$PLIST" ] || { echo "Game not found at: $CONTENTS" >&2; exit 1; }
if pgrep -f zombie.gameStates.MainScreenState >/dev/null; then echo "Quit Project Zomboid first." >&2; exit 1; fi

# remove any previous agent entry (idempotent)
remove_entry() {
  local i=0
  while v=$($PB -c "Print :JVMOptions:$i" "$PLIST" 2>/dev/null); do
    case "$v" in *pz-mouse-fix.jar*) $PB -c "Delete :JVMOptions:$i" "$PLIST"; continue;; esac
    i=$((i+1))
  done
}

if [ "${1:-}" = "--uninstall" ]; then
  remove_entry; rm -f "$JAR_DST" "$BAK"; echo "Uninstalled."; exit 0
fi

MODE="${1:-fix}"
[ -f "$BAK" ] || cp "$PLIST" "$BAK"
cp "$HERE/pz-mouse-fix.jar" "$JAR_DST"
remove_entry
$PB -c "Add :JVMOptions: string -javaagent:\$APP_ROOT/Contents/Java/pz-mouse-fix.jar=$MODE" "$PLIST"
echo "Installed (mode=$MODE). Start the game from Steam as usual."
echo "Backup of original Info.plist: $BAK"
