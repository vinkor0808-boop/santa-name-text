package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.core.GameContext;
import de.windfwm.data.Models;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import de.windfwm.util.AmountParser;
import de.windfwm.util.Fmt;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Shop Manager: eigene Shops, Preise, Preisvergleich und Preislisten. */
public final class ShopWindow extends Window {
    private int tab;
    private String newShopName = "", newListName = "";
    private String selectedShop, selectedList;
    private String itemName = "", itemPrice = "", itemAmount = "1";
    private String search = "";
    private String message = "";
    private long messageTime;

    public ShopWindow() {
        super("shops", "Shop Manager", Icons.SHOP, 390, 300);
        minW = 300;
    }

    private Models.ShopData data() {
        return WindFwm.data().shops.get();
    }

    private void dirty() {
        WindFwm.data().shops.markDirty();
    }

    private void info(String m) {
        message = m;
        messageTime = System.currentTimeMillis();
    }

    @Override
    protected boolean hasSettings() {
        return false;
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        tab = ui.tabs("shop", x, y, new String[]{"Shops", "Preisvergleich", "Preislisten"}, tab);
        if (!message.isEmpty() && System.currentTimeMillis() - messageTime < 4000) {
            Draw.textRight(ui.ctx, message, x + w, y + 4, Theme.accent());
        }
        int cy = y + 22, ch = h - 22;
        switch (tab) {
            case 1 -> compare(ui, x, cy, w, ch);
            case 2 -> lists(ui, x, cy, w, ch);
            default -> shops(ui, x, cy, w, ch);
        }
    }

    // ------------------------------------------------------------------ Shops

