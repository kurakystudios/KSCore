package studio.kuraky.kSCore.nucleo;

import org.bukkit.plugin.java.JavaPlugin;
import studio.kuraky.kSCore.chat.ModuloChat;
import studio.kuraky.kSCore.comandos.ModuloComandos;
import studio.kuraky.kSCore.configuracion.Archivos;
import studio.kuraky.kSCore.configuracion.ModuloConfiguracion;
import studio.kuraky.kSCore.datos.ModuloDatos;
import studio.kuraky.kSCore.efectos.ModuloEfectos;
import studio.kuraky.kSCore.eventos.ModuloEventos;
import studio.kuraky.kSCore.gui.ModuloGuis;
import studio.kuraky.kSCore.items.ModuloItems;
import studio.kuraky.kSCore.mensajes.ModuloMensajes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Nucleo {

    /** Última instancia arrancada — legacy. Preferir {@link #de(JavaPlugin)}. */
    private static volatile Nucleo actual;
    /**
     * Registro por-plugin de todos los núcleos vivos en la JVM. Cuando
     * varios plugins comparten KSCore como dependencia (jar standalone
     * en {@code plugins/KSCore/}), cada uno tiene su propia instancia
     * — este mapa permite localizarla sin caer en el singleton
     * histórico {@link #actual()}, que devuelve el último en arrancar.
     */
    private static final Map<JavaPlugin, Nucleo> INSTANCIAS = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;
    private final String paqueteAnfitrion;
    private final Registro registro;
    private final Depurador depurador;
    private final Tareas tareas;
    private final EscanerClases escaner;
    private final ModuloConfiguracion moduloConfiguracion;
    private final ModuloMensajes moduloMensajes;
    private final ModuloDatos moduloDatos;
    private final ModuloEventos moduloEventos;
    private final ModuloComandos moduloComandos;
    private final ModuloItems moduloItems;
    private final ModuloEfectos moduloEfectos;
    private final ModuloGuis moduloGuis;
    private final ModuloChat moduloChat;
    private final List<Modulo> modulos = new ArrayList<>();

    /** Modo plugin propio: paquete anfitrión = paquete del plugin. */
    public Nucleo(JavaPlugin plugin) {
        this(plugin, plugin.getClass().getPackageName());
    }

    /**
     * Modo embebido: KsCore corre dentro de otro plugin. Se le indica qué
     * paquete del anfitrión debe escanear para descubrir sus propias clases
     * anotadas (@Comando, @Escuchador, @Archivo, @Item, @Menu, @Efecto,
     * @Tabla). El logger, la carpeta de datos y el scheduler del anfitrión
     * se usan tal cual.
     */
    public Nucleo(JavaPlugin plugin, String paqueteAnfitrion) {
        this.plugin = plugin;
        this.paqueteAnfitrion = paqueteAnfitrion;
        this.registro = new Registro(plugin, "Core");
        this.depurador = new Depurador(this.registro, false);
        this.tareas = new Tareas(plugin, registro);
        this.escaner = new EscanerClases(plugin, paqueteAnfitrion, registro);
        this.moduloConfiguracion = new ModuloConfiguracion();
        this.moduloMensajes = new ModuloMensajes();
        this.moduloDatos = new ModuloDatos();
        this.moduloEventos = new ModuloEventos();
        this.moduloComandos = new ModuloComandos();
        this.moduloItems = new ModuloItems();
        this.moduloEfectos = new ModuloEfectos();
        this.moduloGuis = new ModuloGuis();
        this.moduloChat = new ModuloChat();
        registrar(this.moduloConfiguracion);
        registrar(this.moduloMensajes);
        registrar(this.moduloDatos);
        registrar(this.moduloEventos);
        registrar(this.moduloComandos);
        registrar(this.moduloItems);
        registrar(this.moduloEfectos);
        registrar(this.moduloGuis);
        registrar(this.moduloChat);
    }

    /**
     * Atajo estático: crea un Nucleo con el anfitrión y el paquete indicados,
     * lo arranca y lo guarda como instancia "actual" accesible desde
     * {@link #actual()}. Devuelve la instancia por si el anfitrión quiere
     * llamarla luego en {@code onDisable()}.
     */
    public static Nucleo iniciar(JavaPlugin anfitrion, String paqueteBase) {
        Nucleo n = new Nucleo(anfitrion, paqueteBase);
        n.iniciar();
        return n;
    }

    /**
     * Última instancia de núcleo iniciada en la JVM, o {@code null} si no
     * hay ninguno vivo. Con varios plugins usando KSCore como dependencia
     * standalone, cada uno tiene su propio núcleo — este método devuelve
     * el último que arrancó, lo cual rara vez es lo que quieres.
     * Prefiere {@link #de(JavaPlugin)} pasando tu propio plugin.
     *
     * @deprecated en escenarios multi-plugin, usa {@link #de(JavaPlugin)}
     */
    @Deprecated
    public static Nucleo actual() {
        return actual;
    }

    /**
     * Devuelve el {@link Nucleo} asociado al {@link JavaPlugin} indicado,
     * o {@code null} si ese plugin todavía no arrancó su núcleo (o ya lo
     * detuvo). Este es el método correcto para código que corre dentro
     * de un plugin — el que llama sabe cuál es su propio plugin.
     */
    public static Nucleo de(JavaPlugin plugin) {
        return plugin == null ? null : INSTANCIAS.get(plugin);
    }

    public void iniciar() {
        long inicio = System.nanoTime();
        registro.info("Iniciando núcleo...");
        actual = this;
        INSTANCIAS.put(plugin, this);

        // Orden de arranque:
        // configuración → mensajes → datos → eventos → comandos → items → efectos → gui → chat.
        for (Modulo modulo : modulos) {
            try {
                modulo.iniciar(this);
                depurador.log("Nucleo", () -> "Módulo iniciado: " + modulo.nombre());
                if (modulo == moduloConfiguracion) aplicarConfigNucleo();
            } catch (Throwable t) {
                registro.error("Fallo al iniciar módulo " + modulo.nombre(), t);
            }
        }

        long ms = (System.nanoTime() - inicio) / 1_000_000L;
        registro.info("Núcleo iniciado en " + ms + "ms");
    }

    private void aplicarConfigNucleo() {
        if (!Archivos.estaRegistrado(ConfigNucleo.class)) return;
        ConfigNucleo cfg = Archivos.obtener(ConfigNucleo.class);
        depurador.activar(cfg.debug);
    }

    public void detener() {
        registro.info("Deteniendo núcleo...");
        for (int i = modulos.size() - 1; i >= 0; i--) {
            Modulo modulo = modulos.get(i);
            try {
                modulo.detener();
            } catch (Throwable t) {
                registro.error("Fallo al detener módulo " + modulo.nombre(), t);
            }
        }
        tareas.cancelarTodas();
        if (actual == this) actual = null;
        INSTANCIAS.remove(plugin, this);
        registro.info("Núcleo detenido.");
    }

    public void registrar(Modulo modulo) {
        modulos.add(modulo);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public String paqueteAnfitrion() {
        return paqueteAnfitrion;
    }

    public Registro registro() {
        return registro;
    }

    public Depurador depurador() {
        return depurador;
    }

    public Tareas tareas() {
        return tareas;
    }

    public EscanerClases escaner() {
        return escaner;
    }

    public ModuloMensajes mensajes() {
        return moduloMensajes;
    }

    public ModuloConfiguracion configuracion() {
        return moduloConfiguracion;
    }

    public ModuloComandos comandos() {
        return moduloComandos;
    }

    public ModuloEventos eventos() {
        return moduloEventos;
    }

    public ModuloDatos datos() {
        return moduloDatos;
    }

    public ModuloItems items() {
        return moduloItems;
    }

    public ModuloGuis guis() {
        return moduloGuis;
    }

    public ModuloEfectos efectos() {
        return moduloEfectos;
    }

    public ModuloChat chat() {
        return moduloChat;
    }
}
