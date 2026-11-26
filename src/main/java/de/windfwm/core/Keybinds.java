package de.windfwm.core;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Tastenbelegungen. Änderbar in den Minecraft-Steuerungsoptionen und im WindFWM-Menü. */
public final class Keybinds {
    public static KeyBinding openMenu;
    public static KeyBinding addWaypoint;
    public static KeyBinding toggleHud;

    private Keybinds() {}

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("windfwm", "main"));
        openMenu = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.windfwm.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        addWaypoint = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.windfwm.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        toggleHud = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.windfwm.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
    }

    /** Belegt eine Taste neu und speichert die Minecraft-Optionen. keyCode -1 = keine Taste. */
    public static void rebind(KeyBinding binding, int keyCode) {
        InputUtil.Key key = keyCode < 0 ? InputUtil.UNKNOWN_KEY : InputUtil.Type.KEYSYM.createFromCode(keyCode);
        binding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
    }

    public static String label(KeyBinding binding) {
        return binding.getBoundKeyLocalizedText().getString();
    }
}
