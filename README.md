##Details have been moved to [the wiki](https://github.com/Matsyir/pvp-performance-tracker/wiki)
# PvP Performance Tracker

Tracks PvP performance by keeping track of various stats. Only useful for 1v1 PvP fights that involve overhead prayers and/or gear switching, LMS being the perfect example. Multi will cause problems. Potentially inaccurate outside of LMS. 

**1.** Tracks if you successfully hit your opponent off-pray or not. For example, if your opponent is using protect from melee, you must hit him with magic or ranged for a successful off-pray hit.

**2.** Tracks deserved damage. Every hit, this looks at you and your opponent's current gear, and calculates average damage based on accuracy, strength, & prayers. This does **not** track your *actual* hits in any way, it doesn't involve hitsplats. More in-depth details on both of these stats below.

The vast majority of the deserved damage statistic was not implemented by myself, it was created by [Mazhar, @maz_rs on twitter](https://twitter.com/maz_rs), [@voiderman1337](https://github.com/voiderman1337) on github. Massive thanks to him, I think it really brings this plugin together since the off-pray hits alone can be misleading in a few relatively common cases.

## Note about the recent 1v1 Tournament:

The plugin is perfect to practice/improve for this type of tournament, it was great to see it used & discussed so much. It was a great stress-test to verify that everything is working. The plugin isn't perfect due to the available data we have to make these estimations, but for the most part it should have been accurate for the tournament. All the gear used is supported. Aside from some previously known minor issues (listed below), we found that special attacks outside of LMS were not detected properly, and therefore had invalid damage. This *could* make a significant difference in the outcome of deserved damage - so it was a fairly important problem. But, the issue was present for both players, so in most cases it shouldn't have made much of a difference. At least it only affected special attacks - any other regular attack should have worked as intended. This has since been fixed. Thanks once again to Mazhar for noticing the bug and helping to test solutions.

It's worth noting you should set the config to use the same ammo as you are for most accurate results.

-------------------------------
I would love to see other features/stats come into this plugin in the future, feel free to submit issues/suggestions & PRs. If you find a weapon that doesn't work, let me know as well.
