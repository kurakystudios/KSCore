# KSCore

> Framework compartido del ecosistema Kuraky Studios. Un solo JAR que provee configuración, comandos Brigadier, eventos, base de datos, GUIs, items y mensajes — con auto-descubrimiento por anotaciones.

**KSCore** es el núcleo técnico sobre el que corren KsEconomia, KSPlaceholder, KSBoard, KSTienda, KSProtect y KSChunk. Sin KSCore instalado, esos plugins no arrancan. Con KSCore, cada plugin queda liberado del boilerplate típico: registrar comandos, cargar YAML, gestionar conexiones a BD, dibujar GUIs, escribir listeners.

---

## Características

- **Auto-descubrimiento por anotaciones** — `@Comando`, `@Archivo`, `@Escuchador`, `@Item`, `@Menu`, `@Efecto`, `@Tabla`. Una anotación, un método (o clase), y el núcleo hace el resto.
- **Sistema de comandos con Brigadier** — árboles jerárquicos (`@Principal`, `@Sub`) con convertidores de tipos (Player, World, Material, enums, UUID, Duration...). Autocompletado nativo sin escribir tab-completers.
- **Configuración multi-formato** — `@Archivo(ruta = "config.yml")` sobre una clase con campos públicos y KSCore la puebla desde YAML/JSON/TOML. Comentarios preservados en escritura.
- **Base de datos con dialecto** — SQLite por defecto, MariaDB soportada. `@Tabla` + `@Columna` sobre POJOs; `Repositorio<T, ID>` con operaciones asíncronas y caché por entidad.
- **Sistema de eventos** — programático (`Eventos.escuchar(...)`) con suscripciones cancelables + declarativo (`@Escuchador` + `@Evento`) para reducir código de listener.
- **Scheduler unificado** — `Tareas.sync(...)`, `.async(...)`, `.repetir(...)`, `.retrasar(...)`. Folia-safe: usa `GlobalRegionScheduler` cuando aplica.
- **GUIs declarativas** — `Gui` base con `PortadorGui` (anti-dupe), `Menu` registrable por id, botones con manejadores tipados.
- **Sistema de mensajes i18n** — `Mensajes.enviar(sender, clave)` con placeholders `{var}` y colores MiniMessage/legacy.
- **`BigDecimal` en dinero** siempre — cero `double` para importes.
- **`Registro` con niveles** (info/aviso/error/debug) y `Depurador` para trazas verbosas condicionales.

---

## Instalación

1. Descarga `KSCore-1.0-SNAPSHOT.jar`.
2. Coloca el JAR en `plugins/` de tu servidor Paper.
3. Arranca. KSCore genera `plugins/KSCore/config.yml`.

KSCore no expone comandos de usuario final. Su presencia habilita el resto de plugins del ecosistema.

---

## Requisitos

| Requisito | Versión mínima |
|---|---|
| Servidor | **Paper** 1.21+ (api-version 26.2) — **Folia** compatible |
| Java | 21 |

---

## Comandos

Todos requieren permiso `kscore.admin` o superior.

| Comando | Descripción |
|---|---|
| `/core recargar` | Recarga la configuración interna del núcleo |
| `/core debug` | Alterna el modo debug (logs verbosos de cada módulo) |
| `/core info` | Métricas del núcleo (módulos activos, tareas en cola, entidades cacheadas) |
| `/core comandos` | Lista de todos los comandos registrados y su plugin de origen |

---

## Permisos

| Permiso | Default | Descripción |
|---|---|---|
| `kscore.admin` | `op` | Nodo padre |
| `kscore.admin.recargar` | `op` | `/core recargar` |
| `kscore.admin.debug` | `op` | `/core debug` |
| `kscore.admin.info` | `op` | `/core info` |

---

## Configuración

### `config.yml`

```yaml
debug: false                # Traza verbosa de cada módulo

datos:
  dialecto: sqlite          # sqlite | mariadb
  # Sólo para MariaDB:
  host: "localhost"
  puerto: 3306
  base: "ks"
  usuario: ""
  password: ""
  pool_max: 10

mensajes:
  idioma: es
  archivo: mensajes.yml     # Ruta relativa al datafolder del plugin anfitrión
```

---

## Uso para desarrolladores

### Añadir dependencia

En `paper-plugin.yml`:
```yaml
dependencies:
  server:
    KSCore:
      load: BEFORE
      required: true
```

En `build.gradle.kts`:
```kotlin
compileOnly("studio.kuraky:ks-core:1.0-SNAPSHOT")
```

### Plugin main mínimo

```java
public final class MiPlugin extends JavaPlugin {
    private Nucleo nucleo;

    @Override
    public void onEnable() {
        this.nucleo = new Nucleo(this, "com.midominio.miplugin");
        this.nucleo.iniciar();
    }

    @Override
    public void onDisable() {
        if (nucleo != null) nucleo.detener();
    }
}
```

El `Nucleo` escanea el paquete indicado y encuentra automáticamente todo lo anotado.

