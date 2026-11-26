# WindFWM 2.0 – Add-on für FWM

WindFWM läuft **neben FWM 2.4.0** und verändert FWM nicht. Alle FWM-Module bleiben genau wie sie sind.
WindFWM bringt nur zusätzliche Module für WindSMP:

- **Schnellnachrichten**: vorgefertigte Texte oder /befehle per Klick oder Taste einmal senden (die ersten 5 mit Tasten)
- **Auto-Nachrichten**: Chatnachrichten automatisch im eingestellten Intervall (z. B. 90s, 5m, 1h; mindestens 30 s, keine Befehle, Hauptschalter standardmäßig aus, optional nur auf WindSMP)
- **Order-Tracker**: zählt `Order payment: +$…` und `Claimed N ordered item(s).` – pro Stunde, Sitzung, heute, gesamt, optionales HUD

Menü: **Rechts-Shift** (FWM nutzt J, M, Y).

## Installation
In den `mods`-Ordner: Fabric API, `fwm-2.4.0+mc1.21.11.jar` und `windfwm-2.0.0.jar` (Minecraft 1.21.11, Fabric Loader ≥ 0.17.3).

## Bauen
GitHub Actions (`.github/workflows/build.yml`) oder lokal `gradlew build` (Gradle braucht Java 25 für Loom 1.18; die Mod selbst ist Java 21).

Daten: `.minecraft/config/windfwm/addon-config.json`, `orders.json`. Lizenz: MIT. Enthält keinen FWM-Code.
