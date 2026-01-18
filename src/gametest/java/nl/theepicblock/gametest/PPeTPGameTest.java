package nl.theepicblock.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import nl.theepicblock.ppetp.PlayerDuck;

public class PPeTPGameTest {
    /// Teleports a fake player somewhat far, not far enough to unload chunks, to a place where a pet can't just tp to
    @GameTest
    public void basicTp(TestContext context) {
        var world = context.getWorld();
        var testCenter = context.getAbsolutePos(BlockPos.ORIGIN).toBottomCenterPos();
        var player = FakePlayer.fakePlayer(world);
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.setPosition(testCenter);
        world.spawnEntity(player);

        var pet = new WolfEntity(EntityType.WOLF, world);
        world.spawnEntity(pet);
        pet.setTamedBy(player);
        pet.setPosition(testCenter);
        var uuidPet = pet.getUuid();

        // Execute teleport
        player.setPosition(testCenter.add(100, 500, 0));

        // Should've tp'ed
        context.waitAndRun(1, () -> {
            context.assertTrue(world.getEntity(uuidPet) == null, "Pet should've teleported into PPeTP");
            context.assertEquals(1, ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets(), "Pet should've teleported into PPeTP");

            // Tp back to normal location
            player.setPosition(testCenter);
            context.waitAndRun(10, () -> {
                context.assertTrue(world.getEntity(uuidPet) != null, "Pet should've teleported back into the world");
                context.assertEquals(0, ((PlayerDuck)player).PPeTP$getStorage().getNumberOfStoredPets(), "Pet should've teleported back into the world");
                context.complete();
            });
        });
    }
}
