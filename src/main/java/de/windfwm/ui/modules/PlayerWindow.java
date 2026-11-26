package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.core.GameContext;
import de.windfwm.data.Models;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import de.windfwm.util.Fmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Player Manager: Spielernamen speichern, Team verwalten, Notizen.
 * Online-Status stammt nur aus der für alle sichtbaren Tabliste.
 */
public final class PlayerWindow extends Window {
    private int filter;
    private String newName = "";
    private String selected;
    private String search = "";

    public PlayerWindow() {
        super("players", "Player Manager", Icons.USER, 340, 300);
    }

    private List<Models.PlayerNote> players() {
        return WindFwm.data().players.get().players;
    }

    private void dirty() {
        WindFwm.data().players.markDirty();
    }

    private Models.PlayerNote find(String name) {
        for (Models.PlayerNote p : players()) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    private Models.PlayerNote add(String name) {
        String n = name.trim();
        if (n.isEmpty() || !n.matches("[A-Za-z0-9_.*]{2,17}")) return null;
        Models.PlayerNote existing = find(n);
        if (existing != null) return existing;
        Models.PlayerNote p = new Models.PlayerNote();
        p.name = n;
        players().add(p);
        dirty();
        return p;
    }

    @Override
    protected boolean hasSettings() {
        return false;
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        newName = ui.textField("pl-add", x, y, w - 90, newName, "Spielername", 17);
        if (ui.button(x + w - 86, y, 86, 16, "Hinzufügen", UI.Style.PRIMARY) || ui.submitted("pl-add")) {
            Models.PlayerNote p = add(newName);
            if (p != null) {
                selected = p.name;
                newName = "";
            }
        }
        filter = ui.tabs("pl-f", x, y + 22, new String[]{"Alle", "Team", "Online (Tab)"}, filter);
        int ly = y + 44;
        Models.PlayerNote sel = selected == null ? null : find(selected);
        int editorH = sel != null && filter != 2 ? 70 : 0;
        int lh = h - 44 - editorH;
        if (filter == 2) onlineList(ui, x, ly, w, lh);
        else savedList(ui, x, ly, w, lh);
        if (editorH > 0) editor(ui, sel, x, y + h - editorH + 6, w);
    }

    private void savedList(UI ui, int x, int y, int w, int h) {
        search = ui.textField("pl-search", x, y, w, search, "Suchen …", 32);
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<Models.PlayerNote> list = new ArrayList<>();
        for (Models.PlayerNote p : players()) {
            if (filter == 1 && !p.team) continue;
            if (!q.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(q) && !p.note.toLowerCase(Locale.ROOT).contains(q)) continue;
            list.add(p);
        }
        list.sort((a, b) -> {
            int c = Boolean.compare(b.team, a.team);
            if (c != 0) return c;
            c = Boolean.compare(GameContext.isOnline(b.name), GameContext.isOnline(a.name));
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
        });
        int ly = y + 20, lh = h - 20, rowH = 15;
        int contentH = list.size() * rowH;
        int off = ui.beginScroll("pl-list", x, ly, w, lh, contentH);
        Models.PlayerNote remove = null;
        for (int i = 0; i < list.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            Models.PlayerNote p = list.get(i);
            boolean on = GameContext.isOnline(p.name);
            boolean sel = p.name.equalsIgnoreCase(selected);
            boolean hov = ui.hover(x, ry, w, rowH);
            if (sel) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.accent(32));
            else if (hov) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.withAlpha(0xFFFFFF, 10));
            Draw.dot(ui.ctx, x + 6, ry + 7, on ? Theme.GREEN : Theme.TEXT_FAINT);
            Draw.textTrim(ui.ctx, p.name, x + 13, ry + 4, 100, on ? Theme.TEXT : Theme.TEXT_DIM);
            String info = !p.role.isEmpty() ? p.role : p.note;
            Draw.textTrim(ui.ctx, info, x + 118, ry + 4, w - 118 - 44, Theme.TEXT_FAINT);
            if (ui.iconButton(x + w - 38, ry + 1, 13, Icons.STAR, p.team ? "Aus Team entfernen" : "Zum Team hinzufügen", p.team)) {
                p.team = !p.team;
                dirty();
            } else if (hov && ui.iconButton(x + w - 22, ry + 1, 13, Icons.TRASH, "Entfernen", false)) {
                remove = p;
            } else if (ui.pressed(x, ry, w - 40, rowH)) {
                selected = sel ? null : p.name;
            }
        }
        ui.endScroll("pl-list", x, ly, w, lh, contentH);
        if (list.isEmpty()) {
            Draw.textTrim(ui.ctx, filter == 1 ? "Noch keine Teammitglieder (Stern anklicken)" : "Noch keine Spieler gespeichert",
                    x + 3, ly + 3, w, Theme.TEXT_FAINT);
        }
        if (remove != null) {
            players().remove(remove);
            if (remove.name.equalsIgnoreCase(selected)) selected = null;
            dirty();
        }
    }

    private void onlineList(UI ui, int x, int y, int w, int h) {
        List<String> names = GameContext.onlineNamesSorted();
        Draw.text(ui.ctx, names.size() + " Spieler in der Tabliste", x, y, Theme.TEXT_FAINT);
        int ly = y + 14, lh = h - 14, rowH = 14;
        int contentH = names.size() * rowH;
        int off = ui.beginScroll("pl-online", x, ly, w, lh, contentH);
        for (int i = 0; i < names.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            String n = names.get(i);
            Models.PlayerNote known = find(n);
            if (ui.hover(x, ry, w, rowH)) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.withAlpha(0xFFFFFF, 10));
            Draw.text(ui.ctx, n, x + 4, ry + 3, known != null ? Theme.accent() : Theme.TEXT);
            if (known == null) {
                if (ui.iconButton(x + w - 20, ry + 1, 12, Icons.PLUS, "Speichern", false)) add(n);
            } else {
                Draw.textRight(ui.ctx, known.team ? "Team" : "gespeichert", x + w - 8, ry + 3, Theme.TEXT_FAINT);
            }
        }
        ui.endScroll("pl-online", x, ly, w, lh, contentH);
    }

    private void editor(UI ui, Models.PlayerNote p, int x, int y, int w) {
        Draw.rect(ui.ctx, x, y - 4, w, 1, Theme.BORDER_SOFT);
        Draw.text(ui.ctx, p.name, x, y + 2, Theme.TEXT);
        Draw.textRight(ui.ctx, "seit " + Fmt.dateTime(p.added), x + w, y + 2, Theme.TEXT_FAINT);
        String role = ui.textField("pl-role", x, y + 14, w, p.role, "Rolle (z. B. Farmer, Händler)", 40);
        String note = ui.textField("pl-note", x, y + 34, w, p.note, "Notiz", 200);
        if (!role.equals(p.role) || !note.equals(p.note)) {
            p.role = role;
            p.note = note;
            dirty();
        }
    }
}
