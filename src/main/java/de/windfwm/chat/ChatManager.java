package de.windfwm.chat;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.GameContext;
import de.windfwm.money.MoneyRule;
import de.windfwm.util.Fmt;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Chat-Manager: protokolliert Nachrichten, filtert ausgeblendete Wörter und
 * ergänzt Server-Nachrichten optional um Zeitstempel und Markierungen.
 * Arbeitet ausschließlich mit Nachrichten, die beim eigenen Client ankommen.
 */
public final class ChatManager {
    public enum Category {
        MONEY("Geld", 0xFF6FDC9C),
        PRIVATE("Privat", 0xFFC79BFF),
        SERVER("Server", 0xFF8FB8D8),
        PLAYER("Spieler", 0xFFE6E9EC);

        public final String label;
        public final int color;

        Category(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    public record LogLine(long time, Category category, String text, boolean hidden) {}

    private final Deque<LogLine> log = new ArrayDeque<>();
    private int version;
    private Pattern privatePattern;
    private String privatePatternSrc;
    /** Wird zwischen ALLOW_GAME und MODIFY_GAME gesetzt, damit dieselbe Nachricht markiert werden kann. */
    private boolean lastWasMoney;

    public void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> onGame(message, overlay));
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : decorate(message));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onPlayerChat(message));
    }

    private boolean active() {
        WindConfig c = WindFwm.cfg();
        return c.chat.enabled && (!c.general.windSmpOnly || GameContext.isWindSmp());
    }

    private boolean onGame(Text message, boolean overlay) {
        try {
            String plain = Fmt.strip(message.getString());
            MoneyRule rule = WindFwm.money().handle(plain, overlay);
            if (overlay) return true;
            lastWasMoney = rule != null;
            Category cat = rule != null ? Category.MONEY : (isPrivate(plain) ? Category.PRIVATE : Category.SERVER);
            boolean hide = active() && isHidden(plain);
            add(new LogLine(System.currentTimeMillis(), cat, plain, hide));
            return !hide;
        } catch (Exception e) {
            WindFwm.LOG.error("[WindFWM] Fehler bei Chat-Verarbeitung", e);
            return true;
        }
    }

    private boolean onPlayerChat(Text message) {
        try {
            String plain = Fmt.strip(message.getString());
            boolean hide = active() && isHidden(plain);
            if (WindFwm.cfg().chat.logPlayerChat) {
                add(new LogLine(System.currentTimeMillis(), isPrivate(plain) ? Category.PRIVATE : Category.PLAYER, plain, hide));
            }
            return !hide;
        } catch (Exception e) {
            WindFwm.LOG.error("[WindFWM] Fehler bei Chat-Verarbeitung", e);
            return true;
        }
    }

    private Text decorate(Text message) {
        if (!active()) return message;
        WindConfig.Chat c = WindFwm.cfg().chat;
        try {
            String plain = Fmt.strip(message.getString());
            if (plain.isEmpty()) return message;
            boolean highlight = isHighlighted(plain);
            boolean money = c.markMoney && lastWasMoney;
            lastWasMoney = false;
            if (!c.timestamps && !highlight && !money) return message;
            MutableText out = Text.empty();
            if (c.timestamps) {
                out.append(Text.literal(Fmt.time(System.currentTimeMillis()) + " ").styled(s -> s.withColor(0x6B737C)));
            }
            if (highlight) {
                int accent = WindFwm.cfg().general.accent;
                out.append(Text.literal("▌ ").styled(s -> s.withColor(accent)));
            } else if (money) {
                out.append(Text.literal("$ ").styled(s -> s.withColor(0x6FDC9C)));
            }
            out.append(message);
            return out;
        } catch (Exception e) {
            return message;
        }
    }

    private boolean isHidden(String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        for (String w : WindFwm.cfg().chat.hidden) {
            if (w != null && !w.isBlank() && lower.contains(w.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private boolean isHighlighted(String plain) {
        WindConfig.Chat c = WindFwm.cfg().chat;
        String lower = plain.toLowerCase(Locale.ROOT);
        if (c.highlightOwnName) {
            String own = GameContext.playerName().toLowerCase(Locale.ROOT);
            // eigene Nachrichten ("<Name> ...") nicht markieren
            if (!own.isEmpty() && lower.contains(own) && !lower.startsWith(own) && !lower.startsWith("<" + own)) return true;
        }
        for (String w : c.highlight) {
            if (w != null && !w.isBlank() && lower.contains(w.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private boolean isPrivate(String plain) {
        String src = WindFwm.cfg().chat.privatePattern;
        if (src == null || src.isBlank()) return false;
        if (!src.equals(privatePatternSrc)) {
            privatePatternSrc = src;
            try {
                privatePattern = Pattern.compile(src);
            } catch (PatternSyntaxException e) {
                privatePattern = null;
            }
        }
        return privatePattern != null && privatePattern.matcher(plain).find();
    }

    private void add(LogLine line) {
        log.addLast(line);
        int max = Math.max(50, WindFwm.cfg().chat.logSize);
        while (log.size() > max) log.removeFirst();
        version++;
    }

    public int version() {
        return version;
    }

    /** Neueste zuerst. */
    public List<LogLine> lines() {
        List<LogLine> out = new ArrayList<>(log);
        java.util.Collections.reverse(out);
        return out;
    }

    public void clear() {
        log.clear();
        version++;
    }
}
