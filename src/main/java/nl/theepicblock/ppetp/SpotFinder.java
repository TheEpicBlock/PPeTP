package nl.theepicblock.ppetp;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class SpotFinder {
    public static @Nullable BlockPos findSpot(ServerPlayerEntity player, Predicate<BlockPos> spotValidator) {
        var random = ThreadLocalRandom.current();
        var origin = player.getBlockPos();

        for(int i = 0; i < 10; ++i) {
            int j = random.nextInt(-3, 3);
            int k = random.nextInt(-3, 3);
            if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                int l = random.nextInt(-1, 1);
                var pos = new BlockPos(origin.getX() + j, origin.getY() + l, origin.getZ() + k);
                if (spotValidator.test(pos)) {
                    return pos;
                }
            }
        }

        return null;
    }
}
