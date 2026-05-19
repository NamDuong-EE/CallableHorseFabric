package club.someoneice.callablehorse.core;

import club.someoneice.callablehorse.api.IHorseFeatureAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;

public final class HorseFeatures {
    public static final ResourceLocation SCENIC_SPEED_MODIFIER_ID = CallableHorseFabric.id("scenic_ride_speed");
    public static final String CUSTOM_CHEST = "callable_horse_custom_chest";
    public static final String CUSTOM_CHEST_ITEMS = "callable_horse_custom_chest_items";
    public static final String SCENIC_RIDE = "callable_horse_scenic_ride";
    public static final int CHEST_COLUMNS = 5;
    public static final float SCENIC_SPEED_MULTIPLIER = 0.35F;

    private HorseFeatures() {
    }

    public static boolean canUseCustomChest(AbstractHorse horse) {
        return horse instanceof Horse;
    }

    public static boolean hasCustomChest(AbstractHorse horse) {
        return canUseCustomChest(horse)
                && horse instanceof IHorseFeatureAccess access
                && access.callableHorse$hasCustomChest();
    }

    public static void setCustomChest(AbstractHorse horse, boolean value) {
        if (!canUseCustomChest(horse)) return;

        if (horse instanceof IHorseFeatureAccess access) access.callableHorse$setCustomChest(value);
    }

    public static boolean isScenicRide(AbstractHorse horse) {
        return horse.getCompoundTag().getBoolean(SCENIC_RIDE);
    }

    public static void setScenicRide(AbstractHorse horse, boolean value) {
        if (value) {
            horse.getCompoundTag().putBoolean(SCENIC_RIDE, true);
        } else {
            horse.getCompoundTag().remove(SCENIC_RIDE);
        }
        applyScenicRideSpeed(horse);
    }

    public static boolean toggleScenicRide(AbstractHorse horse) {
        boolean next = !isScenicRide(horse);
        setScenicRide(horse, next);
        return next;
    }

    public static void applyScenicRideSpeed(AbstractHorse horse) {
        var speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        speed.removeModifier(SCENIC_SPEED_MODIFIER_ID);
        if (isScenicRide(horse)) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    SCENIC_SPEED_MODIFIER_ID,
                    SCENIC_SPEED_MULTIPLIER - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}
