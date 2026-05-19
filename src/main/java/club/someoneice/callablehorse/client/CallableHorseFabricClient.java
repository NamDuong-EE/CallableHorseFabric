package club.someoneice.callablehorse.client;

import club.someoneice.callablehorse.core.CallableHorseFabric;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class CallableHorseFabricClient implements ClientModInitializer {
    public KeyMapping SET_HORSE = KeyBindingHelper.registerKeyBinding(new KeyMapping(keyStringKey("setHorse"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, keyStringKey("key." + CallableHorseFabric.MODID)));
    public KeyMapping CALL_HORSE = KeyBindingHelper.registerKeyBinding(new KeyMapping(keyStringKey("callHorse"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, keyStringKey("key." + CallableHorseFabric.MODID)));
    // public KeyMapping CHECK_HORSE = KeyBindingHelper.registerKeyBinding(new KeyMapping(keyStringKey("checkHorse"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, keyStringKey("key." + CallableHorseFabric.MODID)));

    private String keyStringKey(String name) {
        return String.format("key.%s.%s", name, CallableHorseFabric.MODID);
    }

    // S2C
    public static ResourceLocation OPEN_GUI = CallableHorseFabric.id("open_horse_gui");

    @Override
    public void onInitializeClient() {
        // ClientPlayNetworking.registerGlobalReceiver(OPEN_GUI, CallableHorseFabricClient::openHorseGUI);
        ClientTickEvents.END_CLIENT_TICK.register(it -> {
           while (CALL_HORSE.consumeClick()) {
               ClientPlayNetworking.send(CallableHorseFabric.CallHorsePayload.INSTANCE);
           }
           while (SET_HORSE.consumeClick()) ClientPlayNetworking.send(CallableHorseFabric.SetHorsePayload.INSTANCE);
           // if (CHECK_HORSE.isDown())    ClientPlayNetworking.send(CallableHorseFabric.STATE_HORSE_PACKAGE, PacketByteBufs.empty());
        });
    }

    /*
    // TODO - Capability is necessary.
    private static void openHorseGUI(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
        byte[] bytes = new byte[buf.getInt(0)];
        buf.getBytes(0, bytes);
        var nbtStr = new String(bytes);
        try {
            CompoundTag horseTag = CompoundTagArgument.compoundTag().parse(new StringReader(nbtStr));
            HorseInfoGUI gui = new HorseInfoGUI(client.player, horseTag);
            client.setScreen(gui);
        } catch (Exception e) {
            CallableHorseFabric.LOG.error(e);
            client.player.sendSystemMessage(Component.literal("An mod error in callable horse. Please send an issues to github with your last log!").withStyle(ChatFormatting.RED));
        }
    }
    */
}
