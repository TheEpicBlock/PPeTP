package nl.theepicblock.ppetp.mixin;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingStatus;
import net.minecraft.world.entity.SectionedEntityCache;
import nl.theepicblock.ppetp.PetTeleporter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ServerEntityManager.class)
public abstract class EntityUnloadListener {
    @Shadow @Final
    SectionedEntityCache<EntityLike> cache;

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "updateTrackingStatus(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/entity/EntityTrackingStatus;)V", at = @At("HEAD"))
    private void onUnload(ChunkPos chunkPos, EntityTrackingStatus trackingStatus, CallbackInfo ci) {
        if (!trackingStatus.shouldTick()) {
            var l = chunkPos.toLong();
            var sections = this.cache.getTrackingSections(l);
            var petsToCheck = new ArrayList<TameableEntity>();
            sections.forEach(section -> {
                section.stream().forEach(e -> {
                    if (e instanceof TameableEntity pet) {
                        petsToCheck.add(pet);
                    }
                });
            });

            for (var pet : petsToCheck) {
                PetTeleporter.petAlmostUnloaded(pet);
            }
        }
    }
}
