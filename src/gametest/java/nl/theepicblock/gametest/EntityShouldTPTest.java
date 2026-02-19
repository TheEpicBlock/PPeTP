package nl.theepicblock.gametest;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import nl.theepicblock.gametest.util.FakePlayer;
import nl.theepicblock.ppetp.PlayerDuck;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EntityShouldTPTest {
    @GameTest
    public void testEachEntity(TestContext context) {
        var world = context.getWorld();
        var testCenter = context.getAbsolutePos(BlockPos.ORIGIN).add(10000,0,0);
        var chunkLoader = FakePlayer.fakePlayer(world);
        chunkLoader.setPosition(testCenter.toBottomCenterPos());
        world.spawnEntity(chunkLoader);

        var checks = new ArrayList<Runnable>();
        context.waitAndRun(1, () -> {
            chunkLoader.kill(world);
            var range = BlockPos.iterate(testCenter, testCenter.add(15,0,15)).iterator();

            for (var entityType : Registries.ENTITY_TYPE) {
                var pos = range.next().toBottomCenterPos();
                var player = FakePlayer.fakePlayer(world);
                player.getAbilities().allowFlying = true;
                player.getAbilities().flying = true;
                player.setPosition(pos);
                world.spawnEntity(player);

                var entity = entityType.create(world, SpawnReason.COMMAND);
                if (entity == null) {
                    context.assertFalse(shouldTp(entityType), " precondition failed, "+Registries.ENTITY_TYPE.getId(entityType)+" should tp but it could not be created");
                    continue;
                }
                entity.setInvulnerable(true);
                entity.setPosition(pos);
                world.spawnEntity(entity);
                if (entity instanceof TameableEntity pet) {
                    pet.setTamedBy(player);
                }

                // Tp player far away, enough to unload chunks
                player.setPosition(pos.add(-10000, 500, 0));

                // Check if tp'ed
                checks.add(() -> {
                    context.assertTrue(((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets() <= 1, "precondition failed, somehow more than 1 entity has been stored");
                    var tped = ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets() == 1;
                    System.out.println((world.getEntity(entity.getUuid()) != null) + " " + entity.isAlive()+" "+Registries.ENTITY_TYPE.getId(entityType)+" "+shouldTp(entityType)+" "+tped);
//                context.assertEquals(shouldTp(entityType), tped, Registries.ENTITY_TYPE.getId(entityType) + " following the player");
                });
            }
        });
        context.waitAndRun(15, () -> {
            checks.forEach(Runnable::run);
        });
        context.waitAndRun(19, context::complete);
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
