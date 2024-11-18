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
import java.util.Objects;
import java.util.function.Predicate;

public class PlayerPetStorage {
    public static final String KEY = "PPeTP";

    /**
     * The instances are kept around purely so functions can be run on them. We
     * reserialize them from nbt when they actually get put into the world.
     */
    private final List<Pair<@Nullable TameableEntity, PetEntry>> entitydatas = new ArrayList<>();

    public void tick(ServerPlayerEntity owner) {
        if (owner.getServerWorld() == null) return;
        var world = owner.getServerWorld();
        // Try to teleport them out!
        var iter = entitydatas.iterator();
        while (iter.hasNext()) {
            var pair = iter.next();
            if (!canExtractPet(owner, pair.getRight())) {
                continue;
            }
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
                if (dropEntityInWorld(pair.getRight().data(), world, spot)) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * @return if the pet should be extracted at this current time
     */
    private boolean canExtractPet(ServerPlayerEntity owner, PetEntry e) {
        // Maintain minecraft's rule of only teleporting into the same dimension
        return e.sourceDimension == null || Objects.equals(owner.getWorld().getDimensionEntry().getIdAsString(), e.sourceDimension());
    }

    private boolean dropEntityInWorld(NbtCompound data, ServerWorld world, BlockPos pos) {
        var optionalEntity = EntityType.getEntityFromNbt(data, world);
        if (optionalEntity.isEmpty()) {
            return false;
        }

        var entity = optionalEntity.get();
        entity.setPosition(pos.toBottomCenterPos());
        return world.tryLoadEntity(entity);
    }

    /**
     * Returns true if and only if the insertion was successful. Removing
     * the entity from the world is a responsibility of the caller.
     */
    public boolean insert(TameableEntity entity) {
        // Serialize the entity to nbt. This will be the canonical representation
        var data = new NbtCompound();
        var success = entity.saveNbt(data);
        // Unable to save to nbt? Better abort to avoid data loss
        if (!success) return false;

        // Try to get the entity's dimension
        var world = entity.getWorld();
        var dimensionId = world == null ? null : world.getDimensionEntry().getIdAsString();

        // Save the pet
        var petEntry = new PetEntry(dimensionId, data);
        entitydatas.add(new Pair<>(entity, petEntry));
        return true;
    }

    public NbtList write() {
        var list = new NbtList();
        entitydatas.forEach(pair -> list.add(pair.getRight().toPlayerNbt()));
        return list;
    }

    /**
     * @param world used for context on registries
     */
    public void read(NbtList data, ServerWorld world) {
        if (world == null) {
            data.forEach(e -> entitydatas.add(new Pair<>(null, PetEntry.fromPlayerNbt((NbtCompound)e))));
            return;
        }

        data.forEach(e -> {
            entitydatas.add(new Pair<>(
                    EntityType.getEntityFromNbt((NbtCompound)e, world).orElse(null) instanceof TameableEntity te ? te : null,
                    PetEntry.fromPlayerNbt((NbtCompound)e)
            ));
        });
    }

    public void writePlayerData(NbtCompound playerData) {
        playerData.put(KEY, this.write());
    }

    public void readPlayerData(NbtCompound playerData, ServerPlayerEntity player) {
        this.read(playerData.getList(KEY, NbtElement.COMPOUND_TYPE), player.getServerWorld());
    }

    private record PetEntry(@Nullable String sourceDimension, NbtCompound data) {
        public static PetEntry fromPlayerNbt(NbtCompound d) {
            if (!d.contains("sourceDimension") || !d.contains("data") || d.getKeys().size() > 6) {
                // This is likely still in the old format, where only entity data was stored without the dimension
                return new PetEntry(null, d);
            } else {
                var dim = d.getString("sourceDimension");
                if (Objects.equals(dim, "")) {
                    dim = null;
                }
                var data = d.getCompound("data");
                return new PetEntry(dim, data);
            }
        }

        public NbtCompound toPlayerNbt() {
            var comp = new NbtCompound();
            if (sourceDimension() != null) {
                comp.putString("sourceDimension", sourceDimension());
            } else {
                comp.putString("sourceDimension", "");
            }
            comp.put("data", data());
            return comp;
        }
    }
}
