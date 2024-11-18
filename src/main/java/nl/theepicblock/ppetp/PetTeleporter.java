package nl.theepicblock.ppetp;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class PetTeleporter {
    /**
     * Callback which is called by {@link nl.theepicblock.ppetp.mixin.TameableEntityMixin} whenever
     * minecraft tries to teleport a pet.
     */
    public static void teleportPet(TameableEntity pet, LivingEntity owner) {
        if (owner instanceof ServerPlayerEntity player && shouldTeleportToInventory(pet, owner)) {
            teleportToInventory(pet, player);
        }
    }

    /**
     * The pet is far enough that it's almost getting unloaded! It needs to get tp'ed right now.
     * This method ignores the distance requirement since it's clearly too far away already.
     */
    public static void petAlmostUnloaded(TameableEntity pet) {
        if (pet.cannotFollowOwner()) {
            // Nvm, the pet is not following us right now
            return;
        }

        // We can't use the normal getOwner method because the player might've died
        var owner = getOwner(pet);
        if (owner != null) {
            teleportToInventory(pet, owner);
        }
    }

    /**
     * Alternative implementation of {@link TameableEntity#getOwner()} that accounts for
     * the player being dead, or in a different dimension
     */
    public static ServerPlayerEntity getOwner(TameableEntity pet) {
        var server = pet.getWorld().getServer();
        if (server == null) return null;
        var ownerUuid = pet.getOwnerUuid();
        return server.getPlayerManager().getPlayer(ownerUuid);
    }

    /**
     * Determines if a teleport to the inventory should occur. There may be more conditions
     * applied {@link TameableEntity#shouldTryTeleportToOwner()}
     */
    public static boolean shouldTeleportToInventory(TameableEntity pet, LivingEntity owner) {
        if (pet.getWorld() != owner.getWorld()) {
            // Different dimension? That's pretty far away as far as I'm concerned!
            return true;
        }
        // Teleport when 48 blocks away horizontally.
        // Vanilla tp kicks in at 12 blocks away any direction.
        // Note that there's also an additional check for when the chunk unloads, which
        // is separate from this condition
        var dist = Math.abs(pet.getPos().horizontalLengthSquared() - owner.getPos().horizontalLengthSquared());
        return dist >= (48 * 48);
    }

    /**
     * "Teleports" the pet into the players "inventory". Aka, it deletes the pet from the world and stores it
     * in the player's data instead
     */
    public static void teleportToInventory(TameableEntity pet, ServerPlayerEntity player) {
        var storage = ((PlayerDuck)player).PPeTP$getStorage();
        var success = storage.insert(pet);
        if (!success) {
            // Something went wrong whilst saving. Just abort
            return;
        }

        // Discard is in fact the right method to call here. It's what parrots do
        // when they sit on their owner's shoulders
        pet.discard();
    }
}
