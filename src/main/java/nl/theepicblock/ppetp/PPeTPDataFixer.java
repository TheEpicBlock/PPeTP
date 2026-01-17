package nl.theepicblock.ppetp;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;

public class PPeTPDataFixer extends DataFix {
    public PPeTPDataFixer(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("FixPPeTPFormat",
                this.getInputSchema().getType(TypeReferences.PLAYER),
                (typed) ->
                    typed.update(DSL.remainderFinder(), dynamic ->
                        dynamic.update("PPeTP", field ->
                                field.createList(
                                        field.asList(dyn ->
                                                dyn.get("sourceDimension").result().isPresent() && dyn.get("data").result().isEmpty() ?
                                                        dyn :
                                                        dyn.createMap(Map.of(dyn.createString("data"), dyn))
                                        ).stream()
                                )
                        )
                    )
                );
    }
}
