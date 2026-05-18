package nl.theepicblock.ppetp.test;

import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

public class Util {
    public static boolean nbtContains(Tag c, String str) {
        var writer = new StringTagVisitor();
        c.accept(writer);
        return writer.build().contains(str);
    }

    public static CompoundTag readNbtResource(String name) throws Exception {
        try (var resource = DataFixerTest.class.getResourceAsStream(name)) {
            assert resource != null;
            var str = IOUtils.toString(resource, StandardCharsets.UTF_8);
            return TagParser.parseCompoundFully(str);
        }
    }
}
