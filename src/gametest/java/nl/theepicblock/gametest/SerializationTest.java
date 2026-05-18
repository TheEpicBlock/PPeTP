package nl.theepicblock.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.storage.TagValueInput;
import nl.theepicblock.gametest.util.FakePlayer;
import nl.theepicblock.ppetp.PlayerDuck;
import nl.theepicblock.ppetp.test.Util;

import static nl.theepicblock.ppetp.PPeTP.LOGGER;

public class SerializationTest {
    private static int LATEST_VERSION = 4661;

    @GameTest
    public void canReadOldData(GameTestHelper context) throws Exception {
        // This is from right before 1.21.11 was released, up until that point the old format might've still existed
        canReadData(context, "/old_format.snbt", 4658);
    }

    @GameTest
    public void canReadNewData(GameTestHelper context) throws Exception {
        canReadData(context, "/new_format.snbt", 4292);
    }

    public void canReadData(GameTestHelper context, String name, int version) throws Exception {
        var oldPlayerData = Util.readNbtResource(name);

        // Update the nbt
        var dyn = new Dynamic<>(NbtOps.INSTANCE, oldPlayerData);
        var newNbt = context.getLevel().getServer().getFixerUpper().update(References.PLAYER, dyn, version, LATEST_VERSION).cast(NbtOps.INSTANCE);

        // Load data
        var reader = TagValueInput.create(new ProblemReporter.ScopedCollector(LOGGER), context.getLevel().registryAccess(), (CompoundTag)newNbt);
        var player = FakePlayer.fakePlayer(context.getLevel());
        player.load(reader);

        // Do checks
        var storage = ((PlayerDuck)player).PPeTP$getStorage();
        context.assertValueEqual(1, storage.getNumberOfStoredPets(), "Should have decoded one pet");

        // yay
        context.succeed();
    }
}
