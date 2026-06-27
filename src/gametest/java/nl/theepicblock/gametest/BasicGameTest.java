package nl.theepicblock.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;
import nl.theepicblock.gametest.util.FakePlayer;
import nl.theepicblock.ppetp.PlayerDuck;

public class BasicGameTest {
    /// Teleports a fake player somewhat far, not far enough to unload chunks, to a place where a pet can't just tp to
    @GameTest
    public void basicTp(GameTestHelper context) {
        var world = context.getLevel();
        var testCenter = Vec3.atBottomCenterOf(context.absolutePos(BlockPos.ZERO));
        var player = FakePlayer.fakePlayer(world);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.setPos(testCenter);
        world.addFreshEntity(player);

        var pet = new Wolf(EntityTypes.WOLF, world);
        world.addFreshEntity(pet);
        pet.tame(player);
        pet.setPos(testCenter);
        var uuidPet = pet.getUUID();

        // Execute teleport
        player.setPos(testCenter.add(100, 500, 0));

        // Should've tp'ed
        context.runAfterDelay(1, () -> {
            context.assertTrue(world.getEntity(uuidPet) == null, "Pet should've teleported into PPeTP");
            context.assertValueEqual(1, ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets(), "Pet should've teleported into PPeTP");

            // Tp back to normal location
            player.setPos(testCenter);
            context.runAfterDelay(10, () -> {
                context.assertTrue(world.getEntity(uuidPet) != null, "Pet should've teleported back into the world");
                context.assertValueEqual(0, ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets(), "Pet should've teleported back into the world");
                context.succeed();
            });
        });
    }
}
