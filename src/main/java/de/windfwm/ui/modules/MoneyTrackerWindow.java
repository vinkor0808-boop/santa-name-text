package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.data.Models;
import de.windfwm.money.MoneyRule;
import de.windfwm.money.MoneyTracker;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import de.windfwm.util.AmountParser;
import de.windfwm.util.Fmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Money Tracker: Kontostand, Geldbewegungen, manuelle Buchungen, Erkennungsregeln. */
public final class MoneyTrackerWindow extends Window {
    private int tab, filter;
    private String balanceText = "", amountText = "", noteText = "", categoryText = "Sonstiges";
    private String message = "";
    private long messageTime;
    private int selectedRule = -1;
    private String testText = "";

    public MoneyTrackerWindow() {
        super("money_tracker", "Money Tracker", Icons.MONEY, 340, 300);
    }

    private void info(String m) {
        message = m;
        messageTime = System.currentTimeMillis();
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        Models.ServerProfile p = WindFwm.money().profile();
        Draw.text(ui.ctx, "Kontostand", x, y, Theme.TEXT_DIM);
        if (p.hasBalance) {
            Draw.textScaled(ui.ctx, Fmt.moneyFull(p.balance), x, y + 11, 1.6f, Theme.TEXT);
            Draw.textRight(ui.ctx, p.balanceSource + " · " + Fmt.dateTime(p.balanceTime), x + w, y, Theme.TEXT_FAINT);
        } else {
            Draw.text(ui.ctx, "Noch nicht erkannt", x, y + 13, Theme.TEXT_FAINT);
        }
        int ty = y + 32;
        tab = ui.tabs("mt", x, ty, new String[]{"Verlauf", "Manuell buchen"}, tab);
        int by = ty + 22;
        int bh = h - (by - y);
        if (tab == 0) history(ui, p, x, by, w, bh);
        else manual(ui, x, by, w);
        if (!message.isEmpty() && System.currentTimeMillis() - messageTime < 4000) {
            Draw.textRight(ui.ctx, message, x + w, ty + 4, Theme.accent());
        }
    }

