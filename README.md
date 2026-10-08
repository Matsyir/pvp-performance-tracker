# PvP Performance Tracker (v1.8.9)
[![Active Installs](http://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/installs/plugin/pvp-performance-tracker)](https://runelite.net/plugin-hub/Matsyir) [![Plugin Rank](http://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/rank/plugin/pvp-performance-tracker)](https://runelite.net/plugin-hub)

[_View patch notes & version history_](https://github.com/Matsyir/pvp-performance-tracker/wiki#version-history--patch-notes)

Tracks PvP performance by keeping track of various stats. Mostly useful for 1v1 PvP fights that involve overhead prayers and/or gear switching, Last Man Standing & PvP arena "NHing" being the perfect examples. Multi will cause problems. **Potentially inaccurate:** there are some imperfections and assumptions in this plugin, but it is generally accurate for most popular gear setups & spec weapons. 

We now have a discord to discuss the PvP Performance Tracker - feel free to join here: https://discord.gg/hg26xeJnY5

This plugin is a bit of a community project at this point. Special thanks to some of the most notable/frequent contributors & helpers:
- [Mazhar](https://twitter.com/maz_rs) for collaborating with me on this plugin during its initial release. He's inspired many of the ideas and helped with a lot of the implementation - expected damage was mostly done by him, and that is definitely the most informative statistic of the plugin.
- [LogicalSolutions](https://github.com/LogicalSoIutions) for creating & hosting the entirety of the PvP-Hub, fixing the Fight Analysis/Merge process to be used in PvP-Hub, sharing good feedback, helping to test new features, and quickly submitting various fixes.
- [Sacca](https://github.com/Sacca-1) for implementing opponent HP tracking, KO chance statistic, Hits on Robes statistic, adding support for tons of new gear & spec weapons, and also submitting various fixes.
- Technically not contributors, but, [Pan1c 07](https://www.youtube.com/@Pan1c07) & [Lagunarium](https://www.youtube.com/@lagunarium) for frequently providing me with very detailed & valuable insights into top-1% PvPer perspectives & concerns, as well as greatly aiding in testing/improving new features.

[All other contributors](https://github.com/Matsyir/pvp-performance-tracker/graphs/contributors) are greatly appreciated as well! No matter how big or small your contribution, this plugin wouldn't be where it is today without everyone's help.

# Details have been moved to [the wiki](https://github.com/Matsyir/pvp-performance-tracker/wiki#pvp-performance-tracker-wiki)

**UI Overview:** (from 1.5.0)

![Plugin Overview Image](https://i.imgur.com/LkQGda3.png)

# PvP-Hub

Generously created & hosted by [LogicalSolutions](https://github.com/LogicalSoIutions), and introduced in 1.7.4, this
new opt-in feature automatically uploads all of your fights to the [PvP-Hub website](https://osrs.pvp-hub.com), where it can be publicly viewed by
anyone. This is disabled by default - the plugin remains entirely client-side if you do not manually opt-into this
feature, and there will be a warning popup when you attempt to do so.

## Why does this PvP-Hub setting say my IP is being requested?

To use this feature, the plugin needs to send some fight data to a server:

https://osrs.pvp-hub.com

Like any website or online service, that server is reached through an IP address. This is simply how devices communicate over the internet.

When the plugin sends data to the server, the server can technically see the IP address the request came from. This is normal for any internet request. However, your IP address is not stored or logged. The server receives the request, processes it, and does not save your IP.

The website source code is available here:

[View the source code on GitHub](https://github.com/LogicalSoIutions/osrs-pvp-performance-tracker-website)

This feature is completely optional.

- The setting is disabled by default.
- If the checkbox is disabled, no data is sent to the server. If it is enabled, data is only sent to the server when you
  finish a fight. This data includes your fight data, IP, and a uniquely generated fight ID. It includes your RSN unless
  you also enable the "Hide RSN on PvP-Hub" option.
- If you enable it, and your opponent also has it enabled, the website can automatically use Advanced Analysis.
- Advanced Analysis lets you view the full fight, including more accurate calculations, extra details such as ammo,
  rings used, and more.
- If your opponent does not have the plugin, or has this setting disabled, the plugin will use your existing gear settings instead.

## Hide RSN on PvP-Hub

If "Hide RSN on PvP-Hub" is enabled, the plugin replaces your RSN in PvP-Hub uploads with a generated name like
`Hidden-ABCDE`.

This is not based on your device, hardware, Windows username, or RuneScape account. The plugin creates a random ID once,
stores it locally in your RuneLite profile config, and derives the `Hidden-XXXXX` name from that ID. The hidden name is
stable so your uploaded fights can still be grouped under the same hidden identity.

Your hidden name is shown in the PvP Performance Tracker side panel while this setting is enabled. If you want to keep
that hidden identity private, do not show the panel on stream, screenshots, or screen share.

To change your hidden name, reset the stored anonymous ID. You can do this by right-clicking the Total Stats and clicking the "Regenerate PvP-Hub Hidden Name" button. You can find the same right-click menu on the button which displays your hidden name.

-------------------------------
I am happy to see other features/stats come into this plugin in the future, feel free to submit issues/suggestions & PRs. If you find a weapon that doesn't work, let me know as well. If you have any problems or questions that don't warrant a whole issue, feel free to join the dedicated PvP Performance Tracker discord (https://discord.gg/hg26xeJnY5), or just DM me: `matsyir` (don't add, just DM - if you need a common server to DM, you can join the official Runelite discord, or the tracker discord linked above).

Note that I'm not super active on RS lately myself, so this project is not among my highest priorities - but I'm happy to keep supporting it, especially for significant issues that may affect most average users with average gear setups in places like LMS.

## Pete Kayer tracking

Pete Kayer fights are tracked only in region **11100**, plane **0** (the arena containing
world tile **2783, 5920, 0**). Instance coordinates are resolved to their template region.
Existing player PvP tracking remains available under its usual settings; Pete support also
works with the LMS restriction enabled.

All 18 attackable Pete forms in the supplied October 7, 2026 cache report are supported:
16579–16586 and 16588–16597. Changing NPC IDs on the same actor keeps the current fight
and updates the combat bonuses used for the next attack. Lobby and dialogue forms are excluded.
Each attack log stores Pete's NPC ID for its setup.

Pete's levels stay fixed at Attack 118, Strength 118, Defence 120, Ranged 112, Magic 99,
and Hitpoints 115. The NPC cache orders these as attack, defence, strength, hitpoints,
ranged, magic ([RuneLite NPCComposition](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/NPCComposition.java)).
His ring is assumed to be Lightbearer. Equipment reference items for all 18 fighting
forms are mapped from the matching October 7 cache's worn models and face colors.
See [cache evidence and mapping tables](../docs/pete-cache/README.md).

| NPC ID | Weapon | Top | Bottom | Shield |
| --- | --- | --- | --- | --- |
| 16579 | Staff of the dead | Virtus | Virtus | Elidinis' ward (f) |
| 16580 | Voidwaker | Masori (f) | Masori (f) | Dragonfire shield |
| 16581 | Noxious halberd | Masori (f) | Masori (f) | None |
| 16582 | Zaryte crossbow | Masori (f) | Masori (f) | Dragonfire shield |
| 16583 | Staff of the dead mesh, different colors | Masori (f) | Masori (f) | Dragonfire shield |
| 16584 | Voidwaker | Masori (f) | Virtus | Dragonfire shield |
| 16585 | Zaryte crossbow | Virtus | Masori (f) | Dragonfire shield |
| 16586 | Staff of the dead | Virtus | Masori (f) | Elidinis' ward (f) |
| 16588 / 16597 | Staff of the dead | Mystic | Mystic | Blessed spirit shield |
| 16589 | Dragon crossbow | Karil's | Verac's mesh | Blessed spirit shield |
| 16590 | Abyssal tentacle | Karil's | Verac's mesh | Dragon defender |
| 16591 | Dragon claws | Karil's | Verac's mesh | None |
| 16592 | Staff of the dead | Karil's mesh, different colors | Mystic | Blessed spirit shield |
| 16593 | Staff of the dead | Karil's | Verac's mesh | Dragon defender |
| 16594 | Abyssal tentacle | Karil's | Verac's mesh | Blessed spirit shield |
| 16595 | Dragon crossbow | Karil's | Verac's mesh | Dragon defender |
| 16596 | Staff of the dead | Karil's | Verac's mesh | Blessed spirit shield |

16579–16586 use Torva helm, **regular fury**, Barrows gloves, an imbued ancient cape,
and upgraded Avernic treads. Max treads remain the user's kit assumption: upgrades
share the same appearance. 16588–16597 use Neitiznot helm, regular fury, Barrows gloves,
dragon boots and the imbued Saradomin cape mesh. Reference item variants do not establish
charge, degradation, hidden inventory or NPC-specific effects. Some meshes have different
NPC colors, documented in the cache evidence.

Only the ZCB forms use the supplied opal enchanted bolt assumption, represented as opal
dragon bolts (e). Bolts are recorded in attack logs and use existing bolt-effect calculations;
reported ranged strength is preserved and ammunition strength is not added twice.
Ammunition remains unknown for the other forms and does not use player config defaults.

Reported NPC bonuses are preserved. Missing damage and ranged defence bonuses are
**estimates** from the reference gear's pinned cache parameters, including the identified
cape. These estimates do not prove Pete uses player equipment mechanics. They do not depend
on RuneLite's item-stat service having the new cape. Gear mappings enable existing weapon
calculations, Virtus ancient spell bonuses, and robe-hit classification.

Validation covers all 18 mappings and bonuses against a cache-derived fixture, gear
snapshot isolation across switches, arena bounds/planes, exclusions, levels, overheads,
and opal ammo behavior. Live attack timing and NPC-specific formulas still need verification.

