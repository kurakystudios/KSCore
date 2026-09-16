package studio.kuraky.kSCore.efectos;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import studio.kuraky.kSCore.datos.Datos;
import studio.kuraky.kSCore.datos.Filtro;
import studio.kuraky.kSCore.datos.Repositorio;
import studio.kuraky.kSCore.eventos.Eventos;
import studio.kuraky.kSCore.eventos.Prioridad;
import studio.kuraky.kSCore.eventos.Suscripcion;
import studio.kuraky.kSCore.mensajes.Mensajes;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;
import studio.kuraky.kSCore.nucleo.TareaCancelable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuloEfectos implements Modulo {

    /** Refresco de la barra de acción cada N ticks. */
    private static final long TICKS_BARRA_ACCION = 20L;

    private final Map<String, EfectoPersonalizado> registrados = new ConcurrentHashMap<>();
    private final Map<String, Efecto> metadata = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, EfectoActivo>> activos = new ConcurrentHashMap<>();
    private final List<Suscripcion> suscripciones = new ArrayList<>();

    private Nucleo nucleo;
    private Repositorio<RegistroEfecto, String> repositorio;
    private TareaCancelable tareaLoop;
    private TareaCancelable tareaBarraAccion;
    private boolean primario;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        // Solo el primer ModuloEfectos registra listeners globales y arranca
        // el loop; los demás delegan en la fachada Efectos.
        if (Efectos.disponible()) {
            this.primario = false;
            return;
        }
        this.primario = true;
        Efectos.inicializar(this);
        descubrirEfectos();
        conectarPersistencia();
        registrarEventos();
        arrancarLoop();
    }

    @Override
    public void detener() {
        if (!primario) {
            this.nucleo = null;
            return;
        }
        if (tareaLoop != null) tareaLoop.cancelar();
        if (tareaBarraAccion != null) tareaBarraAccion.cancelar();
        for (Suscripcion s : suscripciones) { try { s.cancelar(); } catch (Throwable ignored) {} }
        suscripciones.clear();
        // Notificar expiración a todos los efectos activos.
        for (Map<String, EfectoActivo> mp : activos.values()) {
            for (EfectoActivo ea : mp.values()) expirar(ea, false);
        }
        activos.clear();
        registrados.clear();
        metadata.clear();
        Efectos.desinicializar();
        this.nucleo = null;
    }

    // ---------- descubrimiento ----------

    private void descubrirEfectos() {
        for (Class<?> clase : nucleo.escaner().conAnotacion(Efecto.class)) {
            if (Modifier.isAbstract(clase.getModifiers())) continue;
            if (!EfectoPersonalizado.class.isAssignableFrom(clase)) {
                nucleo.registro().aviso(clase.getName() + " tiene @Efecto pero no extiende EfectoPersonalizado.");
                continue;
            }
            Efecto meta = clase.getAnnotation(Efecto.class);
            try {
                Constructor<?> ctor = clase.getDeclaredConstructor();
                ctor.setAccessible(true);
                EfectoPersonalizado ep = (EfectoPersonalizado) ctor.newInstance();
                registrados.put(meta.id(), ep);
                metadata.put(meta.id(), meta);
                nucleo.depurador().log("Efectos", () -> "Registrado " + meta.id() + " (" + clase.getSimpleName() + ")");
            } catch (NoSuchMethodException e) {
                nucleo.registro().error(clase.getName() + " necesita un constructor sin argumentos.");
            } catch (Throwable t) {
                nucleo.registro().error("No se pudo registrar el efecto " + meta.id(), t);
            }
        }
    }

    private void conectarPersistencia() {
        try {
            repositorio = Datos.repositorio(RegistroEfecto.class);
        } catch (Throwable t) {
            nucleo.registro().aviso("Persistencia de efectos deshabilitada: " + t.getMessage());
            repositorio = null;
        }
    }

    private void registrarEventos() {
        suscripciones.add(Eventos.escuchar(EntityDamageByEntityEvent.class, Prioridad.NORMAL, false, this::alGolpear));
        suscripciones.add(Eventos.escuchar(PlayerQuitEvent.class, Prioridad.NORMAL, false, this::alSalir));
        suscripciones.add(Eventos.escuchar(PlayerJoinEvent.class, Prioridad.NORMAL, false, this::alEntrar));
    }

    private void arrancarLoop() {
        // Bucle global 1 tick.
        tareaLoop = nucleo.tareas().repetir(1L, this::tick);
        tareaBarraAccion = nucleo.tareas().repetir(TICKS_BARRA_ACCION, this::refrescarBarrasAccion);
    }

    // ---------- API vanilla ----------

    void aplicarVanilla(Player jugador, PotionEffectType tipo, int nivel, Duration duracion) {
        int ticks = (int) Math.max(1, duracion.toMillis() / 50L);
        jugador.addPotionEffect(new PotionEffect(tipo, ticks, Math.max(0, nivel - 1), true, true, true));
    }

    void quitarVanilla(Player jugador, PotionEffectType tipo) {
        jugador.removePotionEffect(tipo);
    }

    // ---------- API personalizada ----------

    void aplicarPersonalizado(Player jugador, String id, int nivel, Duration duracion) {
        EfectoPersonalizado ep = registrados.get(id);
        Efecto meta = metadata.get(id);
        if (ep == null || meta == null) {
            throw new IllegalArgumentException("Efecto desconocido: " + id);
        }
        long ticks = Math.max(1L, duracion.toMillis() / 50L);
        Map<String, EfectoActivo> mapa = activos.computeIfAbsent(jugador.getUniqueId(), u -> new ConcurrentHashMap<>());
        EfectoActivo previo = mapa.get(id);
        int nivelFinal = nivel;
        long ticksFinales = ticks;
        if (previo != null) {
            switch (meta.apilado()) {
                case REEMPLAZAR -> {
                    // reemplazo total: nivel + duración del nuevo
                }
                case MAYOR_NIVEL -> {
                    nivelFinal = Math.max(previo.nivel(), nivel);
                    ticksFinales = Math.max(previo.ticksRestantes(), ticks);
                }
                case SUMAR_TIEMPO -> {
                    nivelFinal = Math.max(previo.nivel(), nivel);
                    ticksFinales = previo.ticksRestantes() + ticks;
                }
            }
            expirar(previo, false);
        }
        EfectoActivo activo = new EfectoActivo(jugador.getUniqueId(), id, nivelFinal, ticksFinales,
                Math.max(1, meta.intervaloTicks()), true);
        mapa.put(id, activo);
        try { ep.alAplicar(jugador, nivelFinal); }
        catch (Throwable t) { nucleo.registro().error("Efecto " + id + " alAplicar falló", t); }
        persistirSiCorresponde(id, activo);
    }

    void quitarPersonalizado(Player jugador, String id) {
        Map<String, EfectoActivo> mapa = activos.get(jugador.getUniqueId());
        if (mapa == null) return;
        EfectoActivo ea = mapa.remove(id);
        if (ea != null) expirar(ea, true);
    }

    boolean tienePersonalizado(Player jugador, String id) {
        Map<String, EfectoActivo> mapa = activos.get(jugador.getUniqueId());
        return mapa != null && mapa.containsKey(id);
    }

    List<EfectoActivo> activosDe(Player jugador) {
        List<EfectoActivo> salida = new ArrayList<>();
        Map<String, EfectoActivo> customs = activos.get(jugador.getUniqueId());
        if (customs != null) salida.addAll(customs.values());
        for (PotionEffect pe : jugador.getActivePotionEffects()) {
            salida.add(new EfectoActivo(jugador.getUniqueId(), pe.getType().getKey().getKey(),
                    pe.getAmplifier() + 1, pe.getDuration(), 1, false));
        }
        return salida;
    }

    Collection<String> idsRegistrados() {
        return List.copyOf(registrados.keySet());
    }

    // ---------- ciclo ----------

    private void tick() {
        if (activos.isEmpty()) return;
        for (Map.Entry<UUID, Map<String, EfectoActivo>> entrada : activos.entrySet()) {
            Player jugador = Bukkit.getPlayer(entrada.getKey());
            if (jugador == null) continue;
            Map<String, EfectoActivo> mapa = entrada.getValue();
            for (EfectoActivo ea : mapa.values()) {
                try {
                    if (ea.tocaTick()) {
                        EfectoPersonalizado ep = registrados.get(ea.id());
                        if (ep != null) ep.cadaTick(jugador, ea.nivel(), ea.ticksRestantes());
                    }
                    ea.tick();
                    if (ea.expirado()) {
                        mapa.remove(ea.id());
                        expirar(ea, true);
                    }
                } catch (Throwable t) {
                    nucleo.registro().error("Efecto " + ea.id() + " cadaTick falló", t);
                }
            }
        }
    }

    private void refrescarBarrasAccion() {
        if (activos.isEmpty()) return;
        for (Map.Entry<UUID, Map<String, EfectoActivo>> entrada : activos.entrySet()) {
            Player jugador = Bukkit.getPlayer(entrada.getKey());
            if (jugador == null) continue;
            List<Component> lineas = new ArrayList<>();
            for (EfectoActivo ea : entrada.getValue().values()) {
                Efecto meta = metadata.get(ea.id());
                if (meta == null || meta.mostrar() != Mostrar.BARRA_ACCION) continue;
                long segundos = ea.ticksRestantes() / 20;
                String texto = (meta.nombre().isEmpty() ? meta.id() : meta.nombre())
                        + " " + romanos(ea.nivel()) + " &7(" + segundos + "s)";
                lineas.add(Mensajes.parsear(texto));
            }
            if (lineas.isEmpty()) continue;
            Component barra = lineas.get(0);
            for (int i = 1; i < lineas.size(); i++) {
                barra = barra.append(Component.text(" ")).append(lineas.get(i));
            }
            jugador.sendActionBar(barra);
        }
    }

    // ---------- persistencia ----------

    private void persistirSiCorresponde(String id, EfectoActivo activo) {
        if (repositorio == null) return;
        Efecto meta = metadata.get(id);
        if (meta == null || !meta.persistente()) return;
        RegistroEfecto reg = new RegistroEfecto();
        reg.clave = RegistroEfecto.claveDe(activo.jugador(), id);
        reg.jugador = activo.jugador();
        reg.tipo = id;
        reg.nivel = activo.nivel();
        reg.ticksRestantes = activo.ticksRestantes();
        reg.guardadoEn = System.currentTimeMillis();
        repositorio.guardar(reg);
    }

    private void borrarPersistencia(UUID jugador, String id) {
        if (repositorio == null) return;
        Efecto meta = metadata.get(id);
        if (meta == null || !meta.persistente()) return;
        repositorio.eliminar(RegistroEfecto.claveDe(jugador, id));
    }

    private void expirar(EfectoActivo activo, boolean borrarBD) {
        EfectoPersonalizado ep = registrados.get(activo.id());
        Player jugador = Bukkit.getPlayer(activo.jugador());
        if (ep != null && jugador != null) {
            try { ep.alExpirar(jugador); }
            catch (Throwable t) { nucleo.registro().error("Efecto " + activo.id() + " alExpirar falló", t); }
        }
        if (borrarBD) borrarPersistencia(activo.jugador(), activo.id());
    }

    // ---------- eventos ----------

    private void alGolpear(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player jugador)) return;
        if (!(e.getEntity() instanceof LivingEntity objetivo)) return;
        Map<String, EfectoActivo> mapa = activos.get(jugador.getUniqueId());
        if (mapa == null || mapa.isEmpty()) return;
        for (EfectoActivo ea : mapa.values()) {
            EfectoPersonalizado ep = registrados.get(ea.id());
            if (ep == null) continue;
            try { ep.alGolpear(jugador, objetivo, e); }
            catch (Throwable t) { nucleo.registro().error("Efecto " + ea.id() + " alGolpear falló", t); }
        }
    }

    private void alSalir(PlayerQuitEvent e) {
        Map<String, EfectoActivo> mapa = activos.remove(e.getPlayer().getUniqueId());
        if (mapa == null) return;
        for (EfectoActivo ea : mapa.values()) {
            // Antes de perder al jugador, persistir el estado exacto para poder restaurar.
            persistirSiCorresponde(ea.id(), ea);
            EfectoPersonalizado ep = registrados.get(ea.id());
            if (ep != null) {
                try { ep.alExpirar(e.getPlayer()); }
                catch (Throwable t) { nucleo.registro().error("Efecto " + ea.id() + " alExpirar falló", t); }
            }
        }
    }

    private void alEntrar(PlayerJoinEvent e) {
        if (repositorio == null) return;
        UUID uid = e.getPlayer().getUniqueId();
        repositorio.buscar(Filtro.donde("jugador").igualA(uid), null, 100)
                .enHiloPrincipal(lista -> {
                    for (RegistroEfecto reg : lista) {
                        if (reg.ticksRestantes <= 0) {
                            repositorio.eliminar(reg.clave);
                            continue;
                        }
                        Efecto meta = metadata.get(reg.tipo);
                        if (meta == null) continue;
                        long ms = reg.ticksRestantes * 50L;
                        Player jugador = Bukkit.getPlayer(uid);
                        if (jugador == null) return;
                        aplicarPersonalizado(jugador, reg.tipo, reg.nivel, Duration.ofMillis(ms));
                    }
                });
    }

    private static String romanos(int n) {
        return switch (Math.max(1, Math.min(10, n))) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    /** Introspección para tests: número total de efectos activos entre todos los jugadores. */
    public int cantidadActivos() {
        int total = 0;
        for (Map<String, EfectoActivo> mp : activos.values()) total += mp.size();
        return total;
    }
}