    private void history(UI ui, Models.ServerProfile p, int x, int y, int w, int h) {
        filter = ui.tabs("mtf", x, y, new String[]{"Alle", "Einnahmen", "Ausgaben"}, filter);
        List<Models.MoneyEntry> list = new ArrayList<>();
        for (int i = p.entries.size() - 1; i >= 0; i--) {
            Models.MoneyEntry e = p.entries.get(i);
            if (filter == 1 && e.amount <= 0) continue;
            if (filter == 2 && e.amount >= 0) continue;
            list.add(e);
        }
        if (ui.confirmButton("mt-clear", x + w - 60, y, 60, 15, "Leeren")) {
            WindFwm.money().clearHistory();
            info("Historie geleert");
        }
        int ly = y + 21;
        int lh = h - 21 - 14;
        int rowH = 14;
        int contentH = list.size() * rowH;
        int off = ui.beginScroll("mt-list", x, ly, w, lh, contentH);
        Models.MoneyEntry toRemove = null;
        for (int i = 0; i < list.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            Models.MoneyEntry e = list.get(i);
            boolean hov = ui.hover(x, ry, w, rowH);
            if (hov) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.withAlpha(0xFFFFFF, 10));
            Draw.text(ui.ctx, Fmt.dateTime(e.t), x + 3, ry + 3, Theme.TEXT_FAINT);
            String amount = Fmt.signed(e.amount);
            int aw = Draw.width(amount);
            String desc = e.category + (e.source.isEmpty() ? "" : " · " + e.source) + (e.note.isEmpty() ? "" : " · " + e.note)
                    + (e.manual ? " (manuell)" : "");
            int dx = x + 70;
            int right = x + w - 8 - (hov ? 14 : 0);
            Draw.textTrim(ui.ctx, desc, dx, ry + 3, right - aw - dx - 6, Theme.TEXT);
            Draw.textRight(ui.ctx, amount, right, ry + 3, Theme.moneyColor(e.amount));
            if (hov && ui.iconButton(x + w - 19, ry + 1, 12, Icons.TRASH, "Eintrag löschen", false)) toRemove = e;
        }
        ui.endScroll("mt-list", x, ly, w, lh, contentH);
        if (toRemove != null) WindFwm.money().removeEntry(toRemove);
        if (list.isEmpty()) Draw.text(ui.ctx, "Keine Einträge", x + 3, ly + 4, Theme.TEXT_FAINT);
        double sum = 0;
        for (Models.MoneyEntry e : list) sum += e.amount;
        Draw.text(ui.ctx, list.size() + " Einträge", x, y + h - 9, Theme.TEXT_FAINT);
        Draw.textRight(ui.ctx, "Summe " + Fmt.signed(sum), x + w, y + h - 9, Theme.moneyColor(sum));
    }

    private void manual(UI ui, int x, int y, int w) {
        ui.sectionTitle("Kontostand setzen", x, y);
        balanceText = ui.textField("mt-bal", x, y + 11, w - 76, balanceText, "z. B. 1.5M oder 250000", 24);
        if (ui.button(x + w - 70, y + 11, 70, "Setzen") || ui.submitted("mt-bal")) {
            double v = AmountParser.parse(balanceText);
            if (Double.isNaN(v)) info("Ungültiger Betrag");
            else {
                WindFwm.money().setBalance(v, "Manuell");
                balanceText = "";
                info("Kontostand gesetzt");
            }
        }

        int y2 = y + 40;
        ui.sectionTitle("Buchung hinzufügen", x, y2);
        amountText = ui.textField("mt-amt", x, y2 + 11, (w - 6) / 2, amountText, "Betrag", 24);
        categoryText = ui.textField("mt-cat", x + (w - 6) / 2 + 6, y2 + 11, (w - 6) / 2, categoryText, "Kategorie", 32);
        noteText = ui.textField("mt-note", x, y2 + 31, w, noteText, "Notiz (optional)", 64);
        int bw = (w - 6) / 2;
        boolean income = ui.button(x, y2 + 51, bw, 16, "+ Einnahme", UI.Style.PRIMARY);
        boolean expense = ui.button(x + bw + 6, y2 + 51, bw, 16, "− Ausgabe", UI.Style.NORMAL);
        if (income || expense) {
            double v = AmountParser.parse(amountText);
            if (Double.isNaN(v) || v <= 0) info("Ungültiger Betrag");
            else {
                String cat = categoryText.isBlank() ? "Sonstiges" : categoryText.trim();
                WindFwm.money().addEntry(income ? v : -v, cat, "", noteText.trim(), true);
                amountText = "";
                noteText = "";
                info("Gebucht: " + Fmt.signed(income ? v : -v));
            }
        }
        Draw.textTrim(ui.ctx, "Manuelle Buchungen ändern den Kontostand nicht automatisch.", x, y2 + 74, w, Theme.TEXT_FAINT);
    }

    // ------------------------------------------------------------------ Einstellungen

    @Override
    protected void settings(UI ui, int x, int y, int w, int h) {
        WindConfig.Money c = WindFwm.cfg().money;
        List<MoneyRule> rules = c.rules;
        int editorH = selectedRule >= 0 && selectedRule < rules.size() ? 150 : 0;
        int contentH = 150 + rules.size() * 16 + 30 + editorH + 30;
        int off = ui.beginScroll("mt-set", x, y, w, h, contentH);
        int cy = y - off;
        boolean before;

        ui.sectionTitle("Erkennung", x, cy);
        cy += 12;
        before = c.readScoreboard;
        c.readScoreboard = ui.toggleRow(x, cy, w - 6, "Kontostand aus Sidebar lesen", "Sucht Zeilen mit Schlüsselwörtern", c.readScoreboard);
        cy += 18;
        boolean b2 = c.readActionbar;
        c.readActionbar = ui.toggleRow(x, cy, w - 6, "Actionbar auswerten", null, c.readActionbar);
        cy += 18;
        boolean b3 = c.scoreboardDeltas;
        c.scoreboardDeltas = ui.toggleRow(x, cy, w - 6, "Unerklärte Sidebar-Änderungen erfassen", "Als 'Sonstiges' buchen", c.scoreboardDeltas);
        cy += 20;
        if (before != c.readScoreboard || b2 != c.readActionbar || b3 != c.scoreboardDeltas) WindFwm.data().config.markDirty();

        Draw.text(ui.ctx, "Status: " + WindFwm.money().lastSidebarInfo, x, cy, Theme.TEXT_FAINT);
        cy += 13;
        ui.label("Schlüsselwörter (Sidebar, mit Komma getrennt)", x, cy);
        cy += 11;
        String kw = String.join(", ", c.balanceKeywords);
        String nkw = ui.textField("mt-kw", x, cy, w - 6, kw, "money, balance, …", 200);
        if (!nkw.equals(kw)) {
            c.balanceKeywords = new ArrayList<>(Arrays.stream(nkw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
            WindFwm.data().config.markDirty();
        }
        cy += 22;
        int[] sizes = {1000, 5000, 20000};
        int idx = 0;
        for (int i = 0; i < sizes.length; i++) if (sizes[i] == c.maxHistory) idx = i;
        Draw.text(ui.ctx, "Max. Einträge in der Historie", x, cy + 4, Theme.TEXT);
        if (ui.button(x + w - 76, cy, 70, Fmt.number(sizes[idx]))) {
            c.maxHistory = sizes[(idx + 1) % sizes.length];
            WindFwm.data().config.markDirty();
        }
        cy += 26;

        ui.sectionTitle("Erkennungsregeln (Regex mit (?<amount>…))", x, cy);
        cy += 13;
        for (int i = 0; i < rules.size(); i++) {
            MoneyRule r = rules.get(i);
            boolean sel = i == selectedRule;
            if (sel) Draw.round(ui.ctx, x - 2, cy - 2, w - 4, 16, 3, Theme.accent(28));
            boolean en = ui.toggle(x, cy + 1, r.enabled);
            if (en != r.enabled) {
                r.enabled = en;
                WindFwm.data().config.markDirty();
            }
            int col = r.error() != null ? Theme.RED : (r.enabled ? Theme.TEXT : Theme.TEXT_FAINT);
            String tag = r.kindLabel() + " · " + r.scopeLabel();
            int tw = Draw.width(tag);
            Draw.textTrim(ui.ctx, r.name, x + 28, cy + 3, w - 40 - tw - 6, col);
            Draw.textRight(ui.ctx, tag, x + w - 8, cy + 3, Theme.TEXT_FAINT);
            if (ui.pressed(x + 26, cy - 2, w - 30, 16)) selectedRule = sel ? -1 : i;
            cy += 16;
        }
        cy += 4;
        int bw = (w - 12) / 2;
        if (ui.button(x, cy, bw, "+ Eigene Regel")) {
            MoneyRule r = new MoneyRule();
            r.id = Models.newId();
            r.name = "Eigene Regel";
            r.pattern = "^Beispiel: \\$(?<amount>[0-9][0-9.,]*[KMBkmb]?)";
            r.builtin = false;
            rules.add(r);
            selectedRule = rules.size() - 1;
            WindFwm.data().config.markDirty();
        }
        if (ui.confirmButton("mt-reset", x + bw + 6, cy, bw, 16, "Standard wiederherstellen")) {
            c.rules = MoneyRule.defaults();
            selectedRule = -1;
            WindFwm.data().config.markDirty();
        }
        cy += 24;

        if (selectedRule >= 0 && selectedRule < c.rules.size()) {
            ruleEditor(ui, c.rules.get(selectedRule), x, cy, w - 6);
        }
        ui.endScroll("mt-set", x, y, w, h, contentH);
    }

    private void ruleEditor(UI ui, MoneyRule r, int x, int y, int w) {
        Draw.panel(ui.ctx, x - 2, y - 2, w + 4, 146, 4, Theme.SURFACE, Theme.BORDER);
        int cy = y + 3;
        String n = ui.textField("re-name", x + 3, cy, w - 6, r.name, "Name", 48);
        cy += 20;
        String pat = ui.textField("re-pat", x + 3, cy, w - 6, r.pattern, "Regex", 400);
        cy += 20;
        int bw = (w - 18) / 3;
        if (ui.button(x + 3, cy, bw, r.kindLabel())) {
            r.kind = switch (r.kind) {
                case MoneyRule.INCOME -> MoneyRule.EXPENSE;
                case MoneyRule.EXPENSE -> MoneyRule.BALANCE;
                default -> MoneyRule.INCOME;
            };
            WindFwm.data().config.markDirty();
        }
        if (ui.button(x + 9 + bw, cy, bw, r.scopeLabel())) {
            r.scope = switch (r.scope) {
                case MoneyRule.SCOPE_CHAT -> MoneyRule.SCOPE_ACTIONBAR;
                case MoneyRule.SCOPE_ACTIONBAR -> MoneyRule.SCOPE_BOTH;
                default -> MoneyRule.SCOPE_CHAT;
            };
            WindFwm.data().config.markDirty();
        }
        String cat = ui.textField("re-cat", x + 15 + bw * 2, cy, w - 18 - bw * 2, r.category, "Kategorie", 32);
        cy += 22;
        if (!n.equals(r.name) || !pat.equals(r.pattern) || !cat.equals(r.category)) {
            r.name = n;
            r.pattern = pat;
            r.category = cat;
            WindFwm.data().config.markDirty();
        }
        String err = r.error();
        if (err != null) Draw.textTrim(ui.ctx, "Fehler: " + err, x + 3, cy, w - 6, Theme.RED);
        else Draw.text(ui.ctx, "Muster gültig", x + 3, cy, Theme.GREEN);
        cy += 12;
        testText = ui.textField("re-test", x + 3, cy, w - 6, testText, "Testnachricht hier einfügen (Strg+V)", 300);
        cy += 20;
        if (!testText.isEmpty()) {
            MoneyTracker.Match m = MoneyTracker.match(r, testText);
            if (m == null) Draw.text(ui.ctx, "Kein Treffer", x + 3, cy, Theme.TEXT_DIM);
            else Draw.textTrim(ui.ctx, "Treffer: " + Fmt.moneyFull(m.amount())
                    + (m.name() != null ? " · " + m.name() : "") + (m.item() != null ? " · " + m.item() : ""), x + 3, cy, w - 6, Theme.GREEN);
        }
        cy += 14;
        if (ui.confirmButton("re-del", x + 3, cy, 80, 16, "Regel löschen")) {
            WindFwm.cfg().money.rules.remove(r);
            selectedRule = -1;
            WindFwm.data().config.markDirty();
        }
    }
}