    private void shops(UI ui, int x, int y, int w, int h) {
        int leftW = Math.max(96, w / 3);
        Models.Shop sel = null;
        for (Models.Shop s : data().shops) if (s.id.equals(selectedShop)) sel = s;

        newShopName = ui.textField("shop-new", x, y, leftW - 20, newShopName, "Neuer Shop", 40);
        if (ui.iconButton(x + leftW - 17, y + 1, 14, Icons.PLUS, "Shop anlegen", false) || ui.submitted("shop-new")) {
            if (!newShopName.isBlank()) {
                Models.Shop s = new Models.Shop();
                s.name = newShopName.trim();
                data().shops.add(s);
                selectedShop = s.id;
                newShopName = "";
                dirty();
            }
        }
        selectedShop = nameList(ui, "shop-list", x, y + 20, leftW, h - 20, names(data().shops), ids(data().shops), selectedShop);
        Draw.rect(ui.ctx, x + leftW + 5, y, 1, h, Theme.BORDER_SOFT);

        int rx = x + leftW + 12, rw = w - leftW - 12;
        if (sel == null) {
            Draw.textTrim(ui.ctx, data().shops.isEmpty() ? "Lege links deinen ersten Shop an." : "Wähle links einen Shop.", rx, y + 4, rw, Theme.TEXT_FAINT);
            return;
        }
        String n = ui.textField("shop-name", rx, y, rw - 60, sel.name, "Name", 40);
        if (ui.confirmButton("shop-del", rx + rw - 56, y, 56, 16, "Löschen")) {
            data().shops.remove(sel);
            selectedShop = null;
            dirty();
            return;
        }
        String o = ui.textField("shop-owner", rx, y + 20, (rw - 4) / 2, sel.owner, "Besitzer", 32);
        String loc = ui.textField("shop-loc", rx + (rw - 4) / 2 + 4, y + 20, (rw - 4) / 2 - 18, sel.location, "Ort / Warp", 64);
        if (ui.iconButton(rx + rw - 15, y + 21, 14, Icons.TARGET, "Aktuelle Position eintragen", false)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                loc = (int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY()) + " "
                        + (int) Math.floor(mc.player.getZ()) + " (" + GameContext.dimensionLabel(GameContext.dimension()) + ")";
            }
        }
        if (!n.equals(sel.name) || !o.equals(sel.owner) || !loc.equals(sel.location)) {
            sel.name = n;
            sel.owner = o;
            sel.location = loc;
            dirty();
        }
        itemEditor(ui, sel.items, "si", rx, y + 42, rw, h - 42);
    }

    // ------------------------------------------------------------------ Preislisten

    private void lists(UI ui, int x, int y, int w, int h) {
        int leftW = Math.max(96, w / 3);
        Models.PriceList sel = null;
        for (Models.PriceList l : data().lists) if (l.id.equals(selectedList)) sel = l;

        newListName = ui.textField("pl-new", x, y, leftW - 20, newListName, "Neue Liste", 40);
        if (ui.iconButton(x + leftW - 17, y + 1, 14, Icons.PLUS, "Liste anlegen", false) || ui.submitted("pl-new")) {
            if (!newListName.isBlank()) {
                Models.PriceList l = new Models.PriceList();
                l.name = newListName.trim();
                data().lists.add(l);
                selectedList = l.id;
                newListName = "";
                dirty();
            }
        }
        List<String> names = new ArrayList<>(), ids = new ArrayList<>();
        for (Models.PriceList l : data().lists) {
            names.add(l.name);
            ids.add(l.id);
        }
        selectedList = nameList(ui, "pl-list", x, y + 20, leftW, h - 20, names, ids, selectedList);
        Draw.rect(ui.ctx, x + leftW + 5, y, 1, h, Theme.BORDER_SOFT);

        int rx = x + leftW + 12, rw = w - leftW - 12;
        if (sel == null) {
            Draw.textTrim(ui.ctx, data().lists.isEmpty() ? "Erstelle links eine Preisliste." : "Wähle links eine Liste.", rx, y + 4, rw, Theme.TEXT_FAINT);
            return;
        }
        String n = ui.textField("pl-name", rx, y, rw - 120, sel.name, "Name", 40);
        if (!n.equals(sel.name)) {
            sel.name = n;
            dirty();
        }
        if (ui.button(rx + rw - 116, y, 56, "Kopieren")) {
            StringBuilder sb = new StringBuilder(sel.name).append(":");
            for (Models.ShopItem it : sel.items) {
                sb.append("\n").append(it.item).append(" – ").append(Fmt.money(it.price));
                if (it.amount > 1) sb.append(" / ").append(it.amount).append(" Stk.");
            }
            MinecraftClient.getInstance().keyboard.setClipboard(sb.toString());
            info("In Zwischenablage kopiert");
        }
        if (ui.confirmButton("pl-del", rx + rw - 56, y, 56, 16, "Löschen")) {
            data().lists.remove(sel);
            selectedList = null;
            dirty();
            return;
        }
        itemEditor(ui, sel.items, "pl", rx, y + 22, rw, h - 22);
    }

    // ------------------------------------------------------------------ gemeinsamer Artikel-Editor

    private void itemEditor(UI ui, List<Models.ShopItem> items, String key, int x, int y, int w, int h) {
        int nameW = w - 100;
        itemName = ui.textField(key + "-in", x, y, nameW, itemName, "Artikel", 48);
        itemPrice = ui.textField(key + "-ip", x + nameW + 3, y, 50, itemPrice, "Preis", 16);
        itemAmount = ui.textField(key + "-ia", x + nameW + 56, y, 26, itemAmount, "Stk", 5);
        boolean add = ui.iconButton(x + w - 15, y + 1, 14, Icons.PLUS, "Artikel hinzufügen", false)
                || ui.submitted(key + "-in") || ui.submitted(key + "-ip") || ui.submitted(key + "-ia");
        if (add) {
            double price = AmountParser.parse(itemPrice);
            int amount;
            try {
                amount = Math.max(1, Integer.parseInt(itemAmount.trim()));
            } catch (NumberFormatException e) {
                amount = 1;
            }
            if (itemName.isBlank() || Double.isNaN(price)) {
                info("Artikel und Preis angeben");
            } else {
                // vorhandenen Artikel aktualisieren statt doppelt anlegen
                Models.ShopItem existing = null;
                for (Models.ShopItem it : items) if (it.item.equalsIgnoreCase(itemName.trim())) existing = it;
                if (existing != null) {
                    existing.price = price;
                    existing.amount = amount;
                    info("Preis aktualisiert");
                } else {
                    items.add(new Models.ShopItem(itemName.trim(), price, amount));
                }
                itemName = "";
                itemPrice = "";
                itemAmount = "1";
                dirty();
            }
        }
        int ly = y + 22, lh = h - 22, rowH = 14;
        int contentH = items.size() * rowH;
        int off = ui.beginScroll(key + "-items", x, ly, w, lh, contentH);
        Models.ShopItem remove = null;
        for (int i = 0; i < items.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            Models.ShopItem it = items.get(i);
            boolean hov = ui.hover(x, ry, w, rowH);
            if (hov) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.withAlpha(0xFFFFFF, 10));
            String price = Fmt.money(it.price) + (it.amount > 1 ? " / " + it.amount : "");
            int pw = Draw.width(price);
            int right = x + w - 6 - (hov ? 16 : 0);
            Draw.textTrim(ui.ctx, it.item, x + 3, ry + 3, right - pw - x - 10, Theme.TEXT);
            Draw.textRight(ui.ctx, price, right, ry + 3, Theme.accent());
            if (hov && it.amount > 1) ui.tooltip("Stückpreis " + Fmt.money(it.unitPrice()));
            if (hov && ui.iconButton(x + w - 18, ry + 1, 12, Icons.TRASH, null, false)) remove = it;
            if (!hov || remove != null) continue;
            if (ui.pressed(x, ry, w - 20, rowH)) {
                // Klick auf Zeile: Werte zum Bearbeiten in die Felder übernehmen
                itemName = it.item;
                itemPrice = Fmt.money(it.price).replace("$", "");
                itemAmount = String.valueOf(it.amount);
            }
        }
        ui.endScroll(key + "-items", x, ly, w, lh, contentH);
        if (items.isEmpty()) Draw.text(ui.ctx, "Noch keine Artikel", x + 3, ly + 3, Theme.TEXT_FAINT);
        if (remove != null) {
            items.remove(remove);
            dirty();
        }
    }

    // ------------------------------------------------------------------ Preisvergleich

    private record Offer(String item, String where, Models.ShopItem entry) {}

    private void compare(UI ui, int x, int y, int w, int h) {
        search = ui.textField("cmp-search", x, y, w, search, "Artikel suchen, z. B. Diamond", 48);
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<Offer> offers = new ArrayList<>();
        for (Models.Shop s : data().shops) {
            for (Models.ShopItem it : s.items) {
                if (q.isEmpty() || it.item.toLowerCase(Locale.ROOT).contains(q)) offers.add(new Offer(it.item, s.name, it));
            }
        }
        for (Models.PriceList l : data().lists) {
            for (Models.ShopItem it : l.items) {
                if (q.isEmpty() || it.item.toLowerCase(Locale.ROOT).contains(q)) offers.add(new Offer(it.item, "Liste: " + l.name, it));
            }
        }
        offers.sort(Comparator.comparing((Offer o) -> o.item().toLowerCase(Locale.ROOT)).thenComparingDouble(o -> o.entry().unitPrice()));

        int ly = y + 22, lh = h - 22, rowH = 14;
        Draw.text(ui.ctx, "Artikel", x + 3, ly, Theme.TEXT_FAINT);
        Draw.text(ui.ctx, "Quelle", x + w / 2 - 20, ly, Theme.TEXT_FAINT);
        Draw.textRight(ui.ctx, "Stückpreis", x + w - 6, ly, Theme.TEXT_FAINT);
        ly += 12;
        lh -= 12;
        int contentH = offers.size() * rowH;
        int off = ui.beginScroll("cmp-list", x, ly, w, lh, contentH);
        for (int i = 0; i < offers.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            Offer o = offers.get(i);
            // günstigstes / teuerstes Angebot pro Artikel markieren
            double min = Double.MAX_VALUE, max = 0;
            int count = 0;
            for (Offer other : offers) {
                if (!other.item().equalsIgnoreCase(o.item())) continue;
                min = Math.min(min, other.entry().unitPrice());
                max = Math.max(max, other.entry().unitPrice());
                count++;
            }
            double unit = o.entry().unitPrice();
            int col = count > 1 && unit <= min ? Theme.GREEN : (count > 1 && unit >= max ? Theme.RED : Theme.TEXT);
            if (ui.hover(x, ry, w, rowH)) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.withAlpha(0xFFFFFF, 10));
            Draw.textTrim(ui.ctx, o.item(), x + 3, ry + 3, w / 2 - 28, Theme.TEXT);
            Draw.textTrim(ui.ctx, o.where(), x + w / 2 - 20, ry + 3, w / 2 - 50, Theme.TEXT_DIM);
            Draw.textRight(ui.ctx, Fmt.money(unit), x + w - 6, ry + 3, col);
        }
        ui.endScroll("cmp-list", x, ly, w, lh, contentH);
        if (offers.isEmpty()) {
            Draw.textTrim(ui.ctx, q.isEmpty() ? "Noch keine Preise gespeichert." : "Keine Treffer.", x + 3, ly + 3, w, Theme.TEXT_FAINT);
        }
    }

    // ------------------------------------------------------------------ Liste links

    private List<String> names(List<Models.Shop> shops) {
        List<String> out = new ArrayList<>();
        for (Models.Shop s : shops) out.add(s.name + "  (" + s.items.size() + ")");
        return out;
    }

    private List<String> ids(List<Models.Shop> shops) {
        List<String> out = new ArrayList<>();
        for (Models.Shop s : shops) out.add(s.id);
        return out;
    }

    private String nameList(UI ui, String key, int x, int y, int w, int h, List<String> names, List<String> ids, String selected) {
        int rowH = 15;
        int contentH = names.size() * rowH;
        int off = ui.beginScroll(key, x, y, w, h, contentH);
        String result = selected;
        for (int i = 0; i < names.size(); i++) {
            int ry = y + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            boolean sel = ids.get(i).equals(selected);
            boolean hov = ui.hover(x, ry, w, rowH);
            if (sel) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.accent(36));
            else if (hov) Draw.round(ui.ctx, x, ry, w - 4, rowH, 3, Theme.withAlpha(0xFFFFFF, 10));
            Draw.textTrim(ui.ctx, names.get(i), x + 4, ry + 4, w - 10, sel ? Theme.accent() : Theme.TEXT);
            if (ui.pressed(x, ry, w, rowH)) result = ids.get(i);
        }
        ui.endScroll(key, x, y, w, h, contentH);
        return result;
    }
}
