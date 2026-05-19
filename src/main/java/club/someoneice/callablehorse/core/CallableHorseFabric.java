package club.someoneice.callablehorse.core;

import club.someoneice.callablehorse.mixin.AbstractHorseAccess;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.UUID;

public class CallableHorseFabric implements ModInitializer {
    public static final String MODID = "callablehorsefabric";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static int rangeCanCall = 0;
    public static boolean canRespawnHorse = true;
    public static boolean canCallFromOtherWorld = true;
    private static final double CALL_START_DISTANCE = 16.0D;
    private static final double CALL_TARGET_DISTANCE = 2.5D;
    private static final double CALL_RUN_STEP = 0.45D;
    private static final double CALL_RUN_FALLBACK_SPEED = 1.6D;
    private static final double CALL_RUN_REACHED_DISTANCE = 1.25D;
    private static final int CALL_RUN_STUCK_LIMIT = 15;
    private static final String CALL_RUN_ACTIVE = "callable_horse_straight_run";
    private static final String CALL_RUN_FALLBACK = "callable_horse_pathfinding_run";
    private static final String CALL_RUN_STUCK_TICKS = "callable_horse_run_stuck_ticks";
    private static final String CALL_RUN_REPATH_TICKS = "callable_horse_run_repath_ticks";
    private static final String CALL_RUN_TARGET_X = "callable_horse_run_target_x";
    private static final String CALL_RUN_TARGET_Y = "callable_horse_run_target_y";
    private static final String CALL_RUN_TARGET_Z = "callable_horse_run_target_z";

    // C2S
    public static final ResourceLocation CALL_HORSE_PACKAGE = id("call_horse_key");
    public static final ResourceLocation SET_HORSE_PACKAGE = id("set_horse_key");
   //  public static final ResourceLocation STATE_HORSE_PACKAGE = id("state_horse_key");

    public WorldHorseData data;

    public static final SoundEvent whistle = SoundEvent.createVariableRangeEvent(id("whistle"));

