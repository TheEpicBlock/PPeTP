package nl.theepicblock.ppetp.compat;

import com.lizin5ths.indypets.util.IndyPetsUtil;
import net.minecraft.entity.Entity;

public class IndyPetsCompat {

    public static boolean isIndependent(Entity entity) {
        return IndyPetsUtil.isActiveIndependent(entity);
    }
}
