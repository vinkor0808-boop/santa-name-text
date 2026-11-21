# Santa Name Text (Fabric, MC 1.21.11)

Client-Mod: Logos, Prefix, Zeilen ueber/unter dem Namen, Texteffekte. Menue: Rechte Shift-Taste.

## JAR bauen lassen (ohne alles lokal einzurichten)
1. Auf github.com ein neues Repository anlegen und den Inhalt dieses Ordners hochladen
   (inkl. des versteckten Ordners `.github`).
2. Im Repo auf den Tab **Actions** gehen, den Workflow **build** abwarten.
3. Beim fertigen Lauf unten bei **Artifacts** `santanametext-jar` herunterladen.
4. Die JAR (nicht `-sources`) zusammen mit Fabric API in den mods-Ordner legen.

## Lokal bauen
Java 21 und Gradle 9.2.1 installieren, dann `gradle build`. Ergebnis: `build/libs/`.
