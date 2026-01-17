package nl.theepicblock.gametest;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import nl.theepicblock.ppetp.PlayerDuck;

import java.util.UUID;

public class PPeTPGameTest {
    /// Teleports a fake player somewhat far, not far enough to unload chunks, to a place where a pet can't just tp to
    @GameTest
    public void basicTp(TestContext context) {
        var world = context.getWorld();
        var testCenter = context.getAbsolutePos(BlockPos.ORIGIN).toBottomCenterPos();
        var player = FakePlayer.get(world, new GameProfile(UUID.randomUUID(), "Bob"));
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.setPosition(testCenter);

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
            context.complete();
        });
    }
}
