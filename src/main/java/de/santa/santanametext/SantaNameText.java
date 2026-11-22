package de.santa.santanametext;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SantaNameText implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SantaConfig.get();
        KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.santanametext.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new SantaScreen(null));
            }
        });
    }
}
