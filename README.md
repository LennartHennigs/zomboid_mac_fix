# zomboid_mac_fix

Fixes the **"I have to click twice"** bug in Project Zomboid on macOS (single clicks
ignored, double clicks needing four).

> Unofficial community fix. Not affiliated with or endorsed by The Indie Stone.
> It contains no game code or assets.

## Install

1. Quit Project Zomboid.
2. In Terminal:
   ```sh
   git clone https://github.com/LennartHennigs/zomboid_mac_fix.git
   cd zomboid_mac_fix
   ./install.sh
   ```
3. Start the game from Steam as usual.

**Game updates undo the fix** (Steam restores `Info.plist`). Just run `./install.sh` again.

Uninstall: `./install.sh --uninstall`. Your original `Info.plist` is also kept as
`Info.plist.pz-mouse-fix.bak` in the game folder.

Tested on Apple Silicon (M4), macOS 26, Project Zomboid build 42 (bundled Zulu JRE 25).
If the game or Steam is installed somewhere non-standard, set `PZ_CONTENTS` to the app's
`Contents` folder before running `install.sh`.

## What was wrong

The game reads mouse buttons by *polling* `glfwGetMouseButton` once per game tick, and
turns the result into "pressed/released" edges. It ignores the press/release events that
GLFW delivers through its callback.

On macOS those events reach the game in batches. A short click then arrives as a
press **and** its release in the same instant, so by the time the game polls, the button
is already up and the click never existed. The next click arrives normally, which is why
it feels like you must click twice.

Measured on an M4 MacBook in the main menu: **22 of 37 clicks (about 60%) were invisible
to the game.**

## What the fix does

A tiny Java agent (`-javaagent`) patches two classes in memory at startup:

- `org.lwjglx.input.Mouse.addButtonEvent`: also records "this button was pressed".
- `zombie.input.MouseState.poll`: reports a button as down if it was pressed since the
  last poll, even if it has already been released.

Nothing in the game's files is modified except one extra line in `Info.plist`:
`-javaagent:$APP_ROOT/Contents/Java/pz-mouse-fix.jar=fix`.
If a future game update renames those classes, the agent prints a warning and the game
simply starts unpatched.

Limitation: two clicks inside a single game tick still count as one.

## Options

- `./install.sh fix,log` also writes every press/release and what the game saw to
  `~/pz-mouse-fix.log`. Useful for diagnosing or for bug reports.
- `./run.sh [fix|log|fix,log|off]` starts the game directly with the agent, without
  installing anything.
- `./build.sh` rebuilds `pz-mouse-fix.jar` from `src/` (needs JDK 25+ and a game install).

## For the developers

A proper fix inside the game is a few lines: latch presses in the GLFW mouse-button
callback and OR them into the polled state (or drive button edges from the event queue,
which is already filled but unused for clicks).

## License

[MIT](LICENSE)
