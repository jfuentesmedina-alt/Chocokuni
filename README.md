# ChocoCraftPaper

Plugin de Paper que agrega chocobos jugables (razas/colores, silla, doma con
Gysahl Greens, cría y crecimiento de bebé a adulto) usando **FreeMinecraftModels
(FMM)** como motor visual. Funciona para jugadores de Java y Bedrock (vía
Geyser + Floodgate, que ya tienes instalados) porque el chocobo es, por debajo,
un `Horse` vanilla normal con el modelo de FMM puesto encima — exactamente el
mismo enfoque validado en tu `README.txt` de JaimeChocoCraft (mount directo
sobre el Horse oculto, sin el asiento por Interaction entity de FMM).

## Por qué así y no como el mod ChocoCraft original

`chococraft-*-fabric/neoforge.jar` son **mods**, no plugins: dependen del
cliente y no cargan en Paper. Los usé solo como referencia conceptual de
mecánicas (razas por color, silla, cría) — el código de este plugin es nuevo y
no reutiliza ni descompila su código.

`FreeMinecraftModels` solo expone públicamente:
- `DisguiseAPI` → disfraza **jugadores**, no sirve para vestir mobs.
- `ModeledEntityManager` → solo lectura (existencia de modelos, reload).
- Eventos de click sobre entidades modeladas.

No hay una API pública para "spawnear el modelo X sobre la entidad Y", así que
este plugin le pide a FMM que spawnee el modelo **por su propio comando**
(configurable en `config.yml` → `settings.spawn-command`) y luego localiza el
`Horse` vanilla recién aparecido para etiquetarlo con los datos de ChocoCraft
(raza, dueño, etapa). Es un enfoque pragmático dado lo que expone la API
pública; si tu versión de FMM usa otra sintaxis de comando, ajusta esa línea.

## Qué incluye

- **Razas configurables** (`config.yml` → `races`): amarillo ya listo con tu
  `jaime_chocobo_yellow.bbmodel` (ID `JaimeChocoCraft`); negro/azul/verde/dorado
  quedan como plantilla pero **deshabilitados automáticamente** hasta que crees
  esos `.bbmodel` en FMM y apuntes `model-adult`/`model-baby` al ID correcto.
- **Doma**: click derecho en un chocobo salvaje con *Gysahl Greens* (item
  custom, base `WHEAT`), con probabilidad configurable.
- **Silla obligatoria**: no se puede montar sin silla (usa la silla vanilla).
- **Cría**: alimentar dos chocobos adultos domados con Gysahl Greens los pone
  en modo amor; al reproducirse, en vez del potrillo vanilla "pelado" se
  spawnea un chocobo bebé con el modelo FMM correcto, heredando raza de forma
  ponderada (80% hereda de un padre, 20% "muta" a una raza aleatoria global).
- **Crecimiento**: usa el reloj de envejecimiento vanilla del Horse (escalado
  a los minutos que definas); al hacerse adulto, el bebé se reemplaza por la
  versión adulta del modelo (no existe API de FMM para "cambiar el modelo" de
  una entidad ya viva, así que se reemplaza la entidad conservando dueño/raza).
- **Comando** `/chococraft` (alias `/choco`, `/jchoco`): `spawn <raza> [jugador]`,
  `give <gysahl|egg> [jugador] [cantidad]`, `races`, `reload`.

## Cosas que TENÉS que revisar antes de compilar (no pude verificarlas en vivo)

1. **Versión de Paper API** (`pom.xml` → `paper.api.version`): tu server corre
   Minecraft "26.2", un versionado posterior a mi información. Pon ahí
   exactamente la línea que imprime tu server al iniciar
   (`Implementing API version ...`).
2. **`plugin.yml` → `api-version`**: mismo motivo, confírmalo contra tu build.
3. **Nombres de `Attribute`** en `ChocoboManager.applyRaceAttributes` — usé
   `GENERIC_MOVEMENT_SPEED` y `HORSE_JUMP_STRENGTH` (válidos hasta 1.20.x).
   Paper 1.21 reorganizó `org.bukkit.attribute.Attribute` a un registro; si no
   compila, abre esa clase de tu `paper-api` y usa los nombres que tenga.