    @Override
    public void onInitialize() {
        new Config();

        PayloadTypeRegistry.playC2S().register(CallHorsePayload.ID, CallHorsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetHorsePayload.ID, SetHorsePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CallHorsePayload.ID, (payload, context) -> onCallHorse(context.server(), context.player()));
        ServerPlayNetworking.registerGlobalReceiver(SetHorsePayload.ID, (payload, context) -> onSetHorse(context.player()));
        // ServerPlayNetworking.registerGlobalReceiver(STATE_HORSE_PACKAGE, CallableHorseFabric::checkHorseState);

        Registry.register(BuiltInRegistries.SOUND_EVENT, whistle.getLocation(), whistle);

        ServerLifecycleEvents.SERVER_STARTED.register(it -> this.data = WorldHorseData.getServerState(it));

        ServerTickEvents.END_WORLD_TICK.register(CallableHorseFabric::tickCalledHorses);

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (this.data == null) return;
            if (this.data.horseShouldKill.isEmpty()) return;

            for (var entity : world.getAllEntities()) {
                if (!(entity instanceof AbstractHorse horse)) return;

                var nbt = horse.getCompoundTag();
                if (!nbt.contains("player_horse_UUID")) return;
                String uuid = nbt.getString("player_horse_UUID");
                if (!this.data.horseShouldRespawn.contains(uuid) && this.data.horseShouldKill.contains(uuid)) horse.kill();

                this.data.horseShouldKill.remove(uuid);
            }
        });


        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (this.data == null) return;
            if (this.data.horseShouldKill.isEmpty()) return;

            for (var entity : world.getAllEntities()) {
                if (!(entity instanceof AbstractHorse horse)) return;

                var nbt = horse.getCompoundTag();
                if (!nbt.contains("player_horse_UUID")) return;
                String uuid = nbt.getString("player_horse_UUID");
                if (!this.data.horseShouldRespawn.contains(uuid) && this.data.horseShouldKill.contains(uuid)) horse.kill();

                this.data.horseShouldKill.remove(uuid);
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public record CallHorsePayload() implements CustomPacketPayload {
        public static final CallHorsePayload INSTANCE = new CallHorsePayload();
        public static final Type<CallHorsePayload> ID = new Type<>(CALL_HORSE_PACKAGE);
        public static final StreamCodec<RegistryFriendlyByteBuf, CallHorsePayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record SetHorsePayload() implements CustomPacketPayload {
        public static final SetHorsePayload INSTANCE = new SetHorsePayload();
        public static final Type<SetHorsePayload> ID = new Type<>(SET_HORSE_PACKAGE);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetHorsePayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    private static void onCallHorse(MinecraftServer server, ServerPlayer player) {
        playWhistle(player);

        var nbt = player.getCompoundTag();

        if (!nbt.contains("player_horse_UUID")) {
            player.displayClientMessage(Component.translatable("no_horse_can_call.callablehorse.info").withStyle(ChatFormatting.RED), true);
            return;
        }

        String uuid = nbt.getString("player_horse_UUID");
        var data = WorldHorseData.getServerState((((ServerLevel) player.level()).getServer()));

        if (canRespawnHorse && data.horseShouldRespawn.contains(uuid)) {
            if (!respawnAndCallHorse(player)) return;
            data.horseShouldRespawn.remove(uuid);
            player.displayClientMessage(Component.translatable("success_call_house.callablehorse.info").withStyle(ChatFormatting.GREEN), true);
            return;

        }

        if (rangeCanCall > 0) {
            for (var horse : player.level().getEntitiesOfClass(AbstractHorse.class, new AABB(player.getX() - rangeCanCall, player.getY() - rangeCanCall, player.getZ() - rangeCanCall, player.getX() + rangeCanCall, player.getY() + rangeCanCall, player.getZ() + rangeCanCall))) {
                String uuidHorse = horse.getCompoundTag().getString("player_horse_UUID");
                if (!uuid.equals(uuidHorse)) continue;
                callHorse(horse, player);
            }
        } else if (canCallFromOtherWorld) {
            List<Entity> entities = Lists.newArrayList();

            for (ServerLevel w : server.getAllLevels()) entities.addAll(ImmutableList.copyOf(w.getAllEntities()));
            for (var entity : entities) {
                if (!(entity instanceof AbstractHorse horse)) continue;
                String uuidHorse = horse.getCompoundTag().getString("player_horse_UUID");
                if (!uuid.equals(uuidHorse)) continue;
                callHorse(horse, player);
                return;
            }

            if (!callFromUnloadAndCallHorse(player))
                player.displayClientMessage(Component.translatable("horse_cannot_call.callablehorse.info").withStyle(ChatFormatting.RED), true);

            data.horseShouldKill.add(uuid);
        } else {
            for (var entity : ((ServerLevel) player.level()).getAllEntities()) {
                if (!(entity instanceof AbstractHorse horse)) continue;
                String uuidHorse = horse.getCompoundTag().getString("player_horse_UUID");
                if (!uuid.equals(uuidHorse)) continue;
                callHorse(horse, player);
                return;
            }

            if (!callFromUnloadAndCallHorse(player))
                player.displayClientMessage(Component.translatable("horse_cannot_call.callablehorse.info").withStyle(ChatFormatting.RED), true);

            data.horseShouldKill.add(uuid);
        }

        player.displayClientMessage(Component.translatable("success_call_house.callablehorse.info").withStyle(ChatFormatting.GREEN), true);
    }

    private static void callHorse(AbstractHorse horse, ServerPlayer player) {
        horse.ejectPassengers();
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = findCallPosition(level, player, -CALL_START_DISTANCE);
        Vec3 target = findCallPosition(level, player, CALL_TARGET_DISTANCE);
        horse.teleportTo(level, start.x(), start.y(), start.z(), null, player.getYRot(), horse.getXRot());
        startStraightRun(horse, target);
        player.displayClientMessage(Component.translatable("success_call_house.callablehorse.info").withStyle(ChatFormatting.GREEN), true);
    }

    private static void playWhistle(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), whistle, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static Vec3 findCallPosition(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
        if (horizontalLook.lengthSqr() < 1.0E-4D) horizontalLook = Vec3.directionFromRotation(0.0F, player.getYRot());
        horizontalLook = horizontalLook.normalize();

        Vec3 target = player.position().add(horizontalLook.scale(distance));
        BlockPos base = BlockPos.containing(target.x(), player.getY(), target.z());

        int top = Math.min(level.getMaxBuildHeight() - 2, base.getY() + 8);
        int bottom = Math.max(level.getMinBuildHeight() + 1, base.getY() - 8);
        for (int y = top; y >= bottom; y--) {
            BlockPos candidate = new BlockPos(base.getX(), y, base.getZ());
            if (canHorseStandAt(level, candidate)) return Vec3.atBottomCenterOf(candidate);
        }

        return player.position().add(horizontalLook.scale(distance));
    }

    private static boolean canHorseStandAt(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockPos above = pos.above();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }

    private static void spawnHorseBehindPlayer(AbstractHorse horse, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = findCallPosition(level, player, -CALL_START_DISTANCE);
        Vec3 target = findCallPosition(level, player, CALL_TARGET_DISTANCE);
        horse.moveTo(start.x(), start.y(), start.z(), player.getYRot(), horse.getXRot());
        level.addFreshEntity(horse);
        startStraightRun(horse, target);
    }

    private static void startStraightRun(AbstractHorse horse, Vec3 target) {
        var followRange = horse.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && followRange.getBaseValue() < 64.0D) followRange.setBaseValue(64.0D);

        horse.getNavigation().stop();
        horse.setNoAi(true);

        CompoundTag tag = horse.getCompoundTag();
        tag.putBoolean(CALL_RUN_ACTIVE, true);
        tag.putBoolean(CALL_RUN_FALLBACK, false);
        tag.putInt(CALL_RUN_STUCK_TICKS, 0);
        tag.putInt(CALL_RUN_REPATH_TICKS, 0);
        tag.putDouble(CALL_RUN_TARGET_X, target.x());
        tag.putDouble(CALL_RUN_TARGET_Y, target.y());
        tag.putDouble(CALL_RUN_TARGET_Z, target.z());
    }

    private static void tickCalledHorses(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AbstractHorse horse) tickStraightRun(level, horse);
        }
    }

    private static void tickStraightRun(ServerLevel level, AbstractHorse horse) {
        CompoundTag tag = horse.getCompoundTag();
        if (!tag.getBoolean(CALL_RUN_ACTIVE)) return;

        Vec3 target = new Vec3(tag.getDouble(CALL_RUN_TARGET_X), tag.getDouble(CALL_RUN_TARGET_Y), tag.getDouble(CALL_RUN_TARGET_Z));
        Vec3 current = horse.position();
        double dx = target.x() - current.x();
        double dz = target.z() - current.z();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (tag.getBoolean(CALL_RUN_FALLBACK)) {
            tickPathfindingRun(horse, target, horizontalDistance);
            return;
        }

        if (horizontalDistance <= CALL_RUN_REACHED_DISTANCE) {
            Vec3 stop = findGroundedRunPosition(level, target.x(), target.y(), target.z());
            if (stop != null) horse.moveTo(stop.x(), stop.y(), stop.z(), horse.getYRot(), horse.getXRot());
            stopStraightRun(horse);
            return;
        }

        double stepX = dx / horizontalDistance * CALL_RUN_STEP;
        double stepZ = dz / horizontalDistance * CALL_RUN_STEP;
        Vec3 next = findGroundedRunPosition(level, current.x() + stepX, current.y(), current.z() + stepZ);
        if (next == null || !canHorseMoveTo(level, horse, next)) {
            markStraightRunBlocked(horse, target);
            return;
        }

        float yaw = (float) (Math.atan2(-stepX, stepZ) * 180.0D / Math.PI);
        horse.moveTo(next.x(), next.y(), next.z(), yaw, horse.getXRot());
        horse.setYHeadRot(yaw);
        horse.setYBodyRot(yaw);
        tag.putInt(CALL_RUN_STUCK_TICKS, 0);
    }

    private static Vec3 findGroundedRunPosition(ServerLevel level, double x, double nearY, double z) {
        BlockPos base = BlockPos.containing(x, nearY, z);
        int top = Math.min(level.getMaxBuildHeight() - 2, base.getY() + 3);
        int bottom = Math.max(level.getMinBuildHeight() + 1, base.getY() - 4);

        for (int y = top; y >= bottom; y--) {
            BlockPos candidate = new BlockPos(base.getX(), y, base.getZ());
            if (canHorseStandAt(level, candidate)) return new Vec3(x, y, z);
        }

        return null;
    }

    private static boolean canHorseMoveTo(ServerLevel level, AbstractHorse horse, Vec3 next) {
        Vec3 movement = next.subtract(horse.position());
        return level.noCollision(horse, horse.getBoundingBox().move(movement));
    }

    private static void markStraightRunBlocked(AbstractHorse horse, Vec3 target) {
        CompoundTag tag = horse.getCompoundTag();
        int stuckTicks = tag.getInt(CALL_RUN_STUCK_TICKS) + 1;
        tag.putInt(CALL_RUN_STUCK_TICKS, stuckTicks);

        if (stuckTicks >= CALL_RUN_STUCK_LIMIT) startPathfindingRun(horse, target);
    }

    private static void startPathfindingRun(AbstractHorse horse, Vec3 target) {
        CompoundTag tag = horse.getCompoundTag();
        tag.putBoolean(CALL_RUN_FALLBACK, true);
        tag.putInt(CALL_RUN_REPATH_TICKS, 0);

        horse.setNoAi(false);
        horse.getNavigation().stop();
        horse.getNavigation().moveTo(target.x(), target.y(), target.z(), CALL_RUN_FALLBACK_SPEED);
    }

    private static void tickPathfindingRun(AbstractHorse horse, Vec3 target, double horizontalDistance) {
        if (horizontalDistance <= CALL_RUN_REACHED_DISTANCE) {
            stopStraightRun(horse);
            return;
        }

        CompoundTag tag = horse.getCompoundTag();
        int repathTicks = tag.getInt(CALL_RUN_REPATH_TICKS);
        if (repathTicks <= 0 || horse.getNavigation().isDone()) {
            horse.getNavigation().moveTo(target.x(), target.y(), target.z(), CALL_RUN_FALLBACK_SPEED);
            tag.putInt(CALL_RUN_REPATH_TICKS, 20);
        } else {
            tag.putInt(CALL_RUN_REPATH_TICKS, repathTicks - 1);
        }
    }

    private static void stopStraightRun(AbstractHorse horse) {
        CompoundTag tag = horse.getCompoundTag();
        tag.remove(CALL_RUN_ACTIVE);
        tag.remove(CALL_RUN_FALLBACK);
        tag.remove(CALL_RUN_STUCK_TICKS);
        tag.remove(CALL_RUN_REPATH_TICKS);
        tag.remove(CALL_RUN_TARGET_X);
        tag.remove(CALL_RUN_TARGET_Y);
        tag.remove(CALL_RUN_TARGET_Z);
        horse.setNoAi(false);
        horse.getNavigation().stop();
        horse.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean respawnAndCallHorse(ServerPlayer player) {
        var nbt = player.getCompoundTag().getCompound("player_horse_nbt");
        var deadHorse = EntityType.by(nbt).get().create(player.level());
        if (!(deadHorse instanceof AbstractHorse horse)) return false;
        horse.load(nbt);
        ((AbstractHorseAccess) horse).getInventory().clearContent();
        horse.getCompoundTag().putString("player_horse_UUID", player.getCompoundTag().getString("player_horse_UUID"));
        spawnHorseBehindPlayer(horse, player);
        return true;
    }

    private static boolean callFromUnloadAndCallHorse(ServerPlayer player) {
        var nbt = player.getCompoundTag().getCompound("player_horse_nbt");
        var deadHorse = EntityType.by(nbt).get().create(player.level());
        if (!(deadHorse instanceof AbstractHorse horse)) return false;
        horse.load(nbt);
        spawnHorseBehindPlayer(horse, player);

        String uuid = UUID.randomUUID().toString();
        horse.getCompoundTag().putString("player_horse_UUID", uuid);
        player.getCompoundTag().putString("player_horse_UUID", uuid);

        CompoundTag horseTag = new CompoundTag();
        horse.save(horseTag);
        player.getCompoundTag().put("player_horse_nbt", horseTag);
        player.getCompoundTag().putString("player_horse_type", horse.getType().toString());

        return true;
    }

    private static void onSetHorse(ServerPlayer player) {
        var entity = player.getVehicle();
        if (!(entity instanceof AbstractHorse horse)) {
            player.displayClientMessage(Component.translatable("no_horse_can_set.callablehorse.info").withStyle(ChatFormatting.RED), true);
            return;
        }

        String uuid = UUID.randomUUID().toString();
        horse.getCompoundTag().putString("player_horse_UUID", uuid);
        player.getCompoundTag().putString("player_horse_UUID", uuid);

        CompoundTag horseTag = new CompoundTag();
        horse.save(horseTag);
        player.getCompoundTag().put("player_horse_nbt", horseTag);
        player.getCompoundTag().putString("player_horse_type", horse.getType().toString());
        player.displayClientMessage(Component.translatable("success_set_house.callablehorse.info").withStyle(ChatFormatting.GREEN), true);
    }

    /*
    private static void checkHorseState(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        var nbt = player.getCompoundTag();
        if (!nbt.contains("player_horse_UUID")) {
            player.displayClientMessage(Component.translatable("no_horse_can_call.callablehorse.info").withStyle(ChatFormatting.RED), true);
            return;
        }

        byte[] bytes = nbt.getCompound("player_horse_nbt").toString().getBytes();

        FriendlyByteBuf buff = PacketByteBufs.create();
        buff.setInt(0, bytes.length);
        buff.setBytes(0, bytes);

        ServerPlayNetworking.send(player, CallableHorseFabricClient.OPEN_GUI, buff);
    }
    */
}