### Config

```java
@Archivo(ruta = "config.yml")
public final class ConfigMiPlugin extends ArchivoYaml {
    @Seccion("general")
    public General general = new General();

    public static final class General {
        @Comentario("Mensaje mostrado al arrancar.")
        public String saludo = "Hola";
        public int limite = 100;
    }
}

// En cualquier sitio:
ConfigMiPlugin cfg = Archivos.obtener(ConfigMiPlugin.class);
cfg.recargar().join();
```

### Comando

```java
@Comando(nombre = "miplugin", alias = {"mp"},
         permiso = "miplugin.usar",
         descripcion = "Comando principal.")
public final class ComandoMiPlugin {

    @Principal
    public void raiz(Contexto ctx) {
        ctx.enviar("&aHola &f" + ctx.emisor().getName());
    }

    @Sub(nombre = "dar", permiso = "miplugin.admin",
         uso = "<jugador> <cantidad>")
    public void dar(Contexto ctx,
                    @Arg("jugador") Player destino,
                    @Arg("cantidad") int cantidad) {
        // ...
    }
}
```

### Base de datos

```java
@Tabla(nombre = "miplugin_usuarios", cacheMinutos = 15, cacheMaximo = 2000)
public final class Usuario {
    @Id                              public UUID id;
    @Columna(indice = true)          public String nombre = "";
    @Columna                         public int puntos;
    @Columna                         public Instant registrado = Instant.EPOCH;
}

Repositorio<Usuario, UUID> repo = Datos.repositorio(Usuario.class);
repo.obtener(uuid).enHiloPrincipal(opt -> {
    Usuario u = opt.orElseGet(() -> nuevoUsuario(uuid));
    u.puntos += 10;
    repo.guardar(u);
});
```

### Evento programático

```java
Suscripcion s = Eventos.escuchar(BlockBreakEvent.class,
        Prioridad.ALTA, true, e -> {
    if (e.getBlock().getType() == Material.DIAMOND_ORE) {
        e.getPlayer().sendMessage("¡Diamante!");
    }
});

// Al detener:
s.cancelar();
```

### Tareas

```java
// Async con retorno síncrono:
nucleo.tareas().asyncLuegoSync(
    () -> repo.obtener(uuid).futuro().join(),
    resultado -> jugador.sendMessage("Cargado")
);

// Repetitivo:
TareaCancelable t = nucleo.tareas().repetir(20L, () -> tick());
t.cancelar();

// Folia-safe (usa GlobalRegionScheduler internamente):
nucleo.tareas().sync(() -> jugador.teleport(...));
```

---

## Módulos internos

KSCore expone estos módulos, cada uno accesible desde `Nucleo`:

- `ModuloConfiguracion` — `@Archivo` + `Archivos.obtener(...)`
- `ModuloComandos` — `@Comando` + registro Brigadier
- `ModuloEventos` — `@Escuchador` + `Eventos.escuchar(...)`
- `ModuloDatos` — `@Tabla` + `Datos.repositorio(...)`
- `ModuloMensajes` — `Mensajes.enviar(...)`
- `ModuloItems` — `@Item` (builders reutilizables)
- `ModuloGuis` — `@Menu` + `Gui` base
- `ModuloEfectos` — `@Efecto` (partículas, sonidos declarativos)
- `ModuloChat` — hooks de chat con formato y filtros

---

## Convertidores de tipos en comandos

Registrados por defecto:
- Primitivos: `int`, `long`, `double`, `float`, `boolean`, `String`
- Bukkit: `Player`, `OfflinePlayer`, `World`, `Material`
- Utilidad: `UUID`, `Duration`, enums (cualquiera automáticamente)
- Especial: `String[]` (varargs) para capturar el resto de argumentos

Registrar convertidor propio:

```java
Convertidores.registrar(MiTipo.class, new Convertidor<MiTipo>() {
    @Override public MiTipo convertir(Contexto ctx, String texto) {
        return MiTipo.parsear(texto);
    }
    @Override public List<String> sugerencias(Contexto ctx, String parcial) {
        return List.of("opcion1", "opcion2");
    }
});
```

---

## Compatibilidad con Folia

KSCore detecta Folia al arrancar (via `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")`) y enruta:
- `tareas.sync(...)` → `GlobalRegionScheduler`
- `tareas.enEntidad(entidad, ...)` → `entidad.getScheduler()`
- `tareas.enRegion(location, ...)` → `RegionScheduler`

Los plugins que dependan de KSCore no necesitan comprobar Folia — el framework lo hace por ellos.

---

## Soporte

- **Documentación completa:** [wiki]
- **Discord:** [enlace]
- **Bug reports:** [tracker]

---

## Licencia

KSCore es la dependencia obligatoria del ecosistema Kuraky Studios. Se distribuye como jar standalone y va incluido en el bundle de cualquier plugin del ecosistema que adquieras.

Diseñado y mantenido por **Kuraky Studios**.
