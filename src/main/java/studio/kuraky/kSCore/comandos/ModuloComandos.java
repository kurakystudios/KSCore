package studio.kuraky.kSCore.comandos;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import studio.kuraky.kSCore.nucleo.Modulo;
import studio.kuraky.kSCore.nucleo.Nucleo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class ModuloComandos implements Modulo {

    private final Cooldowns cooldowns = new Cooldowns();
    private final List<Registro> pendientes = new ArrayList<>();

    private Nucleo nucleo;
    private EjecutorSeguro ejecutorSeguro;
    private ConstructorArbol constructor;

    @Override
    public void iniciar(Nucleo nucleo) {
        this.nucleo = nucleo;
        this.ejecutorSeguro = new EjecutorSeguro(nucleo.tareas(), nucleo.registro(),
                nucleo.depurador(), cooldowns);
        this.constructor = new ConstructorArbol(ejecutorSeguro);
        Comandos.inicializar(this);

        for (Class<?> clase : nucleo.escaner().conAnotacion(Comando.class)) {
            if (Modifier.isAbstract(clase.getModifiers())) continue;
            try {
                Constructor<?> ctor = clase.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object instancia = ctor.newInstance();
                if (instancia instanceof Condicional c && !c.debeRegistrarse()) {
                    nucleo.depurador().log("Comandos",
                            () -> "Saltado " + clase.getSimpleName() + " (condicional=false).");
                    continue;
                }
                registrar(instancia);
            } catch (NoSuchMethodException e) {
                nucleo.registro().error("La clase " + clase.getName()
                        + " necesita un constructor sin argumentos para registrarse como @Comando.");
            } catch (ReflectiveOperationException e) {
                nucleo.registro().error("No se pudo instanciar " + clase.getName(), e);
            } catch (Throwable t) {
                nucleo.registro().error("No se pudo construir el árbol de " + clase.getName(), t);
            }
        }

        nucleo.plugin().getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, evento -> {
            Commands api = evento.registrar();
            for (Registro r : pendientes) {
                api.register(r.nodo, r.descripcion.isEmpty() ? null : r.descripcion, r.alias);
            }
        });

        nucleo.depurador().log("Comandos", () -> "Registrados: " + pendientes.size());
    }

    @Override
    public void detener() {
        Comandos.desinicializar();
        cooldowns.limpiar();
        pendientes.clear();
        this.constructor = null;
        this.ejecutorSeguro = null;
        this.nucleo = null;
    }

    public void registrar(Object instancia) {
        Comando meta = instancia.getClass().getAnnotation(Comando.class);
        if (meta == null) {
            throw new IllegalArgumentException("La clase " + instancia.getClass().getName()
                    + " no tiene @Comando.");
        }
        LiteralCommandNode<CommandSourceStack> nodo =
                constructor.construir(instancia.getClass(), instancia, meta);
        Registro r = new Registro();
        r.instancia = instancia;
        r.nodo = nodo;
        r.descripcion = meta.descripcion();
        r.alias = Arrays.asList(meta.alias());
        r.nombre = meta.nombre();
        r.descriptores = constructor.analizar(instancia.getClass(), instancia, meta);
        pendientes.add(r);
    }

    Collection<String> nombresRegistrados() {
        return pendientes.stream().map(r -> r.nombre).collect(Collectors.toList());
    }

    List<DescriptorMetodo> descriptores() {
        List<DescriptorMetodo> salida = new ArrayList<>();
        for (Registro r : pendientes) salida.addAll(r.descriptores);
        return salida;
    }

    private static final class Registro {
        Object instancia;
        String nombre;
        String descripcion;
        List<String> alias;
        LiteralCommandNode<CommandSourceStack> nodo;
        List<DescriptorMetodo> descriptores;
    }
}
