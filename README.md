# WindFWM

Moderne Client-Mod für **Minecraft 1.21.11 (Fabric)**, abgestimmt auf den Server **WindSMP**.
Keine Server-Sperre: Alles funktioniert auf jedem Server. Optional lassen sich die Geld- und Chat-Funktionen auf WindSMP beschränken.

## Funktionen

| Modul | Inhalt |
|---|---|
| **Dashboard** | Kontostand, Einnahmen/h, Heute, letzte Geldbewegungen, Spielerinfos, Team, Waypoints, persönliche Statistiken |
| **Money Tracker** | Kontostand (Sidebar, Chat oder manuell), Verlauf mit Filtern, manuelle Buchungen, **editierbare Erkennungsregeln mit Testfeld** |
| **Money Stats** | Einnahmen pro Stunde (Zeitfenster wählbar), Sitzung, Heute/Gesamt, 24-h-Diagramm, Einnahmen nach Kategorie (Farmen/Verkauf), Top-Quellen, Tagesziel |
| **Shop Manager** | Eigene Shops mit Artikeln und Preisen, Preisvergleich (günstigstes Angebot grün), eigene Preislisten (in die Zwischenablage kopierbar) |
| **Player Manager** | Spieler speichern, Team markieren (Stern), Rolle + Notiz, Online-Status aus der Tabliste, Spieler aus der Tabliste übernehmen |
| **Waypoint Manager** | Position oder Koordinaten speichern, Dimension, Entfernung + Richtungspfeil, im HUD verfolgen, Koordinaten kopieren, Nether-Umrechnung |
| **Chat Manager** | Verlauf mit Kategorien (Geld/Privat/Server/Spieler) und Suche, Wörter ausblenden/hervorheben, Zeitstempel, Geld-Markierung |
| **HUD** | Kontostand, $/h, Sitzungsbilanz, verfolgter Waypoint – Ecke wählbar |

Bedienung: **Rechts-Shift** öffnet das Menü. Linke Navigation öffnet Module als Fenster (Titelleiste = verschieben, Ecke unten rechts = Größe ändern, Zahnrad = Moduleinstellungen). Fensterpositionen und alle Daten werden gespeichert.

## Installation (Spieler)

1. **Fabric Loader** (0.17.3 oder neuer) für Minecraft 1.21.11 installieren: https://fabricmc.net/use/
2. **Fabric API** für 1.21.11 herunterladen (Modrinth/CurseForge) und in den `mods`-Ordner legen.
3. `windfwm-1.0.2.jar` in den `mods`-Ordner legen (`%appdata%\.minecraft\mods` bzw. den Mods-Ordner deines Launchers).
4. Minecraft mit dem Fabric-Profil starten, **Rechts-Shift** drücken.

## JAR bauen

### Variante A: GitHub Actions (ohne lokale Installation)
1. Neues GitHub-Repository anlegen und **den gesamten Inhalt dieses Ordners** hochladen (inkl. `.github`, `gradle`, `gradlew`).
2. Tab **Actions** öffnen → Workflow „Build WindFWM“ läuft automatisch (oder „Run workflow“).
3. Nach dem Durchlauf unten bei **Artifacts** „WindFWM“ herunterladen → darin liegt die JAR.

### Variante B: Lokal (Java 21 nötig)
```
gradlew build        (Windows: gradlew.bat build)
```
Die JAR liegt danach in `build/libs/windfwm-1.0.2.jar`.

## Projektstruktur

