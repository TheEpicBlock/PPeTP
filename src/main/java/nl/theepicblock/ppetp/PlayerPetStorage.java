package nl.theepicblock.ppetp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import nl.theepicblock.ppetp.mixin.EntityAccessor;
import nl.theepicblock.ppetp.mixin.TameableEntityAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static nl.theepicblock.ppetp.PPeTP.LOGGER;

public class PlayerPetStorage {
    public static final String KEY = "PPeTP";
    public static final Codec<List<PetEntry>> CODEC = Codec.list(PetEntry.CODEC);

    /**
     * The instances are kept around purely so functions can be run on them. We
     * reserialize them from nbt when they actually get put into the world.
     */
    private List<Pair<@Nullable TameableEntity, PetEntry>> entitydatas = new ArrayList<>();
    private boolean verified = false;

    public void tick(ServerPlayerEntity owner) {
        if (owner.getEntityWorld() == null) return;

        var world = owner.getEntityWorld();
        if (!verified && world.getServer() != null) {
            this.entitydatas.replaceAll(entry -> new Pair<>(entry.getLeft(), entry.getRight().verified(world.getServer())));
            this.verified = true;
        }

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
                if (dropEntityInWorld(owner.getErrorReporterContext(), pair.getRight().data(), world, spot)) {
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

        var gameRules = owner.getEntityWorld().getGameRules();
        if (!gameRules.getValue(PPeTP.SHOULD_TP_CROSS_DIMENSIONAL)) {
            // Maintain minecraft's rule of only teleporting into the same dimension
            if (e.sourceDimension != null && !Objects.equals(owner.getEntityWorld().getRegistryKey().getValue(), e.sourceDimension())) {
                return false;
            }
        }

        // No objections to trying to extract the pet
        return true;
    }

    private boolean dropEntityInWorld(ErrorReporter.Context errorReporterContext, NbtCompound data, ServerWorld world, BlockPos pos) {
        try (ErrorReporter.Logging logging = new ErrorReporter.Logging(errorReporterContext, LOGGER)) {
            var dataReadView = NbtReadView.create(logging.makeChild(() -> ".ppetp"), world.getRegistryManager(), data);
            var optionalEntity = EntityType.getEntityFromData(dataReadView, world, SpawnReason.LOAD);
            if (optionalEntity.isEmpty()) {
                return false;
            }

            var entity = optionalEntity.get();
            entity.setPosition(pos.toBottomCenterPos());
            return world.tryLoadEntity(entity);
        }
    }

    private Optional<Entity> readData(ErrorReporter.Context errorReporterContext, NbtCompound data, ServerWorld world) {
        try (ErrorReporter.Logging logging = new ErrorReporter.Logging(errorReporterContext, LOGGER)) {
            var dataReadView = NbtReadView.create(logging.makeChild(() -> ".ppetp"), world.getRegistryManager(), data);
            return EntityType.getEntityFromData(dataReadView, world, SpawnReason.LOAD);
        }
    }

    /**
     * Returns true if and only if the insertion was successful. Removing
     * the entity from the world is a responsibility of the caller.
     */
    public boolean insert(TameableEntity entity) {
        try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
            NbtWriteView nbtWriteView = NbtWriteView.create(logging.makeChild(() -> ".ppetp"), entity.getRegistryManager());

            // Serialize the entity to nbt. This will be the canonical representation
            var data = new NbtCompound();
            var success = entity.saveSelfData(nbtWriteView);
            // Unable to save to nbt? Better abort to avoid data loss
            if (!success) return false;

            // Try to get the entity's dimension
            var world = entity.getEntityWorld();
            var dimensionId = world == null ? null : world.getRegistryKey().getValue();

            // Save the pet
            var petEntry = new PetEntry(dimensionId, data);
            entitydatas.add(new Pair<>(entity, petEntry));
            return true;
        }
    }

    public void writePlayerData(WriteView view) {
        var list = new ArrayList<PetEntry>(this.entitydatas.size());
        for (var pair : this.entitydatas) {
            list.add(pair.getRight());
        }
        view.put(KEY, CODEC, list);
    }

    public void readPlayerData(ReadView view, ServerPlayerEntity player) {
        var optList = view.read(KEY, CODEC);
        optList.ifPresent(list -> {
            this.entitydatas = new ArrayList<>(list.size());
            this.verified = false;
            var world = player.getEntityWorld();

            if (world == null) {
                list.forEach(e -> entitydatas.add(new Pair<>(null, e)));
                return;
            }

            var errorCtx = player.getErrorReporterContext();
            list.forEach(e -> {
                entitydatas.add(new Pair<>(
                        readData(errorCtx, e.data(), world).orElse(null) instanceof TameableEntity te ? te : null,
                        e)
                );
            });
        });
    }

    private record PetEntry(@Nullable Identifier sourceDimension, NbtCompound data) {
        public static final Codec<PetEntry> CODEC = RecordCodecBuilder.create(petEntryInstance ->
                petEntryInstance.group(
                        Identifier.CODEC.fieldOf("sourceDimension").forGetter(PetEntry::sourceDimension),
                        NbtCompound.CODEC.fieldOf("data").forGetter(PetEntry::data)
                ).apply(petEntryInstance, PetEntry::new));

        private PetEntry verified(MinecraftServer server) {
            if (server.getWorld(RegistryKey.of(RegistryKeys.WORLD, sourceDimension)) == null) {
                return new PetEntry(null, this.data);
            } else {
                return this;
            }
        }
    }
}
