package nl.theepicblock.ppetp.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import nl.theepicblock.ppetp.PPeTP;
import nl.theepicblock.ppetp.PetTeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public class TameableAnimalMixin {
    @Inject(method = "tryToTeleportToOwner", at = @At("RETURN"))
    private void onTeleport(CallbackInfo ci, @Local LivingEntity owner) {
        try {
            if (owner != null) {
                PetTeleporter.teleportPet((TamableAnimal)(Object)this, owner);
            }
        } catch (Exception e) {
            PPeTP.LOGGER.error("Failed to process pet's attempt to teleport", e);
        }
    }
}
