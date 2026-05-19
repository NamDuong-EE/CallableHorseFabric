package club.someoneice.callablehorse.client;

import club.someoneice.callablehorse.core.CallableHorseFabric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HorseInfoGUI extends Screen {
    private int xSize = 176;
    private int ySize = 138;

    private static final ResourceLocation TEXTURE = CallableHorseFabric.id("textures/gui/horse_stat_viewer.png");
    private final AbstractHorse horse;

    private final double speed;
    private final double jumpHeight;
    private final double health;
    private final double maxHealth;
    private final Vec3 lastPos;
    private final ResourceKey<Level> lastDim;

    private Minecraft mc = Minecraft.getInstance();

    public HorseInfoGUI(Player player, CompoundTag horseTag) {
        super(Component.literal("Horse Stat Viewer"));

        this.horse = new Horse(EntityType.HORSE, player.level());
        this.horse.load(horseTag);

        this.health = (Math.floor(horse.getHealth()));
        this.maxHealth = (Math.floor(horse.getMaxHealth() * 10) / 10);
        this.speed = (Math.floor(horse.getAttribute(Attributes.MOVEMENT_SPEED).getValue() * 100) / 10);
        var jumpStrength = horse.getAttribute(Attributes.JUMP_STRENGTH);
        this.jumpHeight = jumpStrength == null ? 0.0 : (Math.floor(jumpStrength.getValue() * 100) / 10);
        this.lastPos = horse.position();
        this.lastDim = horse.level().dimension();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);

        // GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        graphics.blit(TEXTURE, i, j, 0, 0, this.xSize, this.ySize, 256, 256);

        super.render(graphics, mouseX, mouseY, partialTicks);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, i + 18, j + 8, i + 68, j + 78, 25, 0.0625F, mouseX, mouseY, this.horse);

        graphics.drawString(mc.font, this.horse.getName(), i + 84, j + 10, DyeColor.WHITE.getTextColor());

        graphics.drawString(mc.font, "Health:", i + 84, j + 30, DyeColor.LIGHT_GRAY.getTextColor());
        graphics.drawString(mc.font, health + "/" + maxHealth, i + 120, j + 30, DyeColor.WHITE.getTextColor());

        graphics.drawString(mc.font, "Speed:", i + 84, j + 45, DyeColor.LIGHT_GRAY.getTextColor());
        graphics.drawString(mc.font, speed + "", i + 120, j + 45, DyeColor.WHITE.getTextColor());

        graphics.drawString(mc.font, "Jump Height:", i + 84, j + 60, DyeColor.LIGHT_GRAY.getTextColor());
        graphics.drawString(mc.font, jumpHeight + "", i + 148, j + 60, DyeColor.WHITE.getTextColor());

        graphics.drawString(mc.font, "Last known position:" + "", i + 8, j + 84, DyeColor.LIGHT_GRAY.getTextColor());
        graphics.drawString(mc.font, lastPos.equals(Vec3.ZERO) ? "Unknown" : "xyz = " + lastPos.x() + " " + lastPos.y() + " " + lastPos.z(), i + 8, j + 94, DyeColor.WHITE.getTextColor());

        graphics.drawString(mc.font, "Last known dimension:" + "", i + 8, j + 110, DyeColor.LIGHT_GRAY.getTextColor());
        graphics.drawString(mc.font, this.lastDim.location().toString(), i + 8, j + 120, DyeColor.WHITE.getTextColor());

    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.mc.options.keyInventory.isDown()) {
            this.mc.player.closeContainer();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

}
