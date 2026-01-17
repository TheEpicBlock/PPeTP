package nl.theepicblock.gametest;

import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.datafixer.Schemas;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.test.TestContext;
import net.minecraft.util.ErrorReporter;
import nl.theepicblock.ppetp.PlayerDuck;
import nl.theepicblock.ppetp.PlayerPetStorage;
import nl.theepicblock.ppetp.test.Util;

import static nl.theepicblock.ppetp.PPeTP.LOGGER;

public class PPeTPGameTest {
    private static int LATEST_VERSION = 4661;

    @GameTest
    public void canReadOldData(TestContext context) throws Exception {
        // This is from right before 1.21.11 was released, up until that point the old format might've still existed
        canReadData(context, "/old_format.snbt", 4658);
    }

    @GameTest
    public void canReadNewData(TestContext context) throws Exception {
        canReadData(context, "/new_format.snbt", LATEST_VERSION);
    }

    public void canReadData(TestContext context, String name, int version) throws Exception {
        var oldPlayerData = Util.readNbtResource(name);

        // Update the nbt
        var dyn = new Dynamic<>(NbtOps.INSTANCE, oldPlayerData);
        var newNbt = context.getWorld().getServer().getDataFixer().update(TypeReferences.PLAYER, dyn, version, LATEST_VERSION).cast(NbtOps.INSTANCE);

        // Load data
        var reader = NbtReadView.create(new ErrorReporter.Logging(LOGGER), context.getWorld().getRegistryManager(), (NbtCompound)newNbt);
        var player = FakePlayer.get(context.getWorld());
        player.readData(reader);

        // Do checks
        var storage = ((PlayerDuck)player).PPeTP$getStorage();
        context.assertEquals(1, storage.getNumberOfStoredPets(), "Should have decoded one pet");

        // yay
        context.complete();
    }
}
