package nl.theepicblock.ppetp;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import nl.theepicblock.ppetp.mixin.MobEntityAccessor;
import org.jetbrains.annotations.Nullable;

public class PetTeleporter {
    /**
     * Callback which is called by {@link nl.theepicblock.ppetp.mixin.TameableEntityMixin} whenever
     * minecraft tries to teleport a pet.
     */
    public static void teleportPet(TameableEntity pet, LivingEntity owner) {
        if (owner instanceof ServerPlayerEntity player && isPetFarAway(pet, owner)) {
            teleportToInventory(pet, player);
        }
    }

    /**
     * The pet is far enough that it's almost getting unloaded! It needs to get tp'ed right now.
     */
    public static void petAlmostUnloaded(TameableEntity pet) {
        // This method was called by a PPeTP mixin, instead of the tp being requested
        // so we must take extra care to ensure that the pet actually wants to be tp'ed

        // To do this, we check if it has a follow owner goal, and if that goal can be started
        // this should also ensure indypets compatibility, since they mixin to the canStart function
        var goals = ((MobEntityAccessor)pet).getGoalSelector().getGoals();
        FollowOwnerGoal goal = null;
        for (var g : goals) { if (g.getGoal() instanceof FollowOwnerGoal fg) { goal = fg; break; } }

        if (goal != null && goal.canStart()) {
            // We can't use the normal getOwner method because the player might've died
            var owner = getOwner(pet);
            if (owner != null) {
                teleportToInventory(pet, owner);
            }
        }
    }

    /**
     * Alternative implementation of {@link TameableEntity#getOwner()} that accounts for
     * the player being dead, or in a different dimension
     */
    public static @Nullable ServerPlayerEntity getOwner(TameableEntity pet) {
        var server = pet.getEntityWorld().getServer();
        if (server == null) return null;
        var ref = pet.getOwnerReference();
        if (ref == null) return null;
        var ownerUuid = ref.getUuid();
        if (ownerUuid == null) return null;
        return server.getPlayerManager().getPlayer(ownerUuid);
    }

    /**
     * Determines if a teleport to the inventory should occur. There may be more conditions
     * applied by {@link TameableEntity#shouldTryTeleportToOwner()}
     */
    public static boolean isPetFarAway(TameableEntity pet, LivingEntity owner) {
        if (pet.getEntityWorld() != owner.getEntityWorld()) {
            // Different dimension? That's pretty far away as far as I'm concerned!
            return true;
        }
        // Teleport when 48 blocks away horizontally.
        // Vanilla tp kicks in at 12 blocks away any direction.
        // Note that there's also an additional check for when the chunk unloads, which
        // is separate from this condition
        var dist = Math.abs(pet.getEntityPos().subtract(owner.getEntityPos()).horizontalLengthSquared());
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
