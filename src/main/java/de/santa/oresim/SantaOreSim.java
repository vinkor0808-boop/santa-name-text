package de.santa.oresim;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SantaOreSim implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("santa-oresim");
    private static final String P = "§b[OreSim]§r ";
    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;
    private static boolean openMenu;

    @Override
    public void onInitializeClient() {
        OreConfig.load();
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("santaoresim", "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.santaoresim.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.santaoresim.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                OreConfig.INSTANCE.enabled = !OreConfig.INSTANCE.enabled;
                OreConfig.save();
                msg(client, "ESP " + (OreConfig.INSTANCE.enabled ? "§aAN" : "§cAUS"));
            }
            while (menuKey.wasPressed()) openMenu = true;
            if (openMenu && client.currentScreen == null) {
                openMenu = false;
                client.setScreen(new OreScreen(null));
            }
            OreTracker.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("oresim")
                        .executes(SantaOreSim::info)
                        .then(ClientCommandManager.literal("menu").executes(c -> {
                            // Erst im naechsten Tick oeffnen, sonst schliesst der Chat das Menue sofort wieder
                            openMenu = true;
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("on").executes(c -> setEnabled(c, true)))
                        .then(ClientCommandManager.literal("off").executes(c -> setEnabled(c, false)))
                        .then(ClientCommandManager.literal("seed")
                                .then(ClientCommandManager.argument("seed", LongArgumentType.longArg()).executes(c -> {
                                    OreConfig.INSTANCE.seed = LongArgumentType.getLong(c, "seed");
                                    OreConfig.INSTANCE.hasSeed = true;
                                    OreConfig.save();
                                    OreTracker.reset();
                                    feedback(c, "Seed gesetzt: §e" + OreConfig.INSTANCE.seed);
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("radius")
                                .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(16, 256)).executes(c -> {
                                    OreConfig.INSTANCE.radius = IntegerArgumentType.getInteger(c, "blocks");
                                    OreConfig.save();
                                    feedback(c, "Radius: §e" + OreConfig.INSTANCE.radius + " Bloecke");
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("diamonds").executes(c -> {
                            OreConfig.INSTANCE.diamonds = !OreConfig.INSTANCE.diamonds;
                            OreConfig.save();
                            feedback(c, "Diamanten " + onOff(OreConfig.INSTANCE.diamonds));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("debris").executes(c -> {
                            OreConfig.INSTANCE.debris = !OreConfig.INSTANCE.debris;
                            OreConfig.save();
                            feedback(c, "Ancient Debris " + onOff(OreConfig.INSTANCE.debris));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reload").executes(c -> {
                            OreTracker.reset();
                            feedback(c, "Neu berechnet.");
                            return 1;
                        }))));
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> c, boolean on) {
        OreConfig.INSTANCE.enabled = on;
        OreConfig.save();
        feedback(c, "ESP " + onOff(on));
        return 1;
    }

    private static int info(CommandContext<FabricClientCommandSource> c) {
        OreConfig cfg = OreConfig.INSTANCE;
        feedback(c, "ESP " + onOff(cfg.enabled) + "§r | Seed §e" + cfg.seed + "§r | Radius §e" + cfg.radius
                + "§r | Diamanten " + onOff(cfg.diamonds) + "§r | Debris " + onOff(cfg.debris));
        if (OreSim.error() != null) feedback(c, "§cFehler: " + OreSim.error());
        else if (!OreSim.isReady()) feedback(c, "Wird initialisiert...");
        else feedback(c, "§7" + OreTracker.visible().size() + " Boxen, " + OreTracker.cachedChunks() + " Chunks | " + OreSim.indexInfo());
        feedback(c, "§7/oresim menu|on|off|seed <seed>|radius <16-256>|diamonds|debris|reload");
        return 1;
    }

    private static String onOff(boolean b) { return b ? "§aAN" : "§cAUS"; }

    private static void feedback(CommandContext<FabricClientCommandSource> c, String s) {
        c.getSource().sendFeedback(Text.literal(P + s));
    }

    private static void msg(MinecraftClient client, String s) {
        if (client.player != null) client.player.sendMessage(Text.literal(P + s), false);
    }
}
