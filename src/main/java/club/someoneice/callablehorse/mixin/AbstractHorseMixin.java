package club.someoneice.callablehorse.mixin;

import club.someoneice.callablehorse.api.IHorseFeatureAccess;
import club.someoneice.callablehorse.core.CallableHorseFabric;
import club.someoneice.callablehorse.core.HorseFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin implements IHorseFeatureAccess {
    @Unique private static final int CALLABLE_HORSE_CUSTOM_CHEST_FLAG = 128;

    @Shadow protected SimpleContainer inventory;

    @Shadow protected abstract void createInventory();

    @Shadow protected abstract boolean getFlag(int flag);

    @Shadow protected abstract void setFlag(int flag, boolean value);

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    public void callableHorse$equipChest(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (!HorseFeatures.canUseCustomChest(horse)
                || horse.isBaby()
                || !horse.isTamed()
                || HorseFeatures.hasCustomChest(horse)
                || !stack.is(Items.CHEST)) return;

        HorseFeatures.setCustomChest(horse, true);
        horse.playSound(SoundEvents.DONKEY_CHEST, 1.0F, (horse.getRandom().nextFloat() - horse.getRandom().nextFloat()) * 0.2F + 1.0F);
        stack.consume(1, player);
        this.createInventory();
        callableHorse$saveHorseToPlayer(player, horse);
        cir.setReturnValue(InteractionResult.sidedSuccess(horse.level().isClientSide));
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    public void callableHorse$addSecondRider(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (!horse.isVehicle()
                || !horse.isTamed()
                || !horse.isSaddled()
                || horse.getPassengers().size() != 1
                || horse.hasPassenger(player)
                || player.isSecondaryUseActive()) return;

        if (!horse.level().isClientSide) player.startRiding(horse);
        cir.setReturnValue(InteractionResult.sidedSuccess(horse.level().isClientSide));
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    public void onPlayerUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        callableHorse$saveHorseToPlayer(player, horse);
    }

    @Inject(method = "getInventoryColumns", at = @At("HEAD"), cancellable = true)
    public void callableHorse$getInventoryColumns(CallbackInfoReturnable<Integer> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (HorseFeatures.hasCustomChest(horse)) cir.setReturnValue(HorseFeatures.CHEST_COLUMNS);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void callableHorse$saveChestInventory(CompoundTag compound, CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (!HorseFeatures.hasCustomChest(horse)) return;

        compound.putBoolean(HorseFeatures.CUSTOM_CHEST, true);
        ListTag items = new ListTag();
        for (int slot = 1; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack.isEmpty()) continue;

            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) (slot - 1));
            items.add(stack.save(horse.registryAccess(), itemTag));
        }
        compound.put(HorseFeatures.CUSTOM_CHEST_ITEMS, items);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void callableHorse$loadChestInventory(CompoundTag compound, CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        HorseFeatures.applyScenicRideSpeed(horse);
        if (!HorseFeatures.canUseCustomChest(horse)) return;

        HorseFeatures.setCustomChest(horse, compound.getBoolean(HorseFeatures.CUSTOM_CHEST));
        if (!HorseFeatures.hasCustomChest(horse)) return;

        this.createInventory();
        ListTag items = compound.getList(HorseFeatures.CUSTOM_CHEST_ITEMS, 10);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = (itemTag.getByte("Slot") & 255) + 1;
            if (slot >= this.inventory.getContainerSize()) continue;

            this.inventory.setItem(slot, ItemStack.parse(horse.registryAccess(), itemTag).orElse(ItemStack.EMPTY));
        }
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    public void callableHorse$dropCustomChest(CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (!HorseFeatures.hasCustomChest(horse)) return;

        if (!horse.level().isClientSide) horse.spawnAtLocation(Blocks.CHEST);
        HorseFeatures.setCustomChest(horse, false);
    }

    protected boolean canAddPassenger(Entity passenger) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (horse.isSaddled() && horse.isTamed()) return horse.getPassengers().size() < 2;
        return horse.getPassengers().isEmpty();
    }

    @Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
    public void callableHorse$getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale, CallbackInfoReturnable<Vec3> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (horse.getPassengers().indexOf(passenger) != 1) return;

        Vec3 rearSeatOffset = new Vec3(0.0D, 0.0D, -0.65D).yRot(-horse.getYRot() * ((float) Math.PI / 180.0F));
        cir.setReturnValue(cir.getReturnValue().add(rearSeatOffset));
    }

    @Override
    public boolean callableHorse$hasCustomChest() {
        return this.getFlag(CALLABLE_HORSE_CUSTOM_CHEST_FLAG);
    }

    @Override
    public void callableHorse$setCustomChest(boolean value) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        this.setFlag(CALLABLE_HORSE_CUSTOM_CHEST_FLAG, value);
        if (value) {
            horse.getCompoundTag().putBoolean(HorseFeatures.CUSTOM_CHEST, true);
        } else {
            horse.getCompoundTag().remove(HorseFeatures.CUSTOM_CHEST);
            horse.getCompoundTag().remove(HorseFeatures.CUSTOM_CHEST_ITEMS);
        }
    }

    @Unique
    private void callableHorse$saveHorseToPlayer(Player player, AbstractHorse horse) {
        CompoundTag horseTag = new CompoundTag();
        horse.save(horseTag);
        String dimension = horse.level().dimension().location().toString();
        horse.getCompoundTag().putString(CallableHorseFabric.HORSE_DIMENSION, dimension);
        horseTag.putString(CallableHorseFabric.HORSE_DIMENSION, dimension);
        player.getCompoundTag().put("player_horse_nbt", horseTag);
        player.getCompoundTag().putString(CallableHorseFabric.PLAYER_HORSE_DIMENSION, dimension);
    }
}
