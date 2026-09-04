# Bloodlines — Changelog

A running log of what's actually been built, for anyone (or any AI) catching up on this project without
digging through commit history. For deeper technical/architecture notes — how things are built, known
issues, build instructions — see `CLAUDE.md` instead; this file just tracks *what happened and when*.

Newest first.

## v1.1.3 — 2026-09-03: race roster overhaul

- **Renamed, merged, and cut races**: Human → Demi-Human. Wood Elf, High Elf, and Moon Elf merged into one
  combined Elf race (existing players on any of the three old races migrate automatically, no
  re-selection needed). Ghoul removed entirely (redundant with Revenant). Troll's display name changed to
  Ogre (purely cosmetic — the internal race id is unchanged). Goblin renamed to Shadowkin, to stop
  clashing with an unrelated mod's goblin trader NPCs, and given two new stealth passives: Shadow Step
  (near-full movement speed while sneaking, instead of vanilla's crawl) and Unseen (hostile mobs can't
  freshly target you while sneaking, past melee range).
- **Split every race's active ability into a primary and secondary slot**, each with its own cooldown,
  duration, and HUD bar. Primary abilities stay the existing buff-style effects; secondary is for flashier,
  instant abilities. Elf is the first race with both: Elven Ward (regeneration + a shield) as primary,
  Stormcall (a real lightning strike aimed wherever you're looking) as secondary.
- **Added a weapon affinity system** — a damage bonus for using the "right" weapon for your race's theme:
  axes for Dwarf and Ogre, swords for Angelkin/Demonkin/Revenant, tridents for Merfolk, bare fists for
  Beastkin, crossbows for Fae. Elf gets a matching bonus on thrown potions instead (they last 25% longer).
- **Demonkin** can now see clearly and move at normal speed through lava, instead of vanilla's blinding red
  fog and thick-mud crawl.
- **Beastkin** now scares creepers away outright (no explosion risk from an idle skeleton fight gone wrong).
- **Merfolk** no longer slowly sinks to the seafloor while idle in the water — matches the "at home in the
  depths" theme rather than physically behaving like a landlubber who forgot to swim.
- **Fixed a server crash on login**, caused by a player's racial effects being reapplied synchronously
  before the surrounding world had finished generating.
- **Fixed innate racial effects** (night vision, fire resistance, etc.) silently expiring after roughly 14
  hours instead of being genuinely permanent, as intended.
- **Fixed a real bug**, not just a rename: Elf's primary ability was still internally named "Stormcall"
  from before the primary/secondary split existed, so activating the primary buff showed the wrong ability
  name in chat and on the HUD.
- Balance passes: Angelkin's mining penalty (was accidentally twice as harsh as every other race, fixed,
  knockback resistance trimmed slightly to compensate), Beastkin's jump height, Fae/Angelkin stat tuning.

## v1.1.1 — 2026-08-28: remote config + orb rebrand

- Server ops can now edit the mod's config from a remote server, not just the singleplayer host.
- Added a rarity multiplier setting for Orb of Bloodlines spawns, and rebranded the orb's name/flavor.

## v1.1.0 — 2026-08-26: flight fixes and the config menu

- Fixed Fae losing their flight ability on respawn.
- Added an in-game config screen, reachable from the normal Forge mods list.
- Gave the flying races (Fae, Angelkin, Demonkin) a configurable per-race maximum armor weight, since
  flight wasn't meant to be free with heavy armor equipped.

## v1.0.0 and earlier — 2026-08-21 to 2026-08-24: first working version

- Initial playable version of the mod — the full original race roster, stats, and abilities.
- Fae's wings and flight went through many iterations before settling: a custom jointed wing model, several
  art passes, animation tuning, and eventually hunger-gated native flight instead of a stamina resource.
- Angelkin's (internally still "Seraph") wings and flight went through a similar back-and-forth, eventually
  landing on vanilla's own ElytraLayer rendering with Icarus-style flight physics.
- Added Merfolk and Beastkin cosmetics (tails, ears), fixed a bug losing race data on death, first overall
  balance pass across every race.

---

Nothing has shipped to players outside of direct jar hand-offs yet — there's no CurseForge listing live.
See `CLAUDE.md` for the current CurseForge-listing plan and everything else about how this mod is built.
