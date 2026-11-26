package de.windfwm.config;

import java.util.ArrayList;
import java.util.List;

/** Einstellungen des Add-ons (config/windfwm/addon-config.json). */
public class WindConfig {
    public int version = 3;
    public General general = new General();
    public List<QuickMessage> quick = defaultQuick();
    public Auto auto = new Auto();
    public Orders orders = new Orders();

    public static class General {
        public boolean animations = true;
        public int accent = 0x7FD3FF;
    }

    public static class QuickMessage {
        public String name = "";
        public String text = "";

        public QuickMessage() {}

        public QuickMessage(String name, String text) {
            this.name = name;
            this.text = text;
        }
    }

    public static class Auto {
        /** Hauptschalter: aus = es wird nie automatisch gesendet. */
        public boolean enabled = false;
        /** Nur auf WindSMP automatisch senden. */
        public boolean onlyWindSmp = true;
        public List<AutoMessage> messages = new ArrayList<>();
    }

    public static class AutoMessage {
        public String text = "";
        public int intervalSeconds = 300;
        public boolean enabled = true;
    }

    public static class Orders {
        public boolean hud = false;
        /** 0 = oben links, 1 = oben rechts, 2 = unten links, 3 = unten rechts */
        public int hudCorner = 1;
    }

    private static List<QuickMessage> defaultQuick() {
        List<QuickMessage> list = new ArrayList<>();
        list.add(new QuickMessage("Hallo", "Hallo zusammen!"));
        list.add(new QuickMessage("GG", "gg"));
        return list;
    }
}
