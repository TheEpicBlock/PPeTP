# PPeTP

In vanilla minecraft, pets can get lost when they exit simulation distance.
This can happen if you go somewhere and the pet can't find a valid teleportation spot it time

## How?

If you're far enough and it can't find a valid teleportation spot, it'll put your pet into your player data. This
means they can't be unloaded. For the player it looks like the pet smoothly teleports whatever distance they went.

In addition to the vanilla behaviour of teleporting the pet when it ticks and is too far away, it also
tries to teleport the pet just before chunks unload. This fixes situations where the chunks might suddenly unload
without the pet having a chance to tick and teleport. This happens when the player dies or teleports