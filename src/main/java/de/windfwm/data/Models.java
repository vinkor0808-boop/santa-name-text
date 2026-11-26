package de.windfwm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Datenmodelle, die als JSON gespeichert werden. Öffentliche Felder für Gson. */
public final class Models {
    private Models() {}

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------------------------------------------------------------- Geld + Statistik

    /** Daten pro Server (Schlüssel = Serveradresse). */
    public static class Profiles {
        public Map<String, ServerProfile> servers = new HashMap<>();
    }

    public static class ServerProfile {
        public boolean hasBalance = false;
        public double balance = 0;
        public long balanceTime = 0;
        public String balanceSource = "";
        public List<MoneyEntry> entries = new ArrayList<>();
        public long playtimeMs = 0;
        public double distance = 0;
        public long firstSeen = System.currentTimeMillis();
        public int sessions = 0;
    }

    public static class MoneyEntry {
        public long t;
        /** positiv = Einnahme, negativ = Ausgabe */
        public double amount;
        public String category = "Sonstiges";
        /** Spieler oder Item, falls erkannt */
        public String source = "";
        public String note = "";
        public boolean manual = false;

        public MoneyEntry() {}

        public MoneyEntry(long t, double amount, String category, String source, String note, boolean manual) {
            this.t = t;
            this.amount = amount;
            this.category = category;
            this.source = source == null ? "" : source;
            this.note = note == null ? "" : note;
            this.manual = manual;
        }
    }

    // ---------------------------------------------------------------- Shops

    public static class ShopData {
        public List<Shop> shops = new ArrayList<>();
        public List<PriceList> lists = new ArrayList<>();
    }

    public static class Shop {
        public String id = newId();
        public String name = "";
        public String owner = "";
        public String location = "";
        public String note = "";
        public List<ShopItem> items = new ArrayList<>();
    }

    public static class ShopItem {
        public String item = "";
        public double price = 0;
        public int amount = 1;
        public String note = "";

        public ShopItem() {}

        public ShopItem(String item, double price, int amount) {
            this.item = item;
            this.price = price;
            this.amount = Math.max(1, amount);
        }

        public double unitPrice() {
            return price / Math.max(1, amount);
        }
    }

    public static class PriceList {
        public String id = newId();
        public String name = "";
        public List<ShopItem> items = new ArrayList<>();
    }

    // ---------------------------------------------------------------- Spieler

    public static class PlayerData {
        public List<PlayerNote> players = new ArrayList<>();
    }

    public static class PlayerNote {
        public String name = "";
        public boolean team = false;
        public String role = "";
        public String note = "";
        public long added = System.currentTimeMillis();
    }

    // ---------------------------------------------------------------- Waypoints

    public static class WaypointData {
        public List<Waypoint> list = new ArrayList<>();
    }

    public static class Waypoint {
        public String id = newId();
        public String name = "";
        public String server = "";
        public String dimension = "minecraft:overworld";
        public int x, y, z;
        public int color = 0x7FD3FF;
        public long created = System.currentTimeMillis();
    }
}
