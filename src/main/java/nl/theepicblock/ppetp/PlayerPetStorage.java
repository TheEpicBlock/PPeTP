package nl.theepicblock.ppetp;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import nl.theepicblock.ppetp.mixin.EntityAccessor;
import nl.theepicblock.ppetp.mixin.TameableEntityAccessor;
import org.jetbrains.annotations.NotNull;
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
        if (owner.isSpectator()) {
            return false;
        }

        var gameRules = owner.getServerWorld().getGameRules();
        if (!gameRules.getBoolean(PPeTP.SHOULD_TP_CROSS_DIMENSIONAL)) {
            // Maintain minecraft's rule of only teleporting into the same dimension
            if (e.sourceDimension != null && !Objects.equals(owner.getWorld().getRegistryKey().getValue(), e.sourceDimension())) {
                return false;
            }
        }

        // No objections to trying to extract the pet
        return true;
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
        var dimensionId = world == null ? null : world.getRegistryKey().getValue();

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
     * @param server server used for context on if certain worlds exist
     */
    public void read(NbtList data, ServerWorld world, MinecraftServer server) {
        if (world == null) {
            data.forEach(e -> entitydatas.add(new Pair<>(null, PetEntry.fromPlayerNbt((NbtCompound)e, server))));
            return;
        }

        data.forEach(e -> {
            entitydatas.add(new Pair<>(
                    EntityType.getEntityFromNbt((NbtCompound)e, world).orElse(null) instanceof TameableEntity te ? te : null,
                    PetEntry.fromPlayerNbt((NbtCompound)e, world.getServer())
            ));
        });
    }

    public void writePlayerData(NbtCompound playerData) {
        playerData.put(KEY, this.write());
    }

    public void readPlayerData(NbtCompound playerData, ServerPlayerEntity player) {
        this.read(playerData.getList(KEY, NbtElement.COMPOUND_TYPE), player.getServerWorld(), player.getServer());
    }

    private record PetEntry(@Nullable Identifier sourceDimension, NbtCompound data) {
        public static @NotNull PetEntry fromPlayerNbt(@NotNull NbtCompound d, @Nullable MinecraftServer server) {
            if (!d.contains("sourceDimension") || !d.contains("data") || d.getKeys().size() > 6) {
                // This is likely still in the old format, where only entity data was stored without the dimension
                return new PetEntry(null, d);
            } else {
                var dim = d.getString("sourceDimension");
                var dimId = Objects.equals(dim, "") ? null : Identifier.tryParse(dim);
                if (server == null || (dimId != null && server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId)) == null)) {
                    // This world no longer exists
                    dimId = null;
                }

                var data = d.getCompound("data");
                return new PetEntry(dimId, data);
            }
        }

        public @NotNull NbtCompound toPlayerNbt() {
            var comp = new NbtCompound();
            if (sourceDimension() != null) {
                comp.putString("sourceDimension", sourceDimension().toString());
            } else {
                comp.putString("sourceDimension", "");
            }
            comp.put("data", data());
            return comp;
        }
    }
}
