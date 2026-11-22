package de.santa.santanametext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class SantaConfig {
    public boolean enabled = true, bounce = false;
    /** 0 = Tiers rechts, 1 = Tiers links, 2 = Tiers aus. */
    public int tiersMode = 0;
    public TagElement sep = new TagElement("|", FontStyle.NORMAL, Effect.SOLID, 0x9AA0A6, 0xFFFFFF);

    public TagElement prefix = new TagElement("SANTA", FontStyle.BOLD, Effect.RAINBOW, 0xFF3355, 0xFFD000);
    public TagElement name = new TagElement("", FontStyle.NORMAL, Effect.METALLIC, 0x7A838C, 0xFFFFFF);
    public TagElement suffix = new TagElement("", FontStyle.NORMAL, Effect.GRADIENT, 0x32D4FF, 0xBF5AF2);
    public TagElement top = new TagElement("tiktok: @santa", FontStyle.NORMAL, Effect.GRADIENT, 0xFF3355, 0xFFD000);
    public TagElement bottom = new TagElement("", FontStyle.NORMAL, Effect.SOLID, 0xFFFFFF, 0xFFD000);

    /** Ein Baustein des Nametags (Prefix, Name, Zeile oben, Zeile unten). */
    public static class TagElement {
        public String text = "";
        public FontStyle font = FontStyle.NORMAL;
        public Effect effect = Effect.SOLID;
        public int colorA = 0xFFFFFF, colorB = 0xFFD000;
        public boolean bold, italic, underline, strike;

        public TagElement() {}

        public TagElement(String text, FontStyle font, Effect effect, int a, int b) {
            this.text = text; this.font = font; this.effect = effect; this.colorA = a; this.colorB = b;
        }

        void fix() {
            if (text == null) text = "";
            if (font == null) font = FontStyle.NORMAL;
            if (effect == null) effect = Effect.SOLID;
        }
    }

    private void fix() {
        if (sep == null) sep = new TagElement("|", FontStyle.NORMAL, Effect.SOLID, 0x9AA0A6, 0xFFFFFF);
        if (tiersMode < 0 || tiersMode > 2) tiersMode = 0;
        if (prefix == null) prefix = new TagElement("SANTA", FontStyle.BOLD, Effect.RAINBOW, 0xFF3355, 0xFFD000);
        if (name == null) name = new TagElement("", FontStyle.NORMAL, Effect.METALLIC, 0x7A838C, 0xFFFFFF);
        if (suffix == null) suffix = new TagElement("", FontStyle.NORMAL, Effect.GRADIENT, 0x32D4FF, 0xBF5AF2);
        if (top == null) top = new TagElement("", FontStyle.NORMAL, Effect.GRADIENT, 0xFF3355, 0xFFD000);
        if (bottom == null) bottom = new TagElement("", FontStyle.NORMAL, Effect.SOLID, 0xFFFFFF, 0xFFD000);
        prefix.fix(); name.fix(); suffix.fix(); sep.fix(); top.fix(); bottom.fix();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("santanametext_v2.json");
    private static SantaConfig inst;

    public static SantaConfig get() {
        if (inst == null) {
            try { inst = GSON.fromJson(Files.readString(FILE), SantaConfig.class); } catch (Exception ignored) {}
            if (inst == null) inst = new SantaConfig();
            inst.fix();
        }
        return inst;
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(get())); } catch (Exception ignored) {}
    }
}
