# PvP Performance Tracker

Tracks PvP performance by keeping track of various stats. Only useful for 1v1 PvP fights that involve overhead prayers and/or gear switching, LMS being the perfect example. Multi will cause problems.

**1.** Tracks if you successfully hit your opponent off-pray or not. For example, if your opponent is using protect from melee, you must hit him with magic or ranged for a successful off-pray hit.

**2.** Tracks deserved damage. This looks at you and your opponent's current gear, and calculates average damage based on accuracy & strength stats/pray. This does not track your current hits in any way, it doesn't involve hitsplats. More in-depth details on both of these stats below.

Fight history panel:

![Panel Image](https://i.imgur.com/rfOpoBO.png)


[Gif of LMS gameplay (pre-deserved damage tracker)](https://gfycat.com/LittleCompleteGannet)

Here's a full 10 min match if you want to see more (pre-deserved damage tracker): https://youtu.be/Qthc9ctj2GA

Config options:

![Image of config options](https://i.imgur.com/5mN8XzO.png)

Note that I turned off 'Use Simple Overlay' and 'Show Overlay Title' in the gif/video. With Simple Overlay, it only shows the RSN and percentage, no fraction.

## General notes
A component I'm not 100% confident of, although it works very well from the testing I've done so far: target selection. It all comes from the `onInteractingChanged` event, which can have some odd behaviour sometimes since following or trading will also trigger this event. Attacking someone already in combat will also trigger this event. I put a few different checks to validate and ignore this event depending on the state, but I'm sure are other ways it can be improved I didn't think about.

## Statistic #1: Off-pray hits
The stats are displayed as such: "successful off-pray hits / total attacks (success %)". See the gif/video or panel for example. The success percentage will be green if it is the highest one. These stats are displayed on the overlay, and on the panel as the second line. The success percentage is not a guaranteed way of determining if a player performed the best. It is a good general idea, but it is merely saying who hit off-pray more often than on-pray. You need to take into account weapons' attack speeds, total number of attacks, and armor used while attacking to get the full picture, but nonetheless this is a helpful metric to have. Combined with the deserved damage, we can pretty much figure out who performed better.

**I can't guarantee accuracy for use outside of LMS**, because of the way attack styles are determined: that is by checking the Player's current animation. There are definitely a few random weapons I still haven't thought about that won't be included. But I am **confident it will work in over 90% of cases**, since people tend to stick to fairly common pking loadouts. Thankfully, most weapons AnimationIDs are re-used (e.g all normal scim slashes share one animation, crossbow is same with all bolts). Due to this, loads of weapons I haven't even tested are supported.

### Supported Weapons
It would take forever to make a nicely formatted list of these, and on top of that many weapon animations are re-used, so more than I know are supported. Check out the variable names & comments in [this file](https://github.com/Matsyir/pvp-performance-tracker/blob/master/src/main/java/com/pvpperformancetracker/AnimationID.java) for a full breakdown of supported weapons/what I've tested. All LMS gear should be supported, as well as general popular pking gear, including some less common weapons like anchor, the 4 godsword specs, revenant weapons, dmace, etc. Basically all F2P weapons should be supported as well but I don't think this would be useful for F2P. There are surely some uncommon but relevant weapons or specs I forgot about so feel free to submit those as an issue if you notice it in a fight, or not mentioned in the file.

### Known unsupported weapons
Not because they can't work, simply because I don't have their AnimationIDs.
- **Nightmare staves** and their specs: probably works for any spell cast, but not certainly. Specs I definitely don't have.
- **Inquisitor's mace**: It probably re-uses simple animations that are supported, but I can't be sure.

## Statistic #2: Deserved Damage
This directly takes all of you and your opponent's gear, calculates the total stats, and gives you an average damage value, using OSRS accuracy/damage calculations, depending on opponent's gear, pray, if you're using a special attack, among other factors. It does not involve hitsplats in any way, does not look at actual damage dealt. It only calculates average "deserved" damage. This is also displayed on the overlay & panel, on the 3rd line, showing the cumulative value for that fight and the difference with the opponent. For example, if you've dealt 3 attacks, granting 14, 9, and 12 damage respectively, you'd have 35 total deserved damage. If your opponent had 40 damage at the time, your deserved damage would be displayed as "35 (-5)".

Currently, it does not check for offensive prayers. It assumes that you always use the right offensive prayer, and it assumes that you are using one of the 25% prayers for defence, but not augury while getting maged, since you would more likely be trying to range/melee at that time. This works the same for both players so the differences are handed out equally.

This also is not 100% reliable, but it should work in the vast majority of cases, even moreso than the off-pray hits. This component is able to retrieve item stats dynamically, from cached wiki data, for the vast majority of relevant items. Some very obscure items won't have stats. One problem is bolts/ammo, we can't easily detect which are used, so for the time being those are assumed to be diamond bolts for crossbows, dragon arrows for dark bows, dragon javelins for ballistas, and amethyst arrows for MSBs.

The damage calculations can be found all across [this file](https://github.com/Matsyir/pvp-performance-tracker/blob/deserved-dps-tracker/src/main/java/com/pvpperformancetracker/PvpDamageCalc.java).

## Known issues
- **Double deaths *not* on the same tick are not tracked.** This can be fixed using the onPlayerDeath event - it's changing how the existing code works around deaths that is tricky.  
- **Darts will often not get counted** as their animation lasts longer than their attack so the animation doesn't change in line with the attacks. I don't think this can currently be fixed with how attack styles are determined. This probably happens with other fast weapons I haven't found yet. Blowpipe works fine.
- There is no attempt to support multi at the moment, but I would assume it works to a certain extent, on 1 opponent at a time.


I would love to see other features/stats come into this plugin in the future, feel free to submit issues/suggestions & PRs. If you find a weapon that doesn't work, let me know as well.
