package nl.theepicblock.gametest.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class FakePlayer {
    public static ServerPlayer fakePlayer(ServerLevel world) {
        var player = new ServerPlayer(world.getServer(), world, new GameProfile(UUID.randomUUID(), "4555555555555555521111111111111111111111111111111111111111111fddddddddddddddddddddddddddddcc"), ClientInformation.createDefault()); // Player name provided by Mia, my cat
        player.connection = new FakePlayerNetworkHandler(player);
        return player;
    }
}
