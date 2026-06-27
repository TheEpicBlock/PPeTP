package nl.theepicblock.ppetp.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.theepicblock.ppetp.PlayerDuck;
import nl.theepicblock.ppetp.PlayerPetStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class AddStorageToServerPlayer implements PlayerDuck {
    @Unique
    private PlayerPetStorage petStorage = new PlayerPetStorage();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.petStorage.tick(((ServerPlayer)(Object)this));
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void onCopy(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        this.petStorage = ((PlayerDuck)oldPlayer).PPeTP$getStorage();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void onWriteNbt(ValueOutput output, CallbackInfo ci) {
        petStorage.writePlayerData(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onReadNbt(ValueInput input, CallbackInfo ci) {
        petStorage.readPlayerData(input, ((ServerPlayer)(Object)this));
    }

    @Override
    public PlayerPetStorage PPeTP$getStorage() {
        return this.petStorage;
    }
}
