package nl.theepicblock.ppetp.mixin;

import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TameableEntity.class)
public interface TameableEntityAccessor {
    @Invoker
    boolean invokeCanTeleportTo(BlockPos pos);
}
