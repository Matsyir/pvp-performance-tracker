# PvP Performance Tracker

Tracks PvP performance by keeping track of various stats. Only useful for 1v1 PvP fights that involve overhead prayers and/or gear switching, LMS being the perfect example. Multi will cause problems. Potentially inaccurate outside of LMS. 

**1.** Tracks if you successfully hit your opponent off-pray or not. For example, if your opponent is using protect from melee, you must hit him with magic or ranged for a successful off-pray hit.

**2.** Tracks deserved damage. This looks at you and your opponent's current gear, and calculates average damage based on accuracy & strength stats/pray. This does not track your current hits in any way, it doesn't involve hitsplats. More in-depth details on both of these stats below.

The vast majority of the deserved damage statistic was not implemented by myself, it was created by [Mazhar, @maz_rs on twitter](https://twitter.com/maz_rs), [@voiderman1337](https://github.com/voiderman1337) on github. Massive thanks to him, I think it really brings this plugin together since the off-pray hits alone can be misleading in a few relatively common cases.

Current Fight overlay (the *2 mins* label is an unrelated in-game overlay):

![Overlay Image](https://i.imgur.com/LhNSB5W.png)


Fight history panel:

![Panel Image](https://i.imgur.com/wYorhLe.png)


[Gif of LMS gameplay (pre-deserved damage tracker)](https://gfycat.com/LittleCompleteGannet)

Here's a full 10 min match if you want to see more (pre-deserved damage tracker): https://youtu.be/Qthc9ctj2GA

Config options:

![Image of config options](https://i.imgur.com/SqdE9Em.png)

Detailed tooltips:

![Tooltip example](https://i.imgur.com/zQOIg9W.png)

Note that I was not using 'Use Simple Overlay' and 'Show Overlay Title' in the gif/video. With Simple Overlay, it only shows the RSN and off-pray percentage, no fraction and no deserved damage (but they will still be in the panel).

## General notes
A component I'm not 100% confident of, although it works very well from the testing I've done so far: target selection. It all comes from the `onInteractingChanged` event, which can have some odd behaviour sometimes since following or trading will also trigger this event. Attacking someone already in combat will also trigger this event. I put a few different checks to validate and ignore this event depending on the state, but I'm sure are other ways it can be improved I didn't think about.

## Statistic #1: Off-pray hits
The stats are displayed as such: "successful off-pray hits / total attacks (success %)". See the gif/video or panel for example. The success percentage will be green if it is the highest one. These stats are displayed on the overlay, and on the panel as the second line. The success percentage is not a guaranteed way of determining if a player performed the best. It is a good general idea, but it is merely saying who hit off-pray more often than on-pray. You need to take into account weapons' attack speeds, total number of attacks, and armor used while attacking to get the full picture, but nonetheless this is a helpful metric to have. Combined with the deserved damage, we can pretty much figure out who performed better.

**I can't guarantee accuracy for use outside of LMS**, because of the way attack styles are determined: that is by checking the Player's current animation. There are definitely a few random weapons I still haven't thought about that won't be included. But I am **confident it will work in over 90% of cases**, since people tend to stick to fairly common pking loadouts. Thankfully, most weapons AnimationIDs are re-used (e.g all normal scim slashes share one animation, crossbow is same with all bolts). Due to this, loads of weapons I haven't even tested are supported.

### Supported Weapons (Off-pray hits)
It would take forever to make a nicely formatted list of these, and on top of that many weapon animations are re-used, so more than I know are supported. Check out the variable names & comments in [this file](https://github.com/Matsyir/pvp-performance-tracker/blob/master/src/main/java/matsyir/pvpperformancetracker/AnimationID.java) for a full breakdown of supported weapons/what I've tested. All LMS gear should be supported, as well as general popular pking gear, including some less common weapons like anchor, the 4 godsword specs, revenant weapons, dmace, etc. Basically all F2P weapons should be supported as well but I don't think this would be useful for F2P. There are surely some uncommon but relevant weapons or specs I forgot about so feel free to submit those as an issue if you notice it in a fight, or not mentioned in the file.

### Known unsupported weapons (Off-pray hits)
Not because they can't work, simply because I don't have their AnimationIDs.
- **Nightmare staves** and their specs: probably works for any spell cast, but not certainly. Specs I definitely don't have.
- **Inquisitor's mace**: It probably re-uses simple animations that are supported, but I can't be sure.

## Statistic #2: Deserved Damage
This directly takes all of you and your opponent's gear, calculates the total stats, and gives you an average damage value, using OSRS accuracy/damage calculations, depending on opponent's gear, pray, if you're using a special attack, among other factors. It does not involve hitsplats in any way, does not look at actual damage dealt. It only calculates average "deserved" damage. This is also displayed on the overlay & panel, on the 3rd line, showing the cumulative value for that fight and the difference with the opponent. For example, if you've dealt 3 attacks, granting 14, 9, and 12 damage respectively, you'd have 35 total deserved damage. If your opponent had 40 damage at the time, your deserved damage would be displayed as "35 (-5)".

Currently, it does not check for offensive prayers. It assumes that you always use the right offensive prayer, and it assumes that you are using one of the 25% prayers for defence, but not augury while getting maged, since you would more likely be trying to range/melee at that time. We could detect the player's offensive prayer, but not the opponent's. To make that fair, we use equal estimations for both players, and the inaccuracies should be handed out evenly.

This also is not 100% reliable, but it too should work in the vast majority of cases. This component is able to retrieve item stats dynamically, from cached wiki data, for the vast majority of relevant items. Some very obscure items won't have stats. One problem is bolts/ammo, we can't detect which are used, so there are configs to choose which ones are used for the estimations. Another is rings, we can't detect that either. Base spell damage also needs to be hardcoded based on animation, which is not completely possible since animations are shared between different spells. However, all other melee gear stats or general range/mage gear stats can be automatically retrieved and used for damage calculations.

The damage calculations can be found all across [this file](https://github.com/Matsyir/pvp-performance-tracker/blob/master/src/main/java/matsyir/pvpperformancetracker/PvpDamageCalc.java). Range Ammo Data is in [this file](https://github.com/Matsyir/pvp-performance-tracker/blob/master/src/main/java/matsyir/pvpperformancetracker/RangeAmmoData.java). 

**Stats are currently assumed for both the player and opponent**, using potted LMS stats: 118 Attack/Strength, 75 Defence, 112 Range, 99 Magic
### Supported Weapons (Deserved Damage)
All weapon & gear stats are loaded dynamically. The exceptions to this are stats for rings and ammo (bolts/arrows), and base magic spell damage. We can't detect what bolts/rings are used, and we must manually hardcode all base spell damage. Special attacks with damage or accuracy modifiers also have to be manually hardcoded.
#### Melee - Supported special attacks:
- Dragon Claws
- Dragon Dagger
- Armadyl Godsword
- Vesta's Longsword
- Statius' Warhammer

#### Range: - Supported weapons/specs/ammo:
Ammo is either specified in config, or assumed. LMS uses regular diamond bolts (e) even for ACB so that is default. Bolt specs assume no diary completion. Antifire potions & anti-dragon shields are never taken into account for dragonfire bolts, if chosen.
- Rune Crossbow [config ammo]
- Dragon Crossbow [config ammo]
- Armadyl Crossbow (w/ spec) [config ammo]
- Dragon Hunter Crossbow [config ammo]
- Blowpipe [config ammo]
- Light Ballista [Dragon Javelins assumed]
- Heavy Ballista (w/ spec) [Dragon Javelins assumed]
- Dark Bow (w/ spec) [Dragon Arrows assumed]
- Magic Shortbow [Amethyst Arrows assumed]
- Magic Shortbow (i) [Amethyst Arrows assumed]
- Craw's Bow [Ammo bonus built into weapon - fixed ammo stats]

**RCB Ammo config options:**
- Runite Bolts
- Diamond Bolts (e)
- Dragonfire Bolts (e)

**ACB/DCB/DHCB Ammo config options (includes RCB Ammo):**
- Diamond Dragon Bolts (e)
- Dragonfire Dragon Bolts (e)
- Opal Diamond Bolts (e)

**Blowpipe Ammo config options:**
- Adamant Darts
- Rune Darts
- Dragon Darts

#### Mage
Based on animations, which are re-used for many spells, so can't be fully accurate. Other sets of spells could be added but it's tedious. Submit an issue if you'd make use of more.
- Ice Barrage (Any multi-target ancient spell will use ice barrage damage - yes, even *smoke burst*, can't currently differentiate)
- Ice Blitz (Any single-target ancient spell will use ice blitz damage)

Since these are currently the only specified spells, anything that isn't a multi-target ancient spell will use ice blitz damage to avoid being completely off.

## Known issues
- Occasionally, certain attacks won't be counted when attacking each other at the same time, since their animation will be cancelled and the attack will go undetected. Since attack styles are currently determined using animations, I don't think it's possible to fix at the moment. This has only been seen with Ahrim's staff, but it's a rare occurence. It must be possible with other weapons. This is probably related to 'blocking' animations.
- **Double deaths *not* on the same tick are not tracked.** This can be fixed using the onPlayerDeath event - it's changing how the existing code works around deaths that is tricky.  
- **Darts will often not get counted** as their animation lasts longer than their attack so the animation doesn't change in line with the attacks. I don't think this can currently be fixed with how attack styles are determined. This probably happens with other fast weapons I haven't found yet. Blowpipe works fine.
- There is no attempt to support multi at the moment, but I would assume it works to a certain extent, on 1 opponent at a time.
- Attacks before both players took part of the fight are not saved. This is done to prevent other interactions during a fight being interpreted as getting a new target.


I would love to see other features/stats come into this plugin in the future, feel free to submit issues/suggestions & PRs. If you find a weapon that doesn't work, let me know as well.
