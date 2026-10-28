# EpicLite — souls-like combat for Paper 26.2

A Paper plugin plus a tiny server resource pack that give vanilla clients an Epic Fight-style combat feel.
Players need **no client mod**; they only accept the server resource pack.

> **Status: untested.** This was written without a compiler or a Paper server available, so it has never been built or run.
> Expect to fix a few compile errors or API-name changes on the first build (see "First build" below).

## What it does

| Feature | How it works |
|---|---|
| **Stamina** | Own stamina bar (action bar), independent of hunger. Attacks, dodges and skills spend it. Out of stamina = weak hits and a broken combo. |
| **Weapon types** | Sword, axe, spear/trident, mace, detected by material name. |
| **4-hit combos** | Hit again within the window to advance the chain; the 4th hit is a finisher with bonus damage and knockback. |
| **Dodge** | Double-tap sneak. Dashes in the direction you were moving (backstep if standing still) with brief i-frames. |
| **Weapon skills** | Press the swap-hand key (F) while holding a weapon: Crescent Slash (sword), Earthsplitter (axe), Lunge (spear/trident), Seismic Slam (mace). Each has a cooldown and stamina cost. |
| **Animations** | Each hit plays a glowing slash arc (left, right, overhead, wide finisher) as an `ItemDisplay` using a model from the resource pack, interpolated client-side, plus arm swing, particles and sounds. Other players see it too. |

Skill damage goes through the normal damage event pipeline, so claim and PvP-protection plugins can still veto it.

## Install

1. Build the plugin (below) and drop `EpicLite-0.1.0.jar` into `plugins/`.
2. Host `EpicLite-ResourcePack.zip` somewhere with a direct download link, then set in `server.properties`:
   ```
   resource-pack=https://your.host/EpicLite-ResourcePack.zip
   resource-pack-sha1=<sha1 of the zip>
   require-resource-pack=true
   ```
   (`sha1sum EpicLite-ResourcePack.zip`)
3. Start the server. Tune everything in `plugins/EpicLite/config.yml`; `/eflite reload` applies changes.

Requires Paper **26.2** and **Java 25**.

## First build

```
cd plugin
gradle build        # or add the Gradle wrapper first: gradle wrapper
```

Output: `plugin/build/libs/EpicLite-0.1.0.jar`.

Things most likely to need a tweak:

- **Paper dependency string** in `plugin/build.gradle.kts`. It is set to `26.2.build.+`. Check the exact build string on
  https://papermc.io/downloads/paper (26.x uses `26.2.build.N-<channel>` rather than the old `1.x-R0.1-SNAPSHOT`) and pin it.
- **Enum/constant names** such as `Particle.EXPLOSION`, `Particle.BLOCK`, `Attribute.ATTACK_DAMAGE`; Paper 26.2 also ships Adventure 5.
- **Resource pack `pack.mcmeta`** uses `min_format [88,0]` (the 26.2 format). Adjust if your client reports a mismatch.

## Tuning the slash animation

The arc is a flat crescent model rotated around the player. If it appears sideways, mirrored or offset on your setup, adjust
`animation.yaw-offset` in `config.yml` (try `90`, `180`, `-90`). The texture is `assets/eflite/textures/item/slash_arc.png`
and is easy to replace with your own art.

## Limits (by design)

- Animations are **effects around the player**, not a re-posed player body. The real arm swing is vanilla; there is no first-person animation,
  lock-on camera, or custom HUD. Doing that needs a client mod (Epic Fight itself).
- Hitboxes are vanilla.
- Not Folia-compatible (uses the normal Bukkit scheduler).
- For true skeletal animations on the player model, swap `AnimationManager.slash()` for calls into a model library
  (BetterModel / ModelEngine) and author the animations in Blockbench.

## Licensing

All code and the slash texture here are original; nothing was copied from Epic Fight. Epic Fight's code is GPL-3.0 and its **assets are
All Rights Reserved**, so its animations, models, textures and sounds must not be reused here without the authors' written permission.
