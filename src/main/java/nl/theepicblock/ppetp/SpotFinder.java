package nl.theepicblock.ppetp;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class SpotFinder {
    public static @Nullable BlockPos findSpot(ServerPlayer player, Predicate<BlockPos> spotValidator) {
        var random = ThreadLocalRandom.current();
        var origin = player.blockPosition();

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
