package de.windfwm.core;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Tastenbelegungen. FWM nutzt J, M, Y – WindFWM nutzt Rechts-Shift und freie Schnellnachricht-Tasten. */
public final class Keybinds {
    public static final int QUICK_KEYS = 5;
    public static KeyBinding openMenu;
    public static final KeyBinding[] quick = new KeyBinding[QUICK_KEYS];

    private Keybinds() {}

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("windfwm", "main"));
        openMenu = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.windfwm.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        for (int i = 0; i < QUICK_KEYS; i++) {
            quick[i] = KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.windfwm.quick" + (i + 1), InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        }
    }

    /** Neu belegen und speichern. keyCode -1 = keine Taste. */
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
