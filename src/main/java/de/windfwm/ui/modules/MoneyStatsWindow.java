package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.money.MoneyStats;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import de.windfwm.util.AmountParser;
import de.windfwm.util.Fmt;

import java.util.Map;

/** Money Stats: Einnahmen pro Stunde, Tages-/Gesamtwerte, Verlauf, Farm-/Verkaufsauswertung. */
public final class MoneyStatsWindow extends Window {
    private String goalText;

    public MoneyStatsWindow() {
        super("money_stats", "Money Stats", Icons.CHART, 340, 310);
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        MoneyStats s = WindFwm.stats();
        int catRows = Math.max(1, s.incomeByCategory.size());
        int srcRows = Math.max(1, s.topSources.size());
        int contentH = 2 * 40 + 8 + 96 + 16 + catRows * 14 + 18 + srcRows * 12 + 10;
        int off = ui.beginScroll("ms", x, y, w, h, contentH);
        int cy = y - off;
        int tw = (w - 12) / 2;

        tile(ui, x, cy, tw, "Pro Stunde (" + WindFwm.cfg().stats.rateWindowMinutes + " min)", Fmt.money(s.ratePerHour), Theme.accent());
        tile(ui, x + tw + 6, cy, tw, "Sitzung pro Stunde", Fmt.money(s.sessionRate), Theme.TEXT);
        cy += 40;
        double todayNet = s.todayIncome - s.todayExpense;
        double totalNet = s.totalIncome - s.totalExpense;
        tile(ui, x, cy, tw, "Heute netto (" + s.todayCount + ")", Fmt.signed(todayNet), Theme.moneyColor(todayNet));
        tile(ui, x + tw + 6, cy, tw, "Gesamt netto (" + s.totalCount + ")", Fmt.signed(totalNet), Theme.moneyColor(totalNet));
        cy += 48;

        // Balkendiagramm der letzten 24 Stunden
        ui.sectionTitle("Letzte 24 Stunden", x, cy);
        cy += 12;
        int chartH = 70;
        Draw.panel(ui.ctx, x, cy, w - 6, chartH + 10, 4, Theme.SURFACE, Theme.BORDER_SOFT);
        double max = 1;
        for (double v : s.hourly) max = Math.max(max, Math.abs(v));
        int cw = w - 6 - 10;
        float barW = cw / 24f;
        long hourMs = 3_600_000L;
        long currentHour = (ui.now / hourMs) * hourMs;
        for (int i = 0; i < 24; i++) {
            double v = s.hourly[i];
            int bx = x + 5 + Math.round(i * barW);
            int bwid = Math.max(1, Math.round(barW) - 2);
            int bh = (int) Math.round(Math.abs(v) / max * (chartH - 6));
            int base = cy + 5 + chartH;
            boolean hov = ui.hover(bx, cy + 5, bwid + 1, chartH);
            int col = v >= 0 ? (hov ? Theme.accent() : Theme.accent(170)) : Theme.withAlpha(Theme.RED, hov ? 255 : 170);
            if (bh > 0) Draw.round(ui.ctx, bx, base - bh, bwid, bh, 1, col);
            else Draw.rect(ui.ctx, bx, base - 1, bwid, 1, Theme.SURFACE_3);
            if (hov) {
                long start = currentHour - (23 - i) * hourMs;
                ui.tooltip(Fmt.time(start) + " Uhr: " + Fmt.signed(v));
            }
        }
        cy += chartH + 18;

        ui.sectionTitle("Einnahmen nach Kategorie (Farmen = Verkauf)", x, cy);
        cy += 12;
        if (s.incomeByCategory.isEmpty()) {
            Draw.text(ui.ctx, "Noch keine Einnahmen", x, cy, Theme.TEXT_FAINT);
            cy += 14;
        } else {
            double catMax = 1;
            for (double v : s.incomeByCategory.values()) catMax = Math.max(catMax, v);
            for (Map.Entry<String, Double> e : s.incomeByCategory.entrySet()) {
                String val = Fmt.money(e.getValue());
                Draw.textTrim(ui.ctx, e.getKey(), x, cy, 80, Theme.TEXT);
                Draw.textRight(ui.ctx, val, x + w - 6, cy, Theme.GREEN);
                int barX = x + 84;
                int barMax = w - 6 - 84 - Draw.width(val) - 8;
                if (barMax > 10) Draw.progress(ui.ctx, barX, cy + 3, barMax, 4, (float) (e.getValue() / catMax), Theme.accent(200));
                cy += 14;
            }
        }
        cy += 4;
        ui.sectionTitle("Top-Quellen (Spieler / Items)", x, cy);
        cy += 12;
        if (s.topSources.isEmpty()) {
            Draw.text(ui.ctx, "—", x, cy, Theme.TEXT_FAINT);
        } else {
            for (Map.Entry<String, Double> e : s.topSources) {
                DashboardView.row(ui, x, cy, w - 6, e.getKey(), Fmt.money(e.getValue()), Theme.GREEN);
                cy += 12;
            }
        }
        ui.endScroll("ms", x, y, w, h, contentH);
    }

    private void tile(UI ui, int x, int y, int w, String label, String value, int color) {
        Draw.panel(ui.ctx, x, y, w, 34, 4, Theme.SURFACE, Theme.BORDER_SOFT);
        Draw.textTrim(ui.ctx, label, x + 7, y + 6, w - 12, Theme.TEXT_DIM);
        Draw.textScaled(ui.ctx, Draw.trim(value, (int) ((w - 14) / 1.3f)), x + 7, y + 18, 1.3f, color);
    }

    @Override
    protected void settings(UI ui, int x, int y, int w, int h) {
        WindConfig.Stats c = WindFwm.cfg().stats;
        int[] windows = {15, 30, 60, 120};
        int idx = 2;
        for (int i = 0; i < windows.length; i++) if (windows[i] == c.rateWindowMinutes) idx = i;
        Draw.text(ui.ctx, "Zeitfenster für 'pro Stunde'", x, y + 4, Theme.TEXT);
        if (ui.button(x + w - 70, y, 70, windows[idx] + " min")) {
            c.rateWindowMinutes = windows[(idx + 1) % windows.length];
            WindFwm.stats().invalidate();
            WindFwm.data().config.markDirty();
        }
        boolean net = ui.toggleRow(x, y + 24, w, "Netto rechnen (Ausgaben abziehen)", null, c.netRate);
        if (net != c.netRate) {
            c.netRate = net;
            WindFwm.stats().invalidate();
            WindFwm.data().config.markDirty();
        }
        ui.label("Tagesziel (0 = aus)", x, y + 50);
        if (goalText == null) goalText = c.dailyGoal > 0 ? Fmt.money(c.dailyGoal).replace("$", "") : "";
        goalText = ui.textField("ms-goal", x, y + 61, w - 76, goalText, "z. B. 5M", 20);
        if (ui.button(x + w - 70, y + 61, 70, "Speichern") || ui.submitted("ms-goal")) {
            double v = goalText.isBlank() ? 0 : AmountParser.parse(goalText);
            if (!Double.isNaN(v)) {
                c.dailyGoal = v;
                WindFwm.data().config.markDirty();
            }
        }
        Draw.textTrim(ui.ctx, "Aktuell: " + (c.dailyGoal > 0 ? Fmt.money(c.dailyGoal) : "kein Ziel"), x, y + 82, w, Theme.TEXT_FAINT);
    }
}
