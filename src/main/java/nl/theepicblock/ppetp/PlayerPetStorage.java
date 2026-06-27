package nl.theepicblock.ppetp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.theepicblock.ppetp.mixin.EntityAccessor;
import nl.theepicblock.ppetp.mixin.TameableAnimalAccessor;
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
    private List<Pair<@Nullable TamableAnimal, PetEntry>> entitydatas = new ArrayList<>();
    private boolean verified = false;

    public void tick(ServerPlayer owner) {
        if (owner.level() == null) return;

        var world = owner.level();
        if (!verified && world.getServer() != null) {
            this.entitydatas.replaceAll(entry -> Pair.of(entry.left(), entry.right().verified(world.getServer())));
            this.verified = true;
        }

        // Try to teleport them out!
        var iter = entitydatas.iterator();
        while (iter.hasNext()) {
            var pair = iter.next();
            if (!canExtractPet(owner, pair.right())) {
                continue;
            }
            Predicate<BlockPos> spotValidator;

            var e = pair.left();
            if (e != null) {
                ((EntityAccessor)e).invokeSetLevel(world);
                spotValidator = (pos) -> ((TameableAnimalAccessor)e).invokeCanTeleportTo(pos);
            } else {
                spotValidator = (pos) -> world.getBlockState(pos).isAir() &&
                        !world.getBlockState(pos.below()).getCollisionShape(world, pos.below()).isEmpty();
            }
            var spot = SpotFinder.findSpot(owner, spotValidator);
            if (spot != null) {
                if (dropEntityInWorld(owner.problemPath(), pair.right().data(), world, spot)) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * @return if the pet should be extracted at this current time
     */
    private boolean canExtractPet(ServerPlayer owner, PetEntry e) {
        if (owner.isSpectator()) {
            return false;
        }

        var gameRules = owner.level().getGameRules();
        if (!gameRules.get(PPeTP.SHOULD_TP_CROSS_DIMENSIONAL)) {
            // Maintain minecraft's rule of only teleporting into the same dimension
            if (e.sourceDimension.isPresent() && !Objects.equals(owner.level().dimension().identifier(), e.sourceDimension().get())) {
                return false;
            }
        }

        // No objections to trying to extract the pet
        return true;
    }

    private boolean dropEntityInWorld(ProblemReporter.PathElement errorReporterContext, CompoundTag data, ServerLevel world, BlockPos pos) {
        try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(errorReporterContext, LOGGER)) {
            var dataReadView = TagValueInput.create(logging.forChild(() -> ".ppetp"), world.registryAccess(), data);
            var optionalEntity = EntityType.create(dataReadView, world, new EntitySpawnRequest(EntitySpawnReason.LOAD, false));
            if (optionalEntity.isEmpty()) {
                return false;
            }

            var entity = optionalEntity.get();
            entity.setPos(Vec3.atBottomCenterOf(pos));
            return world.addWithUUID(entity);
        }
    }

    private Optional<Entity> readData(ProblemReporter.PathElement errorReporterContext, CompoundTag data, ServerLevel world) {
        try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(errorReporterContext, LOGGER)) {
            var dataReadView = TagValueInput.create(logging.forChild(() -> ".ppetp"), world.registryAccess(), data);
            return EntityType.create(dataReadView, world, new EntitySpawnRequest(EntitySpawnReason.LOAD, false));
        }
    }

    /**
     * Returns true if and only if the insertion was successful. Removing
     * the entity from the world is a responsibility of the caller.
     */
    public boolean insert(TamableAnimal entity) {
        try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput nbtWriteView = TagValueOutput.createWithContext(logging.forChild(() -> ".ppetp"), entity.registryAccess());

            // Serialize the entity to nbt. This will be the canonical representation
            var success = entity.saveAsPassenger(nbtWriteView);
            // Unable to save to nbt? Better abort to avoid data loss
            if (!success) return false;

            // Try to get the entity's dimension
            var world = entity.level();
            var dimensionId = world == null ? null : world.dimension().identifier();

            // Save the pet
            var petEntry = new PetEntry(Optional.ofNullable(dimensionId), nbtWriteView.buildResult());
            entitydatas.add(Pair.of(entity, petEntry));
            return true;
        }
    }

    public void writePlayerData(ValueOutput view) {
        var list = new ArrayList<PetEntry>(this.entitydatas.size());
        for (var pair : this.entitydatas) {
            list.add(pair.right());
        }
        view.store(KEY, CODEC, list);
    }

    public void readPlayerData(ValueInput view, ServerPlayer player) {
        var optList = view.read(KEY, CODEC);
        optList.ifPresent(list -> {
            this.entitydatas = new ArrayList<>(list.size());
            this.verified = false;
            var world = player.level();

            if (world == null) {
                list.forEach(e -> entitydatas.add(Pair.of(null, e)));
                return;
            }

            var errorCtx = player.problemPath();
            list.forEach(e -> {
                entitydatas.add(Pair.of(
                        readData(errorCtx, e.data(), world).orElse(null) instanceof TamableAnimal te ? te : null,
                        e)
                );
            });
        });
    }

    /// For testing purposes
    public int getNumberOfStoredPets() {
        return this.entitydatas.size();
    }

    private record PetEntry(Optional<Identifier> sourceDimension, CompoundTag data) {
        public static final Codec<PetEntry> CODEC = RecordCodecBuilder.create(petEntryInstance ->
                petEntryInstance.group(
                        Identifier.CODEC.optionalFieldOf("sourceDimension").forGetter(PetEntry::sourceDimension),
                        CompoundTag.CODEC.fieldOf("data").forGetter(PetEntry::data)
                ).apply(petEntryInstance, PetEntry::new));

        /**
         * Creates a {@link PetEntry} where the {@link #sourceDimension()} has been verified to exist against the
         * provided server. If the {@link #sourceDimension()} it will be replaced with {@link Optional#empty()} to
         * indicate the pet is from an unknown dimension.
         */
        private PetEntry verified(MinecraftServer server) {
            if (sourceDimension.isPresent() && server.getLevel(ResourceKey.create(Registries.DIMENSION, sourceDimension.get())) == null) {
                return new PetEntry(null, this.data);
            } else {
                return this;
            }
        }
    }
}