4. **Sintaxis real del comando de spawn de FMM** (`config.yml` →
   `settings.spawn-command`). Verifica con `/fmm help` en tu servidor.

## Compilar SIN instalar nada (GitHub Actions)

El proyecto ya trae `.github/workflows/build.yml` y el jar de FMM en
`libs/freeminecraftmodels.jar`, listos para que GitHub lo compile por vos:

1. Entra a https://github.com/new y crea un repositorio nuevo (puede ser
   privado). No hace falta que sepas usar git en la terminal.
2. Entra al repo recién creado → botón **"Add file" → "Upload files"**.
3. Arrastra **todo** el contenido descomprimido de este zip (la carpeta
   `chococraft-paper` completa: `src/`, `libs/`, `.github/`, `pom.xml`, etc. —
   sube el contenido de adentro de `chococraft-paper`, no la carpeta en sí) y
   confirma el commit ("Commit changes").
4. Andá a la pestaña **"Actions"** del repo. Debería haber arrancado solo un
   workflow llamado "Build ChocoCraftPaper" (tarda 1-2 minutos). Si no arrancó,
   hace click en "Build ChocoCraftPaper" en el panel izquierdo → "Run workflow".
5. Cuando el run termine en verde ✅, entra a ese run → sección **"Artifacts"**
   al final de la página → descarga `chococraft-paper-jar.zip`.
6. Ese zip trae el `.jar` ya compilado, listo para `plugins/`.

Si el run termina en rojo ❌, entra al log del paso "Build plugin": lo más
probable es que el error diga algo como "could not resolve
io.papermc.paper:paper-api:X" — eso confirma que la versión puesta en
`pom.xml` (`paper.api.version`) no es la real de tu server. Corrígela ahí
(mirando el "Implementing API version ..." que imprime tu server al iniciar),
sube de nuevo ese archivo al repo (Actions se vuelve a disparar solo) y listo.

## Alternativa: compilar en tu PC

Si en algún momento preferís instalar JDK 21 + Maven localmente:

```bash
# 1) Instala el jar de FMM como dependencia local de Maven
mvn install:install-file -Dfile=libs/freeminecraftmodels.jar \
    -DgroupId=com.magmaguy -DartifactId=freeminecraftmodels \
    -Dversion=local -Dpackaging=jar

# 2) Compila el plugin
cd chococraft-paper
mvn clean package
# el jar queda en target/chococraft-paper-1.0.0.jar
```

## Instalar en el server

1. Copia `target/chococraft-paper-1.0.0.jar` a `plugins/`.
2. Asegúrate de que `FreeMinecraftModels.jar`, `Geyser-Spigot.jar` y
   `floodgate-spigot.jar` ya estén en `plugins/` (los tuyos).
3. Copia `jaime_chocobo_yellow.bbmodel` a
   `plugins/FreeMinecraftModels/models/JaimeChocoCraft/` (como en tu
   `README.txt` original).
4. Arranca el server, deja que se genere `plugins/ChocoCraftPaper/config.yml`.
5. `/fmm reload`, luego `/chococraft spawn yellow`.

## Bedrock: una salvedad sobre los items

Gysahl Greens y el Huevo de Chocobo son ítems vanilla (`WHEAT`, `TURTLE_EGG`)
renombrados con lore — jugadores Bedrock los verán con la textura vanilla del
material base (Geyser no traduce texturas custom de items sin un resource
pack mapeado). El propio mob/mount sí se ve igual en Java y Bedrock porque es
una entidad real con modelo FMM, no un ítem. Si quieres texturas propias para
esos ítems en Bedrock, se puede añadir un mapeo de Geyser (`custom-items` en
su config) — puedo armarlo si lo necesitas.

## Limitaciones conocidas / decisiones de diseño

- El "cambio de modelo" al crecer de bebé a adulto se resuelve
  despawneando y volviendo a spawnear el chocobo (no hay API de FMM para
  actualizar el modelo de una entidad viva).
- La búsqueda del `Horse` recién creado tras el comando de FMM es "el Horse
  sin etiquetar más cercano en un radio configurable, un tick después" — en
  un área muy concurrida con más de un Horse sin etiquetar podría marcar el
  equivocado. Si eso llega a pasar, bajá `settings.spawn-search-radius`.
