package nl.theepicblock.ppetp.mixin;

import nl.theepicblock.ppetp.PPeTP;
import nl.theepicblock.ppetp.PetTeleporter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;

@Mixin(PersistentEntitySectionManager.class)
public abstract class EntityUnloadListener {
    @Shadow @Final
    private EntitySectionStorage<@NotNull EntityAccess> sectionStorage;

    @Inject(method = "updateChunkStatus(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/entity/Visibility;)V", at = @At("HEAD"))
    private void onUnload(ChunkPos pos, Visibility chunkStatus, CallbackInfo ci) {
        try {
            if (!chunkStatus.isTicking()) {
                var l = pos.pack();
                var sections = this.sectionStorage.getExistingSectionsInChunk(l);
                var petsToCheck = new ArrayList<TamableAnimal>();
                sections.forEach(section -> {
                    section.getEntities().forEach(e -> {
                        if (e instanceof TamableAnimal pet) {
                            petsToCheck.add(pet);
                        }
                    });
                });

                for (var pet : petsToCheck) {
                    PetTeleporter.petAlmostUnloaded(pet);
                }
            }
        } catch (Exception e) {
            PPeTP.LOGGER.error("Error processing chunk unload", e);
        }
    }
}
