![A big body of water with a dog on one side, and a Minecraft character on the other. An arrow indicates the dog can still follow the character](https://github.com/TheEpicBlock/PPeTP/raw/main/banner.png)
# Proper Pet TP

Have you ever been exploring, and found a cool dog, cat or parrot?
Have you ever continued exploring only to find that pet not following you anymore?
This mod fixes that! In vanilla, pets stop following you if they're outside of simulation distance.
This mod ensures they follow you around either way. You can't accidentally lose your pet anymore!

## What if my dog's sitting?
This mod won't change that. If your pet wouldn't normally teleport, due to being leashed or sitting,
it still won't!

## How?
Pets commonly get lost if they don't have any safe spot to teleport to.
This mod temporarily attaches the pet to your player data until you land in a safe spot.
This is a simple way to ensure that the pet can always keep following you. For the pet-owner,
it'll look as if the pet just smoothly teleported thousands of blocks.

In addition to minecraft's own checks for when a pet is too far away, this mod also ensures
that pets have a chance to teleport when chunks get suddenly unloaded. This handles cases
where the player suddenly teleports (through commands or enderpearls), or when the player dies.

## What about cross-dimensional teleports?
PPeTP retains the vanilla behaviour of not teleporting across dimensions. If you take a long nether trip, this
mod will still allow your pet to teleport to you, but only once you're back in the overworld.
You can change this behaviour using `/gamerule petTeleportCrossDimension`. When set to true, your pet will teleport to
you even if it's in another dimension. If it's false, pets will only switch dimensions if they themselves touch a portal
(like in vanilla). This behaviour may be confusing if a pet accidentally walks through a portal.

## Does this work with modded pets?
Yeah, if they reuse Minecraft's code for tameable entities, it should be fine!
