package nl.theepicblock.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import nl.theepicblock.gametest.util.FakePlayer;
import nl.theepicblock.ppetp.PetTeleporter;
import nl.theepicblock.ppetp.PlayerDuck;

import java.util.List;

public class EntityShouldTPTest {
    @GameTest
    public void simulateUnload(GameTestHelper context) {
        var world = context.getLevel();
        var testCenter = context.absolutePos(BlockPos.ZERO).offset(100,0,0);
        var range = BlockPos.betweenClosed(testCenter, testCenter.offset(15,0,15)).iterator();

        for (var entityType : BuiltInRegistries.ENTITY_TYPE) {
            var pos = Vec3.atBottomCenterOf(range.next());
            var entity = entityType.create(world, EntitySpawnReason.COMMAND);
            if (entity == null) {
                context.assertFalse(shouldTp(entityType), " precondition failed, "+BuiltInRegistries.ENTITY_TYPE.getKey(entityType)+" should tp but it could not be created");
                continue;
            }
            entity.setInvulnerable(true);
            entity.setPos(pos);
            world.addFreshEntity(entity);
            if (entity instanceof TamableAnimal pet) {
                var player = FakePlayer.fakePlayer(world);
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.setPos(pos.add(0, 500, 0));
                world.addFreshEntity(player);
                world.getServer().getPlayerList().respawn(player, true, Entity.RemovalReason.KILLED);
                pet.tame(player);

                // Simulate unload
                PetTeleporter.petAlmostUnloaded(pet);

                // Check if tp'ed
                context.assertTrue(((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets() <= 1, "precondition failed, somehow more than 1 entity has been stored");
                var tped = ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets() == 1;
                context.assertValueEqual(shouldTp(entityType), tped, BuiltInRegistries.ENTITY_TYPE.getKey(entityType) + " following the player");
            } else {
                entity.kill(world);
            }
        }
        context.succeed();
    }

    private static boolean shouldTp(EntityType<?> entity) {
        var mcEntitiesThatTp = List.of(
                EntityTypes.WOLF,
                EntityTypes.CAT,
                EntityTypes.PARROT
        );
        return mcEntitiesThatTp.contains(entity);
    }
}