```
build.gradle / settings.gradle / gradle.properties   Build-Konfiguration (Loom, Yarn 1.21.11+build.6, Fabric API 0.141.6)
.github/workflows/build.yml                          Automatischer Build auf GitHub
src/main/resources/fabric.mod.json                   Mod-Metadaten (nur Client)
src/main/resources/assets/windfwm/                   Icon + Übersetzungen der Tastenbelegungen
src/main/java/de/windfwm/
  WindFwm.java              Einstiegspunkt: lädt Daten, registriert Events, Tasten, HUD
  config/WindConfig.java    Alle Einstellungen (config/windfwm/config.json)
  data/Models.java          Datenmodelle (Geld, Shops, Spieler, Waypoints)
  data/DataManager.java     Laden/Speichern aller JSON-Dateien, automatisches Speichern alle 5 s
  util/JsonStore.java       Atomares Speichern, Sicherung defekter Dateien
  util/AmountParser.java    Beträge wie 1.5K / 2M / 1,234.56 / 1.234,56 erkennen
  util/Fmt.java             Formatierung von Geld, Zeit, Strecken
  core/GameContext.java     Server-/WindSMP-Erkennung, eigene Position, Dimension, Tabliste
  core/SessionTracker.java  Spielzeit und Strecke
  core/Keybinds.java        Tastenbelegungen (Rechts-Shift etc.), Neubelegung im Menü
  core/WaypointService.java Waypoint-Logik (Entfernung, Richtungspfeil)
  money/MoneyRule.java      Erkennungsregeln (Regex) inkl. Standardregeln
  money/MoneyTracker.java   Geldbewegungen aus Chat/Actionbar + Kontostand aus Sidebar
  money/MoneyStats.java     Statistiken (pro Stunde, Heute, Gesamt, 24 h, Kategorien)
  money/ScoreboardReader.java  Liest die Sidebar
  chat/ChatManager.java     Chat-Verlauf, Filter, Hervorhebung, Zeitstempel
  ui/Theme.java, Draw.java, Icons.java   Farben, abgerundete Flächen, Schatten, Pixel-Icons
  ui/UI.java                Eigenes Immediate-Mode-GUI (Buttons, Schalter, Textfelder, Tabs, Scrollen)
  ui/Window.java            Verschiebbares Fenster mit Einstellungsansicht
  ui/WindScreen.java        Hauptmenü mit Navigation, Dashboard und Fensterverwaltung
  ui/Hud.java               HUD-Overlay
  ui/modules/*              Dashboard und die sieben Modulfenster
```

## Anpassung an WindSMP

**Auf WindSMP bestätigt (aus Screenshots):**
- Sidebar `Money 21.1M` → Kontostand (gerundete Anzeige wird berücksichtigt)
- `Order payment: +$800K` → Einnahme „Auftrag“
- `Bought wheat seeds for $130` → Ausgabe „Kauf“
- `+$348` (von /sell) → Einnahme „Verkauf“
- `You paid Name $1.` / `You received $1 from [Kopf] Name.` → Zahlung (Spielerkopf wird übersprungen)
- Sidebar-Titel „WindSMP“ → Server wird erkannt, auch bei anderer IP


Die Standard-Erkennungsregeln basieren auf Nachrichtenformaten aus der FWM-Referenz (z. B. `You received $X from Name.`, `Sold container items for $X.`). **Sie sind nicht einzeln auf WindSMP bestätigt.**
So passt du sie an:
1. Menü → Money Tracker → Zahnrad → Erkennungsregeln.
2. Regel anklicken, die echte Chatzeile ins Testfeld kopieren (im Chat Manager eine Zeile anklicken = kopiert).
3. Muster anpassen, bis „Treffer“ erscheint. Pflicht: Gruppe `(?<amount>…)`; optional `(?<name>…)` und `(?<item>…)`.

Der Kontostand wird aus der Sidebar gelesen, wenn eine Zeile eines der Schlüsselwörter enthält (z. B. „Money“, „Kontostand“) – ebenfalls im Money Tracker anpassbar.

## Datenschutz & Fairness

WindFWM liest nur, was dein eigener Client ohnehin anzeigt (Chat, Sidebar, Tabliste, eigene Position). Keine Ortung anderer Spieler, keine Automatisierung, keine Umgehung von Server-Schutzmaßnahmen, keine Netzwerkverbindungen.

Gespeicherte Daten: `.minecraft/config/windfwm/` (`config.json`, `profiles.json`, `shops.json`, `players.json`, `waypoints.json`).

Lizenz: MIT. WindFWM enthält keinen Code aus FWM.
