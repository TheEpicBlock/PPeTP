package nl.theepicblock.ppetp.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.entity.mob.MobEntity.class)
public interface MobEntityAccessor {
    @Accessor
    GoalSelector getGoalSelector();
}
