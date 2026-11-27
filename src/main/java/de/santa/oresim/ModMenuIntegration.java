package de.santa.oresim;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Zahnrad-Knopf in Mod Menu oeffnet das OreSim-Menue. Wird nur geladen, wenn Mod Menu installiert ist. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OreScreen::new;
    }
}
