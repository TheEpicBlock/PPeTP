package nl.theepicblock.ppetp.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V1460;

// We're going to pretend that our `PPeTP` nbt field was added somewhere around the 1.13 snapshots
// This means minecraft will always take it into account
// Code "inspired" by https://github.com/emilyploszaj/trinkets/blob/main/src/main/java/dev/emi/trinkets/mixin/datafixer/Schema1460Mixin.java
@Mixin(V1460.class)
public abstract class AddNbtToSchema extends NamespacedSchema {
    @Unique
    private static Schema SCHEMA;

    public AddNbtToSchema(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    @Inject(method = "registerTypes", at = @At("HEAD"))
    private void captureSchema(Schema schemax, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes, CallbackInfo ci) {
        SCHEMA = schemax;
    }

    @ModifyReturnValue(method = "lambda$registerTypes$2", at = @At("RETURN"))
    private static TypeTemplate onRegister(TypeTemplate original) {
        return DSL.allWithRemainder(
                DSL.optional(DSL.field("PPeTP", DSL.list(
                        DSL.or(
                                // This is the new way of storing things
                                DSL.field("data", References.ENTITY_TREE.in(SCHEMA)),
                                // But in previous versions of the mod the type was inserted directly
                                References.ENTITY_TREE.in(SCHEMA)
                        )
                ))),
                original
        );
    }
}
