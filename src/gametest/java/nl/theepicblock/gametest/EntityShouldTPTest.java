package nl.theepicblock.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import nl.theepicblock.gametest.util.FakePlayer;
import nl.theepicblock.ppetp.PetTeleporter;
import nl.theepicblock.ppetp.PlayerDuck;

import java.util.List;

public class EntityShouldTPTest {
    @GameTest
    public void simulateUnload(TestContext context) {
        var world = context.getWorld();
        var testCenter = context.getAbsolutePos(BlockPos.ORIGIN).add(100,0,0);
        var range = BlockPos.iterate(testCenter, testCenter.add(15,0,15)).iterator();

        for (var entityType : Registries.ENTITY_TYPE) {
            var pos = range.next().toBottomCenterPos();
            var entity = entityType.create(world, SpawnReason.COMMAND);
            if (entity == null) {
                context.assertFalse(shouldTp(entityType), " precondition failed, "+Registries.ENTITY_TYPE.getId(entityType)+" should tp but it could not be created");
                continue;
            }
            entity.setInvulnerable(true);
            entity.setPosition(pos);
            world.spawnEntity(entity);
            if (entity instanceof TameableEntity pet) {
                var player = FakePlayer.fakePlayer(world);
                player.getAbilities().allowFlying = true;
                player.getAbilities().flying = true;
                player.setPosition(pos.add(0, 500, 0));
                world.spawnEntity(player);
                world.getServer().getPlayerManager().respawnPlayer(player, true, Entity.RemovalReason.KILLED);
                pet.setTamedBy(player);

                // Simulate unload
                PetTeleporter.petAlmostUnloaded(pet);

                // Check if tp'ed
                context.assertTrue(((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets() <= 1, "precondition failed, somehow more than 1 entity has been stored");
                var tped = ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets() == 1;
                context.assertEquals(shouldTp(entityType), tped, Registries.ENTITY_TYPE.getId(entityType) + " following the player");
            } else {
                entity.kill(world);
            }
        }
        context.complete();
    }

    private static boolean shouldTp(EntityType<?> entity) {
        var mcEntitiesThatTp = List.of(
                EntityType.WOLF,
                EntityType.CAT,
                EntityType.PARROT
        );
        return mcEntitiesThatTp.contains(entity);
    }
}
