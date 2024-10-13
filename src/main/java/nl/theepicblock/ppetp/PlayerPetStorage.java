package nl.theepicblock.ppetp;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import nl.theepicblock.ppetp.mixin.EntityAccessor;
import nl.theepicblock.ppetp.mixin.TameableEntityAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PlayerPetStorage {
    public static final String KEY = "PPeTP";

    /**
     * The instances are kept around purely so functions can be run on them. We
     * reserialize them from nbt when they actually get put into the world.
     */
    private final List<Pair<@Nullable TameableEntity, NbtCompound>> entitydatas = new ArrayList<>();

    public void tick(ServerPlayerEntity owner) {
        if (owner.getServerWorld() == null) return;
        var world = owner.getServerWorld();
        // Try to teleport them out!
        var iter = entitydatas.iterator();
        while (iter.hasNext()) {
            var pair = iter.next();
            Predicate<BlockPos> spotValidator;

            var e = pair.getLeft();
            if (e != null) {
                ((EntityAccessor)e).invokeSetWorld(world);
                spotValidator = (pos) -> ((TameableEntityAccessor)e).invokeCanTeleportTo(pos);
            } else {
                spotValidator = (pos) -> world.getBlockState(pos).isAir() &&
                        !world.getBlockState(pos.down()).getCollisionShape(world, pos.down()).isEmpty();
            }
            var spot = SpotFinder.findSpot(owner, spotValidator);
            if (spot != null) {
                if (dropEntityInWorld(pair.getRight(), world, spot)) {
                    iter.remove();
                }
            }
        }
    }

    private boolean dropEntityInWorld(NbtCompound data, ServerWorld world, BlockPos pos) {
        var optionalEntity = EntityType.getEntityFromNbt(data, world);
        if (optionalEntity.isEmpty()) {
            return false;
        }

        var entity = optionalEntity.get();
        entity.setPosition(pos.toCenterPos());
        return world.tryLoadEntity(entity);
    }

    /**
     * Returns true if and only if the insertion was successful. Removing
     * the entity from the world is a responsibility of the caller.
     */
    public boolean insert(TameableEntity entity) {
        var data = new NbtCompound();
        var success = entity.saveNbt(data);
        if (!success) return false;
        entitydatas.add(new Pair<>(entity, data));
        return true;
    }

    public NbtList write() {
        var list = new NbtList();
        entitydatas.forEach(pair -> list.add(pair.getRight()));
        return list;
    }

    /**
     * @param world used for context on registries
     */
    public void read(NbtList data, ServerWorld world) {
        if (world == null) {
            data.forEach(e -> entitydatas.add(new Pair<>(null, (NbtCompound)e)));
            return;
        }

        data.forEach(e -> {
            entitydatas.add(new Pair<>(
                    EntityType.getEntityFromNbt((NbtCompound)e, world).orElse(null) instanceof TameableEntity te ? te : null,
                    (NbtCompound)e
            ));
        });
    }

    public void writePlayerData(NbtCompound playerData) {
        playerData.put(KEY, this.write());
    }

    public void readPlayerData(NbtCompound playerData, ServerPlayerEntity player) {
        this.read(playerData.getList(KEY, NbtElement.COMPOUND_TYPE), player.getServerWorld());
    }
}
