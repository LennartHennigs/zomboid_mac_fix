# zomboid_mac_fix

Fixes the **"I have to click twice"** bug in Project Zomboid on macOS (single clicks
ignored, double clicks needing four).

> Unofficial community fix. Not affiliated with or endorsed by The Indie Stone.
> It contains no game code or assets.

## Quick check: is a window manager the cause?

Before installing anything, quit utilities that watch the mouse system-wide, especially
window managers such as **Magnet**, then start the game and click around.

On my M4 MacBook (macOS 26, Project Zomboid build 42), with Magnet running, **22 of 37
clicks were invisible to the game**. With Magnet quit and nothing else changed, **0 of 10
clicks arrived as instant press/release pairs** and the game saw 9 of 10 (the tenth was a
short click during loading). A Steam user reported the same fix on a Mac Mini M2, see
[below](#reports-about-project-zomboid).

If quitting your window manager solves it, you can stop there. If you need it running, or
the problem persists, use the fix below. It makes the game tolerate late, batched mouse
events whatever the cause.

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

## The problem

On macOS, Project Zomboid ignores a large share of mouse clicks. Typical symptoms:

- You click a button or inventory item and **nothing happens**. You click again and it works.
- A double click only registers if you do it **four times**.
- It happens in the menus and in-game, with the built-in trackpad and with external mice.
- It was reported as new in build 42 (it did not happen in 41).
- Light or quick taps seem to fail more often than firm, slow presses.

It is easy to mistake for a sluggish UI or a hardware problem. The input device is fine:
the click does reach the game, but the game then throws it away.

### Why it happens

The game does not react to click events. It **polls** the button state: once per game
tick it asks GLFW "is the left button down right now?" (`glfwGetMouseButton`) and builds
its "pressed" and "released" edges by comparing that answer with the previous tick. The
press/release events that GLFW also delivers, through the mouse-button callback, are put
in a queue that nothing uses for clicks.

On macOS those events can reach the game in **batches** (why exactly is unclear, see below):
the main thread only collects them occasionally. A short click (typically 30-130 ms) therefore arrives as a press **and**
its release in the same instant. When the game polls right afterwards the button is
already up, so as far as the game is concerned the click never happened. The next click
usually arrives as a separate event and gets through, which is why it feels like you have
to click twice.

Measured on an M4 MacBook in the main menu, without the fix and with Magnet running, the
log of the callbacks shows this pattern over and over:

```
PRESS   t
RELEASE t   held=0.2ms      <- press and release delivered together, never seen by the game
```

**22 of 37 clicks (about 60%) were invisible to the game.** With the fix, all of them are seen.

### Same symptom in other Mac games

This is not unique to Project Zomboid. Players report exactly this ("I have to double
click on everything") in other games on macOS, on both Apple Silicon and Intel Macs, with
trackpads and mice:

- [Have to double click on everything [Mac M1]](https://forum.paradoxplaza.com/forum/threads/have-to-double-click-on-everything-mac-m1.1509533/)
  (Paradox forums: Cities: Skylines, Stellaris and Roguebook players, 2022-2023). The
  workarounds mentioned there: toggling *Tap to click* off and on in the trackpad
  settings, tabbing out of the game and clicking another window, unplugging headphones
  from the jack, using a different USB mouse, or not touching the mouse while the game
  starts. These are **not tested with Project Zomboid** and none of them is a permanent
  fix, but they may help if you do not want to use the agent.

That the symptom shows up across unrelated engines suggests that macOS sometimes hands
mouse events to the game late and in bursts. Most games survive that. Project Zomboid does
not, because it only looks at the button state once per tick.

### Reports about Project Zomboid

I could not find an existing write-up of this exact cause for PZ, but there are related
reports on the Steam forums:

- [Double-click bug report, v42.20 (Mac Mini M2)](https://steamcommunity.com/app/108600/discussions/6/588433527532573235/):
  every menu and in-game click needed a double click. The reporter says it did not happen
  in v41, and found that **quitting the Magnet window-manager app fixed it**. Utilities
  that watch the mouse system-wide (window managers, mouse remappers, screen tools) can
  delay the events a game receives, which fits the batching described above. If you run
  one, try quitting it before blaming the game.

- [Help! I have a mac mouse and cannot fight](https://steamcommunity.com/app/108600/discussions/0/3362406825530524234/):
  "PZ doesn't reliably sense a light tap on the trackpad; you need to use a firm press to
  get the click to register." This matches the behaviour described here.

Separate Mac issues that are **not** what this fixes: the game not accepting clicks at
all or having a misaligned cursor in fullscreen on M1 Macs (usually worked around with
`fullscreen=false` in `~/Zomboid/options.ini`), for example
[Can't even accept terms to start](https://steamcommunity.com/app/108600/discussions/0/3784750482946464430)
and
[Zomboid wont go past acknowledgment page on MacOS Sonoma](https://steamcommunity.com/app/108600/discussions/1/4522260786596649817).
If your clicks land in the wrong place, that is a different bug. If they land correctly
but only every other one works, this is the one.

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
