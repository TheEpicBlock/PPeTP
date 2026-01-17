package nl.theepicblock.ppetp.test;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.Schemas;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.nbt.*;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static nl.theepicblock.ppetp.test.Util.nbtContains;
import static nl.theepicblock.ppetp.test.Util.readNbtResource;

/**
 * Checks if entities inside of player data are datafixed correctly.
 * Eg, if mojang changes one of the fields of entities, are those updated correctly in the player data
 */
public class DataFixerTest {
    static int OLD_VERSION = 3094;
    static String OLD_NAME = "minecraft:british";
    static int NEW_VERSION = 3098;
    static String NEW_NAME = "minecraft:british_shorthair";

    @ParameterizedTest
    @ValueSource(strings = {"/old_format.snbt", "/new_format.snbt"})
    void checkUpdate(String filename) throws Exception {
        // ppetp used to use a format where the pets array directly contained a list of entities.
        // Now it contains { data: <entity_data>, dimension: <string> }
        // This test tests the former format

        // Setup nbt
        SharedConstants.createGameVersion();
        var oldFormatNbt = readNbtResource(filename);
        // Sanity check
        Assertions.assertTrue(nbtContains(oldFormatNbt, OLD_NAME));
        Assertions.assertFalse(nbtContains(oldFormatNbt, NEW_NAME));

        // Conversion
        var dyn = new Dynamic<>(NbtOps.INSTANCE, oldFormatNbt);
        var newNbt = Schemas.getFixer().update(TypeReferences.PLAYER, dyn, OLD_VERSION, NEW_VERSION).cast(NbtOps.INSTANCE);

        // Check if it was converted correctly
        Assertions.assertTrue(nbtContains(newNbt, NEW_NAME));
    }
}
