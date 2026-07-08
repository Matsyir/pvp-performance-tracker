# PvP Performance Tracker (v1.8.4)
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
