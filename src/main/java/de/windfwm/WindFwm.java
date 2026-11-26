package de.windfwm;

import de.windfwm.chat.ChatManager;
import de.windfwm.config.WindConfig;
import de.windfwm.core.GameContext;
import de.windfwm.core.Keybinds;
import de.windfwm.core.SessionTracker;
import de.windfwm.core.WaypointService;
import de.windfwm.data.DataManager;
import de.windfwm.data.Models;
import de.windfwm.money.MoneyStats;
import de.windfwm.money.MoneyTracker;
import de.windfwm.ui.Hud;
import de.windfwm.ui.WindScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einstiegspunkt der Mod (nur Client). */
public final class WindFwm implements ClientModInitializer {
    public static final String MOD_ID = "windfwm";
    public static final String VERSION = "1.0.2";
    public static final Logger LOG = LoggerFactory.getLogger("WindFWM");

    private static DataManager data;
    private static MoneyTracker money;
    private static MoneyStats stats;
    private static ChatManager chat;
    private static SessionTracker session;

    public static DataManager data() { return data; }
    public static WindConfig cfg() { return data.cfg(); }
    public static MoneyTracker money() { return money; }
    public static MoneyStats stats() { return stats; }
    public static ChatManager chat() { return chat; }
    public static SessionTracker session() { return session; }

    @Override
    public void onInitializeClient() {
        data = new DataManager();
        data.loadAll();
        money = new MoneyTracker();
        stats = new MoneyStats();
        chat = new ChatManager();
        session = new SessionTracker();

        Keybinds.register();
        chat.register();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(session::onJoin));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            session.onLeave();
            data.saveAllNow();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> data.saveAllNow());
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "hud"), (context, tickCounter) -> Hud.render(context));

        LOG.info("[WindFWM] {} geladen", VERSION);
    }

    private void onTick(MinecraftClient client) {
        try {
            if (client.player != null) {
                while (Keybinds.openMenu.wasPressed()) {
                    if (client.currentScreen == null) client.setScreen(new WindScreen());
                }
                while (Keybinds.addWaypoint.wasPressed()) {
                    Models.Waypoint w = WaypointService.addAtPlayer(null);
                    if (w != null) {
                        client.inGameHud.getChatHud().addMessage(Text.literal("§b[WindFWM] §7Waypoint §f" + w.name
                                + " §7gesetzt: §f" + w.x + " " + w.y + " " + w.z));
                    }
                }
                while (Keybinds.toggleHud.wasPressed()) {
                    cfg().hud.enabled = !cfg().hud.enabled;
                    data.config.markDirty();
                }
                session.tick(client);
                money.tick();
                stats.update(GameContext.serverKey(), session.sessionStart());
            }
            data.tick();
        } catch (Exception e) {
            LOG.error("[WindFWM] Fehler im Tick", e);
        }
    }
}
