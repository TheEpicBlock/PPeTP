package nl.theepicblock.ppetp.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import nl.theepicblock.ppetp.PlayerDuck;
import nl.theepicblock.ppetp.PlayerPetStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements PlayerDuck {
    @Unique
    private PlayerPetStorage petStorage = new PlayerPetStorage();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.petStorage.tick(((ServerPlayerEntity)(Object)this));
    }

    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void onCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.petStorage = ((PlayerDuck)oldPlayer).PPeTP$getStorage();
    }

    @Inject(method = "writeCustomData", at = @At("HEAD"))
    private void onWriteNbt(WriteView view, CallbackInfo ci) {
        petStorage.writePlayerData(view);
    }

    @Inject(method = "readCustomData", at = @At("HEAD"))
    private void onReadNbt(ReadView view, CallbackInfo ci) {
        petStorage.readPlayerData(view, ((ServerPlayerEntity)(Object)this));
    }

    @Override
    public PlayerPetStorage PPeTP$getStorage() {
        return this.petStorage;
    }
}
