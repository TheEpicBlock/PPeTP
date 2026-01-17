package nl.theepicblock.ppetp.test;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.visitor.StringNbtWriter;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;

public class Util {
    public static boolean nbtContains(NbtElement c, String str) {
        var writer = new StringNbtWriter();
        c.accept(writer);
        return writer.getString().contains(str);
    }

    public static NbtCompound readNbtResource(String name) throws Exception {
        try (var resource = DataFixerTest.class.getResourceAsStream(name)) {
            assert resource != null;
            var str = IOUtils.toString(resource, StandardCharsets.UTF_8);
            return StringNbtReader.readCompound(str);
        }
    }
}
