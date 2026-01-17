package nl.theepicblock.ppetp.mixin;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import nl.theepicblock.ppetp.PPeTPDataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

@Mixin(DataFixerBuilder.class)
public abstract class AddFixerTo4661 {
    @Shadow
    public abstract void addFixer(DataFix fix);

    @Inject(method = "addSchema(ILjava/util/function/BiFunction;)Lcom/mojang/datafixers/schemas/Schema;", at = @At("RETURN"))
    private void onAdd(int version, BiFunction<Integer,Schema,Schema> factory, CallbackInfoReturnable<Schema> cir) {
        if (version == 4661) {
            var schema = cir.getReturnValue();
            this.addFixer(new PPeTPDataFixer(schema, false));
        }
    }
}
