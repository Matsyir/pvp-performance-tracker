# PvP Performance Tracker

Tracks if you successfully hit your opponent off-pray or not. For example, if your opponent is using protect from melee, you must hit him with magic or ranged for a successful off-pray hit.

Fight history panel:

![Panel Image](https://i.imgur.com/jGpayTj.png)


[Gif of LMS gameplay](https://gfycat.com/LittleCompleteGannet)

Here's a full 10 min match if you want to see more: https://youtu.be/Qthc9ctj2GA

Config options:

![Image of config options](https://i.imgur.com/H4qqJJA.png)

Note that I turned off 'Use Simple Overlay' and 'Show Overlay Title' in the gif/video. With Simple Overlay, it only shows the RSN and percentage, no fraction.

The stats are displayed as such: "successful off-pray hits / total attacks (success %)" See the gif/video or panel for example. The success percentage will be green if it is the highest one. The success percentage is not a guaranteed way of determining if a player performed the best. It is a good general idea, but it is merely saying who hit off-pray more often than on-pray. You need to take into account weapons' attack speeds and total number of attacks to get the full picture, but nonetheless this is a helpful metric to have. For your current fight, it displays your stats on an overlay.

**I can't guarantee accuracy for use outside of LMS**, because of the way attack styles are determined: that is by checking the Player's current animation. There are definitely a few random weapons I still haven't thought about that won't be included. But I am **confident it will work in over 90% of cases**, since people tend to stick to fairly common pking loadouts. Thankfully, most weapons AnimationIDs are re-used (e.g all normal scim slashes share one animation, crossbow is same with all bolts). Due to this, loads of weapons I haven't even tested are supported.

Another part of the logic I'm not 100% confident of, although it works very well from the testing I've done so far: target selection. It all comes from the `onInteractingChanged` event, which can have some odd behaviour sometimes since following or trading will also trigger this event. Attacking someone already in combat will also trigger this event. I put some checks to only trust a fight as started once the player has attacked back, but I bet there are other ways it can be improved I didn't think of.

I would love to see other features/stats come into this plugin in the future, feel free to submit issues & PRs. If you find a weapon that doesn't work, let me know as well.

### Supported Weapons
It would take forever to make a nicely formatted list of these, and on top of that many weapon animations are re-used, so more than I know are supported. Check out the variable names & comments in [this file](https://github.com/Matsyir/pvp-performance-tracker/blob/master/src/main/java/com/pvpperformancetracker/AnimationID.java) for a full breakdown of supported weapons/what I've tested. All LMS gear should be supported, as well as general popular pking gear, including some less common weapons like anchor, the 4 godsword specs, revenant weapons, dmace, etc. Basically all F2P weapons should be supported as well but I don't think this would be useful for F2P.

### Known issues
- Double deaths *not* on the same tick are not tracked. This can be fixed using the onPlayerDeath event - it's changing how the existing code works around deaths that is tricky.  
- Darts will often not get counted as their animation lasts longer than their attack so the animation doesn't change in line with the attacks. I don't think this can currently be fixed with how attack styles are determined. This probably happens with other fast weapons I haven't found yet.

### Known unsupported weapons
- Nightmare staves and their specs
- Inquisitor's mace
