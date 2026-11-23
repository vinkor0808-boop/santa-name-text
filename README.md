# Santa Name Text

Client-side Fabric mod for Minecraft **1.21.11** (Fabric Loader >= 0.16, Fabric API, Java 21).
Open source, MIT license.

## Features
- Own nametag: **prefix**, **name**, **suffix**, freely colourable **separator**
- Extra lines **above** and **below** the name
- 20 text effects (rainbow, gradient, metallic, shimmer, chromatic, fade, glowing, fire, ...), live animated previews
- 40 font styles (Unicode styles + Minecraft fonts), bold / italic / underline / strikethrough
- HSV colour picker, hex input, colour swatches, always-visible live preview
- Tier mods: tier can be shown **left**, **right** or hidden
- Rounded glass menu - open with **K** (rebindable in Controls)

Note: Minecraft hides your own nametag - you see it in third person (F5). Other players do not see it; it is purely client-side.

## Deutsch
Eigener Nametag mit Prefix, Suffix, Trenner, Zeilen ueber/unter dem Namen, Effekten, 40 Schriften und Farbwaehler.
Menue: Taste K (aenderbar unter Steuerung). Den eigenen Tag siehst du nur in der Third-Person-Ansicht (F5), andere sehen ihn nicht.

## Build
```
gradle build        # Java 21, Gradle 9.2.1
```
Result: `build/libs/santanametext-<version>.jar` (use the one without `-sources`).
The included GitHub workflow builds the mod and uploads it as the artifact `santanametext-jar`.

## Contributing
Issues and pull requests are welcome. The code lives in `src/main/java/de/santa/santanametext`:
`NameBuilder` (text/effects), `FontStyle`/`Glyphs` (fonts), `SantaScreen`/`Ui` (menu), `mixin/PlayerRendererMixin` (rendering).
