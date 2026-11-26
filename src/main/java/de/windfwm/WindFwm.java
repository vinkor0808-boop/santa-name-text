package de.windfwm;

import de.windfwm.config.WindConfig;
import de.windfwm.core.ChatSender;
import de.windfwm.core.Keybinds;
import de.windfwm.data.DataManager;
import de.windfwm.feature.AutoMessages;
import de.windfwm.feature.OrderTracker;
import de.windfwm.ui.Hud;
import de.windfwm.ui.WindScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** WindFWM – Add-on für FWM mit zusätzlichen WindSMP-Modulen (nur Client). */
public final class WindFwm implements ClientModInitializer {
    public static final String MOD_ID = "windfwm";
    public static final String VERSION = "2.0.1";
    public static final Logger LOG = LoggerFactory.getLogger("WindFWM");

    private static DataManager data;
    private static AutoMessages auto;
    private static OrderTracker orders;
    /** Menü im nächsten Tick öffnen (nach dem Schließen des Chats). */
    private static boolean openRequested;

    public static DataManager data() { return data; }
    public static WindConfig cfg() { return data.cfg(); }
    public static AutoMessages auto() { return auto; }
    public static OrderTracker orders() { return orders; }

    @Override
    public void onInitializeClient() {
        data = new DataManager();
        data.loadAll();
        auto = new AutoMessages();
        orders = new OrderTracker();

        Keybinds.register();
        orders.register();
        // Fallback, falls keine Taste belegt ist: /windfwm öffnet das Menü
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("windfwm").executes(ctx -> {
                    openRequested = true;
                    return 1;
                })));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            auto.onJoin();
            orders.onJoin();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            auto.onLeave();
            orders.onLeave();
            data.saveAllNow();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> data.saveAllNow());
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "orders_hud"), (context, tickCounter) -> Hud.render(context));
        LOG.info("[WindFWM] Add-on {} geladen", VERSION);
    }

    private void onTick(MinecraftClient client) {
        try {
            if (client.player != null) {
                while (Keybinds.openMenu.wasPressed()) {
                    if (client.currentScreen == null) client.setScreen(new WindScreen());
                }
                if (openRequested && client.currentScreen == null) {
                    openRequested = false;
                    client.setScreen(new WindScreen());
                }
                List<WindConfig.QuickMessage> quick = cfg().quick;
                for (int i = 0; i < Keybinds.QUICK_KEYS; i++) {
                    while (Keybinds.quick[i].wasPressed()) {
                        if (client.currentScreen == null && i < quick.size()) ChatSender.send(quick.get(i).text);
                    }
                }
                auto.tick();
            }
            data.tick();
        } catch (Exception e) {
            LOG.error("[WindFWM] Fehler im Tick", e);
        }
    }
}
