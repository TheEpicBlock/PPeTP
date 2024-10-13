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
     * Determines if a teleport to the inventory should occur. There may be more conditions
     * applied {@link TameableEntity#shouldTryTeleportToOwner()}
     */
    public static boolean shouldTeleportToInventory(TameableEntity pet, LivingEntity owner) {
        // Teleport when 14 blocks away horizontally.
        // Vanilla tp kicks in at 12 blocks away any direction.
        var dist = Math.abs(pet.getPos().horizontalLengthSquared() - owner.getPos().horizontalLengthSquared());
        return dist >= (14 * 14);
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
