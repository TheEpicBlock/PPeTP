package nl.theepicblock.gametest.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class FakePlayer {
    public static ServerPlayerEntity fakePlayer(ServerWorld world) {
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "4555555555555555521111111111111111111111111111111111111111111fddddddddddddddddddddddddddddcc"), SyncedClientOptions.createDefault()); // Player name provided by Mia, my cat
        player.networkHandler = new FakePlayerNetworkHandler(player);
        return player;
    }
}
