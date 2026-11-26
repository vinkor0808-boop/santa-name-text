package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.chat.ChatManager;
import de.windfwm.config.WindConfig;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import de.windfwm.util.Fmt;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chat Manager: übersichtlicher Verlauf, Filter, Hervorhebungen und Darstellung. */
public final class ChatWindow extends Window {
    private int tab, category;
    private String search = "", newHidden = "", newHighlight = "";
    private String message = "";
    private long messageTime;

    public ChatWindow() {
        super("chat", "Chat Manager", Icons.CHAT, 380, 300);
        minW = 280;
    }

    private void info(String m) {
        message = m;
        messageTime = System.currentTimeMillis();
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        tab = ui.tabs("chat", x, y, new String[]{"Verlauf", "Filter & Markierungen"}, tab);
        if (!message.isEmpty() && System.currentTimeMillis() - messageTime < 3000) {
            Draw.textRight(ui.ctx, message, x + w, y + 4, Theme.accent());
        }
        if (tab == 0) history(ui, x, y + 22, w, h - 22);
        else filters(ui, x, y + 22, w, h - 22);
    }

    private void history(UI ui, int x, int y, int w, int h) {
        String[] cats = {"Alle", "Geld", "Privat", "Server", "Spieler"};
        category = ui.tabs("chat-cat", x, y, cats, category);
        search = ui.textField("chat-search", x, y + 20, w, search, "Im Verlauf suchen …", 64);
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<ChatManager.LogLine> lines = new ArrayList<>();
        for (ChatManager.LogLine l : WindFwm.chat().lines()) {
            if (category > 0 && l.category().ordinal() != category - 1) continue;
            if (!q.isEmpty() && !l.text().toLowerCase(Locale.ROOT).contains(q)) continue;
            lines.add(l);
        }
        int ly = y + 40, lh = h - 40, rowH = 12;
        int contentH = lines.size() * rowH;
        int off = ui.beginScroll("chat-list", x, ly, w, lh, contentH);
        for (int i = 0; i < lines.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            ChatManager.LogLine l = lines.get(i);
            boolean hov = ui.hover(x, ry, w, rowH);
            if (hov) Draw.round(ui.ctx, x, ry, w - 4, rowH, 2, Theme.withAlpha(0xFFFFFF, 10));
            String time = Fmt.time(l.time());
            Draw.text(ui.ctx, time, x + 2, ry + 2, Theme.TEXT_FAINT);
            int tx = x + 4 + Draw.width(time) + 4;
            Draw.rect(ui.ctx, tx, ry + 2, 2, 8, l.category().color);
            int col = l.hidden() ? Theme.TEXT_FAINT : (l.category() == ChatManager.Category.PLAYER ? Theme.TEXT : Theme.mix(Theme.TEXT, l.category().color, 0.35f));
            String text = l.hidden() ? "[ausgeblendet] " + l.text() : l.text();
            Draw.textTrim(ui.ctx, text, tx + 6, ry + 2, x + w - tx - 14, col);
            if (hov && Draw.width(text) > x + w - tx - 14) ui.tooltip(Draw.trim(l.text(), 400));
            if (ui.pressed(x, ry, w, rowH)) {
                MinecraftClient.getInstance().keyboard.setClipboard(l.text());
                info("Nachricht kopiert");
            }
        }
        ui.endScroll("chat-list", x, ly, w, lh, contentH);
        if (lines.isEmpty()) Draw.text(ui.ctx, "Keine Nachrichten", x + 3, ly + 3, Theme.TEXT_FAINT);
    }

    private void filters(UI ui, int x, int y, int w, int h) {
        WindConfig.Chat c = WindFwm.cfg().chat;
        int half = (w - 10) / 2;
        newHidden = wordList(ui, "hide", x, y, half, h, "Ausblenden (enthält)", c.hidden, newHidden, Theme.RED);
        newHighlight = wordList(ui, "hl", x + half + 10, y, half, h, "Hervorheben (enthält)", c.highlight, newHighlight, Theme.accent());
    }

    private String wordList(UI ui, String key, int x, int y, int w, int h, String title, List<String> words, String input, int color) {
        ui.sectionTitle(title, x, y);
        String v = ui.textField(key + "-in", x, y + 11, w - 18, input, "Wort oder Satzteil", 64);
        if ((ui.iconButton(x + w - 15, y + 12, 14, Icons.PLUS, "Hinzufügen", false) || ui.submitted(key + "-in")) && !v.isBlank()) {
            if (words.stream().noneMatch(s -> s.equalsIgnoreCase(v.trim()))) words.add(v.trim());
            WindFwm.data().config.markDirty();
            return "";
        }
        int ly = y + 32, lh = h - 32, rowH = 14;
        int contentH = words.size() * rowH;
        int off = ui.beginScroll(key + "-list", x, ly, w, lh, contentH);
        String remove = null;
        for (int i = 0; i < words.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            boolean hov = ui.hover(x, ry, w, rowH);
            Draw.round(ui.ctx, x, ry, w - 4, rowH - 2, 3, hov ? Theme.SURFACE_2 : Theme.SURFACE);
            Draw.rect(ui.ctx, x + 3, ry + 3, 2, 6, color);
            Draw.textTrim(ui.ctx, words.get(i), x + 9, ry + 2, w - 30, Theme.TEXT);
            if (ui.iconButton(x + w - 18, ry, 12, Icons.CLOSE, "Entfernen", false)) remove = words.get(i);
        }
        ui.endScroll(key + "-list", x, ly, w, lh, contentH);
        if (words.isEmpty()) Draw.text(ui.ctx, "Leer", x + 3, ly + 2, Theme.TEXT_FAINT);
        if (remove != null) {
            words.remove(remove);
            WindFwm.data().config.markDirty();
        }
        return v;
    }

    @Override
    protected void settings(UI ui, int x, int y, int w, int h) {
        WindConfig.Chat c = WindFwm.cfg().chat;
        boolean en = ui.toggleRow(x, y, w, "Chat Manager aktiv (Filter & Darstellung)", null, c.enabled);
        boolean ts = ui.toggleRow(x, y + 20, w, "Zeitstempel vor Server-Nachrichten", null, c.timestamps);
        boolean own = ui.toggleRow(x, y + 40, w, "Eigenen Namen hervorheben", null, c.highlightOwnName);
        boolean mon = ui.toggleRow(x, y + 60, w, "Geld-Nachrichten mit $ markieren", null, c.markMoney);
        boolean pc = ui.toggleRow(x, y + 80, w, "Spieler-Chat im Verlauf speichern", null, c.logPlayerChat);
        if (en != c.enabled || ts != c.timestamps || own != c.highlightOwnName || mon != c.markMoney || pc != c.logPlayerChat) {
            c.enabled = en;
            c.timestamps = ts;
            c.highlightOwnName = own;
            c.markMoney = mon;
            c.logPlayerChat = pc;
            WindFwm.data().config.markDirty();
        }
        ui.label("Erkennung privater Nachrichten (Regex)", x, y + 104);
        String pp = ui.textField("chat-pp", x, y + 115, w, c.privatePattern, "Regex", 300);
        if (!pp.equals(c.privatePattern)) {
            c.privatePattern = pp;
            WindFwm.data().config.markDirty();
        }
        if (ui.confirmButton("chat-clear", x, y + 140, 110, 16, "Verlauf leeren")) {
            WindFwm.chat().clear();
            info("Verlauf geleert");
        }
        Draw.textTrim(ui.ctx, "Zeitstempel/Markierungen gelten für Server-Nachrichten.", x, y + 164, w, Theme.TEXT_FAINT);
    }
}
