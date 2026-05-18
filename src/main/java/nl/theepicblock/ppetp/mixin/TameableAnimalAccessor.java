package nl.theepicblock.ppetp.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TamableAnimal.class)
public interface TameableAnimalAccessor {
    @Invoker
    boolean invokeCanTeleportTo(BlockPos pos);
}
