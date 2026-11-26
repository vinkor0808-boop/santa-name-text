package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.core.GameContext;
import de.windfwm.core.WaypointService;
import de.windfwm.data.Models;
import de.windfwm.money.MoneyStats;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.WindScreen;
import de.windfwm.util.Fmt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Zentrales Dashboard mit Karten (Masonry-Layout, scrollbar bei wenig Platz). */
public final class DashboardView {
    private static final int GAP = 8;
    private static final int ROW = 12;

    private interface CardBody {
        void draw(UI ui, int x, int y, int w);
    }

    private record Card(String title, Icons icon, int height, String windowId, CardBody body) {}

    public void render(UI ui, WindScreen screen, int x, int y, int w, int h) {
        DrawContext ctx = ui.ctx;
        ui.owner = "main";
        String name = GameContext.playerName();
        Draw.textScaled(ctx, "Hallo, " + name, x, y + 2, 1.5f, Theme.TEXT);
        Draw.text(ctx, "Dashboard · " + GameContext.serverLabel() + " · " + Fmt.time(ui.now), x, y + 18, Theme.TEXT_DIM);
        int top = y + 34;
        int areaH = h - (top - y);

        List<Card> cards = buildCards();
        int cols = w >= 640 ? 3 : (w >= 330 ? 2 : 1);
        int colW = (w - GAP * (cols - 1)) / cols;
        int[] colY = new int[cols];
        int[][] pos = new int[cards.size()][2];
        for (int i = 0; i < cards.size(); i++) {
            int best = 0;
            for (int c = 1; c < cols; c++) if (colY[c] < colY[best]) best = c;
            pos[i][0] = best;
            pos[i][1] = colY[best];
            colY[best] += cards.get(i).height() + GAP;
        }
        int contentH = 0;
        for (int c : colY) contentH = Math.max(contentH, c);

        int off = ui.beginScroll("dashboard", x, top, w, areaH, contentH);
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int cx = x + pos[i][0] * (colW + GAP);
            int cy = top + pos[i][1] - off;
            if (!ui.visible(cy, card.height())) continue;
            drawCard(ui, screen, card, cx, cy, colW);
        }
        ui.endScroll("dashboard", x, top, w, areaH, contentH);
    }

    private void drawCard(UI ui, WindScreen screen, Card card, int x, int y, int w) {
        DrawContext ctx = ui.ctx;
        boolean hov = ui.hover(x, y, w, card.height());
        float t = ui.anim("card#" + card.title(), hov ? 1 : 0, 14f);
        Draw.panel(ctx, x, y, w, card.height(), 6, Theme.SURFACE, Theme.mix(Theme.BORDER, Theme.accent(), t * 0.35f));
        Draw.icon(ctx, card.icon(), x + 9, y + 8, Theme.accent());
        Draw.text(ctx, card.title(), x + 23, y + 9, Theme.TEXT_DIM);
        if (card.windowId() != null) {
            String open = "Öffnen ›";
            int ow = Draw.width(open);
            int ox = x + w - ow - 9;
            if (t > 0.05f) Draw.text(ctx, open, ox, y + 9, Theme.fade(Theme.accent(), t));
            if (ui.pressed(ox - 3, y + 5, ow + 6, 14)) screen.openWindow(card.windowId());
        }
        card.body().draw(ui, x + 9, y + 24, w - 18);
    }

    private List<Card> buildCards() {
        List<Card> cards = new ArrayList<>();
        MoneyStats st = WindFwm.stats();
        Models.ServerProfile p = WindFwm.money().profile();

        cards.add(new Card("Kontostand", Icons.MONEY, 62, "money_tracker", (ui, x, y, w) -> {
            if (p.hasBalance) {
                Draw.textScaled(ui.ctx, Fmt.money(p.balance), x, y + 2, 2f, Theme.TEXT);
                Draw.textTrim(ui.ctx, p.balanceSource + " · " + Fmt.dateTime(p.balanceTime), x, y + 24, w, Theme.TEXT_FAINT);
            } else {
                Draw.text(ui.ctx, "Noch kein Kontostand erkannt", x, y + 4, Theme.TEXT_DIM);
                Draw.textTrim(ui.ctx, "Sidebar, Chat oder manuell setzen", x, y + 18, w, Theme.TEXT_FAINT);
            }
        }));

        cards.add(new Card("Einnahmen pro Stunde", Icons.CHART, 62, "money_stats", (ui, x, y, w) -> {
            Draw.textScaled(ui.ctx, Fmt.money(st.ratePerHour), x, y + 2, 2f, Theme.accent());
            Draw.textTrim(ui.ctx, "Sitzung " + Fmt.money(st.sessionRate) + "/h · Fenster "
                    + WindFwm.cfg().stats.rateWindowMinutes + " min", x, y + 24, w, Theme.TEXT_FAINT);
        }));

        cards.add(new Card("Heute", Icons.DASHBOARD, 62, "money_stats", (ui, x, y, w) -> {
            double net = st.todayIncome - st.todayExpense;
            Draw.text(ui.ctx, "Einnahmen", x, y, Theme.TEXT_DIM);
            Draw.textRight(ui.ctx, Fmt.signed(st.todayIncome), x + w, y, Theme.GREEN);
            Draw.text(ui.ctx, "Ausgaben", x, y + ROW, Theme.TEXT_DIM);
            Draw.textRight(ui.ctx, Fmt.signed(-st.todayExpense), x + w, y + ROW, Theme.RED);
            double goal = WindFwm.cfg().stats.dailyGoal;
            if (goal > 0) {
                Draw.progress(ui.ctx, x, y + ROW * 2 + 4, w, 4, (float) (st.todayIncome / goal), Theme.accent());
                Draw.textRight(ui.ctx, Math.min(999, Math.round(st.todayIncome / goal * 100)) + "% Ziel", x + w, y + ROW * 2 + 10, Theme.TEXT_FAINT);
            } else {
                Draw.text(ui.ctx, "Netto", x, y + ROW * 2, Theme.TEXT_DIM);
                Draw.textRight(ui.ctx, Fmt.signed(net), x + w, y + ROW * 2, Theme.moneyColor(net));
            }
        }));

        List<Models.MoneyEntry> recent = new ArrayList<>();
        for (int i = p.entries.size() - 1; i >= 0 && recent.size() < 6; i--) recent.add(p.entries.get(i));
        cards.add(new Card("Letzte Geldbewegungen", Icons.MONEY, 30 + Math.max(1, recent.size()) * ROW, "money_tracker", (ui, x, y, w) -> {
            if (recent.isEmpty()) {
                Draw.text(ui.ctx, "Noch keine Bewegungen erfasst", x, y, Theme.TEXT_FAINT);
                return;
            }
            int ly = y;
            for (Models.MoneyEntry e : recent) {
                String amount = Fmt.signed(e.amount);
                int aw = Draw.width(amount);
                Draw.text(ui.ctx, Fmt.time(e.t), x, ly, Theme.TEXT_FAINT);
                String desc = e.category + (e.source.isEmpty() ? "" : " · " + e.source);
                Draw.textTrim(ui.ctx, desc, x + 32, ly, w - 36 - aw, Theme.TEXT);
                Draw.textRight(ui.ctx, amount, x + w, ly, Theme.moneyColor(e.amount));
                ly += ROW;
            }
        }));

        cards.add(new Card("Spieler", Icons.USER, 30 + 6 * ROW, null, (ui, x, y, w) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            String pos = mc.player == null ? "—" : (int) Math.floor(mc.player.getX()) + " / " + (int) Math.floor(mc.player.getY())
                    + " / " + (int) Math.floor(mc.player.getZ());
            int ping = GameContext.ping();
            row(ui, x, y, w, "Name", GameContext.playerName(), Theme.TEXT);
            row(ui, x, y + ROW, w, "Server", GameContext.serverLabel(), GameContext.isWindSmp() ? Theme.accent() : Theme.TEXT);
            String dim = GameContext.dimension();
            row(ui, x, y + ROW * 2, w, "Dimension", GameContext.dimensionLabel(dim), GameContext.dimensionColor(dim));
            row(ui, x, y + ROW * 3, w, "Position", pos, Theme.TEXT);
            row(ui, x, y + ROW * 4, w, "Ping", ping < 0 ? "—" : ping + " ms", ping < 0 ? Theme.TEXT : (ping < 80 ? Theme.GREEN : ping < 180 ? Theme.YELLOW : Theme.RED));
            row(ui, x, y + ROW * 5, w, "Sitzung", Fmt.duration(WindFwm.session().sessionLength()) + " · " + GameContext.onlineCount() + " online", Theme.TEXT);
        }));

        List<Models.PlayerNote> team = new ArrayList<>();
        for (Models.PlayerNote pn : WindFwm.data().players.get().players) if (pn.team) team.add(pn);
        team.sort((a, b) -> Boolean.compare(GameContext.isOnline(b.name), GameContext.isOnline(a.name)));
        int teamRows = Math.max(1, Math.min(7, team.size()));
        cards.add(new Card("Team", Icons.TEAM, 30 + teamRows * ROW, "players", (ui, x, y, w) -> {
            if (team.isEmpty()) {
                Draw.textTrim(ui.ctx, "Keine Teammitglieder – im Spieler-Modul markieren", x, y, w, Theme.TEXT_FAINT);
                return;
            }
            int ly = y;
            for (int i = 0; i < Math.min(7, team.size()); i++) {
                Models.PlayerNote pn = team.get(i);
                boolean on = GameContext.isOnline(pn.name);
                Draw.dot(ui.ctx, x + 2, ly + 3, on ? Theme.GREEN : Theme.TEXT_FAINT);
                Draw.textTrim(ui.ctx, pn.name, x + 9, ly, w / 2, on ? Theme.TEXT : Theme.TEXT_DIM);
                if (!pn.role.isEmpty()) Draw.textRight(ui.ctx, Draw.trim(pn.role, w / 2 - 12), x + w, ly, Theme.TEXT_FAINT);
                ly += ROW;
            }
        }));

        List<Models.Waypoint> wps = WaypointService.visible();
        int wpRows = Math.max(1, Math.min(5, wps.size()));
        cards.add(new Card("Waypoints", Icons.PIN, 30 + wpRows * ROW, "waypoints", (ui, x, y, w) -> {
            if (wps.isEmpty()) {
                Draw.textTrim(ui.ctx, "Noch keine Waypoints gespeichert", x, y, w, Theme.TEXT_FAINT);
                return;
            }
            int ly = y;
            for (int i = 0; i < Math.min(5, wps.size()); i++) {
                Models.Waypoint wp = wps.get(i);
                double d = WaypointService.distance(wp);
                String dist = d < 0 ? GameContext.dimensionLabel(wp.dimension) : WaypointService.arrow(wp) + " " + Fmt.distance(d);
                int dw = Draw.width(dist);
                Draw.rect(ui.ctx, x, ly + 1, 2, 7, 0xFF000000 | wp.color);
                Draw.textTrim(ui.ctx, wp.name, x + 7, ly, w - dw - 12, Theme.TEXT);
                Draw.textRight(ui.ctx, dist, x + w, ly, d < 0 ? GameContext.dimensionColor(wp.dimension) : Theme.TEXT_DIM);
                ly += ROW;
            }
        }));

        cards.add(new Card("Persönliche Statistiken", Icons.CHART, 30 + 6 * ROW, "money_stats", (ui, x, y, w) -> {
            row(ui, x, y, w, "Einnahmen gesamt", Fmt.money(st.totalIncome), Theme.GREEN);
            row(ui, x, y + ROW, w, "Ausgaben gesamt", Fmt.money(st.totalExpense), Theme.RED);
            row(ui, x, y + ROW * 2, w, "Transaktionen", Fmt.number(st.totalCount), Theme.TEXT);
            row(ui, x, y + ROW * 3, w, "Spielzeit", Fmt.duration(p.playtimeMs), Theme.TEXT);
            row(ui, x, y + ROW * 4, w, "Strecke", Fmt.distance(p.distance), Theme.TEXT);
            row(ui, x, y + ROW * 5, w, "Beste Stunde (24h)", Fmt.money(st.bestHour), Theme.accent());
        }));
        return cards;
    }

    static void row(UI ui, int x, int y, int w, String label, String value, int color) {
        int vw = Math.min(Draw.width(value), w * 2 / 3);
        Draw.textTrim(ui.ctx, label, x, y, w - vw - 6, Theme.TEXT_DIM);
        Draw.textRight(ui.ctx, Draw.trim(value, w * 2 / 3), x + w, y, color);
    }
}
