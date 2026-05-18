package nl.theepicblock.gametest.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerPacketListener;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class FakePlayer {
    public static ServerPlayer fakePlayer(ServerLevel world) {
        var player = new ServerPlayer(world.getServer(), world, new GameProfile(UUID.randomUUID(), "4555555555555555521111111111111111111111111111111111111111111fddddddddddddddddddddddddddddcc"), ClientInformation.createDefault()); // Player name provided by Mia, my cat
        // We need the player to tick, so we cannot use the fabric FakePlayer, so we must use this internal
        // api. Luckily it's only for gametests so no real risk of this breaking things
        //noinspection UnstableApiUsage
        player.connection = new FakePlayerPacketListener(player);
        return player;
    }
}
