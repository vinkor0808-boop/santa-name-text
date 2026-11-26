package de.windfwm.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/** Sendet Chatnachrichten oder Befehle wie vom Spieler selbst eingetippt. */
public final class ChatSender {
    public static final int MAX_LENGTH = 256;

    private ChatSender() {}

    /** @return true, wenn gesendet */
    public static boolean send(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;
        if (t.length() > MAX_LENGTH) t = t.substring(0, MAX_LENGTH);
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null || mc.player == null) return false;
        if (t.startsWith("/")) handler.sendChatCommand(t.substring(1));
        else handler.sendChatMessage(t);
        return true;
    }
}
