package studio.kuraky.kSCore.comandos;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Analiza una clase anotada con {@link Comando} y construye el árbol
 * Brigadier correspondiente. Este trabajo se hace una sola vez en el
 * arranque; el árbol resultante se registra con Paper y no vuelve a
 * usar reflexión en tiempo de ejecución.
 */
public final class ConstructorArbol {

    private final EjecutorSeguro ejecutor;

    public ConstructorArbol(EjecutorSeguro ejecutor) {
        this.ejecutor = ejecutor;
    }

    public LiteralCommandNode<CommandSourceStack> construir(Class<?> clase, Object instancia, Comando meta) {
        List<DescriptorMetodo> metodos = analizar(clase, instancia, meta);
        NodoBuilder raiz = new NodoBuilder(Commands.literal(meta.nombre()));
        if (!meta.permiso().isEmpty()) {
            raiz.literal.requires(src -> src.getSender().hasPermission(meta.permiso()));
        }
        for (DescriptorMetodo m : metodos) {
            insertar(raiz, m, 0);
        }
        return finalizar(raiz).build();
    }

    List<DescriptorMetodo> analizar(Class<?> clase, Object instancia, Comando meta) {
        Map<String, Method> completadores = new HashMap<>();
        for (Method m : clase.getDeclaredMethods()) {
            Completar c = m.getAnnotation(Completar.class);
            if (c != null) {
                completadores.put(c.sub() + "|" + c.arg(), m);
            }
        }

        List<DescriptorMetodo> resultado = new ArrayList<>();
        for (Method m : clase.getDeclaredMethods()) {
            Principal p = m.getAnnotation(Principal.class);
            Sub s = m.getAnnotation(Sub.class);
            if (p == null && s == null) continue;
            if (p != null && s != null) {
                throw new IllegalStateException("El método " + m + " no puede tener @Principal y @Sub simultáneamente.");
            }

            String[] partes = s != null ? s.nombre().trim().split("\\s+") : new String[0];
            String permiso = (s != null && !s.permiso().isEmpty()) ? s.permiso() : meta.permiso();
            boolean soloJugador = (s != null && s.soloJugador()) || meta.soloJugador();
            String uso = s != null ? s.uso() : "";
            Cooldown cd = m.getAnnotation(Cooldown.class);
            int segundos = cd != null ? cd.segundos() : 0;
            String claveCd = cd != null ? cd.mensaje() : "";
            boolean async = m.isAnnotationPresent(Async.class);

            List<DescriptorParametro> params = new ArrayList<>();
            Parameter[] ps = m.getParameters();
            if (ps.length == 0 || !Contexto.class.isAssignableFrom(ps[0].getType())) {
                throw new IllegalStateException("El primer parámetro debe ser Contexto en " + clase.getName() + "#" + m.getName());
            }
            for (int i = 1; i < ps.length; i++) {
                Parameter par = ps[i];
                Arg a = par.getAnnotation(Arg.class);
                if (a == null) {
                    throw new IllegalStateException("Falta @Arg en " + clase.getName() + "#" + m.getName() + " parámetro " + i);
                }
                String nombre = a.value().isEmpty() ? par.getName() : a.value();
                boolean varargs = m.isVarArgs() && i == ps.length - 1;
                Class<?> tipo = par.getType();
                Convertidor<?> conv = varargs ? Convertidores.obtener(String[].class) : Convertidores.obtener(tipo);
                if (conv == null) {
                    throw new IllegalStateException("Tipo no soportado " + tipo + " en "
                            + clase.getName() + "#" + m.getName() + " parámetro " + par.getName());
                }
                DescriptorParametro dp = new DescriptorParametro(nombre, tipo, conv,
                        a.opcional(), a.defecto(), conv.absorbeResto());

                String claveSub = s != null ? s.nombre() : "";
                Method completador = completadores.get(claveSub + "|" + nombre);
                if (completador != null) {
                    completador.setAccessible(true);
                    dp.completadorPersonalizado(completador);
                }
                params.add(dp);
            }

            m.setAccessible(true);
            resultado.add(new DescriptorMetodo(instancia, m, partes, params, permiso,
                    soloJugador, uso, segundos, claveCd, async));
        }
        return resultado;
    }

    private static void insertar(NodoBuilder nodo, DescriptorMetodo m, int i) {
        if (i == m.partesRuta().length) {
            if (nodo.metodo != null) {
                throw new IllegalStateException("Ruta duplicada para " + m.rutaLegible());
            }
            nodo.metodo = m;
            return;
        }
        NodoBuilder hijo = nodo.hijos.computeIfAbsent(m.partesRuta()[i],
                k -> new NodoBuilder(Commands.literal(k)));
        insertar(hijo, m, i + 1);
    }

    private LiteralArgumentBuilder<CommandSourceStack> finalizar(NodoBuilder nodo) {
        DescriptorMetodo m = nodo.metodo;
        if (m != null && !m.permiso().isEmpty()) {
            nodo.literal.requires(src -> src.getSender().hasPermission(m.permiso()));
        }
        if (m != null) {
            adjuntarArgs(nodo.literal, m);
        }
        for (NodoBuilder hijo : nodo.hijos.values()) {
            nodo.literal.then(finalizar(hijo));
        }
        return nodo.literal;
    }

    private void adjuntarArgs(LiteralArgumentBuilder<CommandSourceStack> literal, DescriptorMetodo m) {
        List<DescriptorParametro> params = m.parametros();
        if (params.isEmpty()) {
            literal.executes(construirEjecutor(m, 0));
            return;
        }
        if (todosOpcionalesDesde(params, 0)) {
            literal.executes(construirEjecutor(m, 0));
        }
        literal.then(construirCadena(m, 0));
    }

    private ArgumentBuilder<CommandSourceStack, ?> construirCadena(DescriptorMetodo m, int desde) {
        DescriptorParametro p = m.parametros().get(desde);
        ArgumentType<String> tipo = p.absorbeResto()
                ? StringArgumentType.greedyString()
                : StringArgumentType.string();
        RequiredArgumentBuilder<CommandSourceStack, String> b = Commands.argument(p.nombre(), tipo);
        b.suggests((brigCtx, builder) -> {
            Contexto contexto = crearContexto(brigCtx, m, desde);
            String parcial = builder.getRemainingLowerCase();
            for (String s : obtenerSugerencias(m, p, contexto, parcial)) {
                if (s == null) continue;
                if (parcial.isEmpty() || s.toLowerCase(Locale.ROOT).startsWith(parcial)) {
                    builder.suggest(s);
                }
            }
            return builder.buildFuture();
        });
        if (todosOpcionalesDesde(m.parametros(), desde + 1)) {
            b.executes(construirEjecutor(m, desde + 1));
        }
        if (desde + 1 < m.parametros().size() && !p.absorbeResto()) {
            b.then(construirCadena(m, desde + 1));
        }
        return b;
    }

    private Command<CommandSourceStack> construirEjecutor(DescriptorMetodo m, int argsPresentes) {
        return brigCtx -> {
            Contexto contexto = crearContexto(brigCtx, m, argsPresentes);
            Object[] valores = new Object[m.parametros().size() + 1];
            valores[0] = contexto;
            try {
                for (int i = 0; i < m.parametros().size(); i++) {
                    DescriptorParametro p = m.parametros().get(i);
                    String texto;
                    if (i < argsPresentes) {
                        texto = StringArgumentType.getString(brigCtx, p.nombre());
                    } else {
                        texto = p.defecto();
                    }
                    if (texto == null || texto.isEmpty()) {
                        valores[i + 1] = valorPorDefecto(p.tipo());
                    } else {
                        valores[i + 1] = p.convertidor().convertir(contexto, texto);
                    }
                }
            } catch (ErrorArgumento ea) {
                if (ea.getMessage() != null) contexto.enviar(ea.getMessage());
                if (!m.uso().isEmpty()) {
                    contexto.enviarDe("comun.uso-incorrecto", Map.of("uso", m.uso()));
                }
                return Command.SINGLE_SUCCESS;
            }
            ejecutor.ejecutar(m, contexto, valores);
            return Command.SINGLE_SUCCESS;
        };
    }

    private static Contexto crearContexto(CommandContext<CommandSourceStack> brigCtx,
                                          DescriptorMetodo m, int argsPresentes) {
        String[] args = new String[argsPresentes];
        for (int i = 0; i < argsPresentes; i++) {
            args[i] = StringArgumentType.getString(brigCtx, m.parametros().get(i).nombre());
        }
        CommandSourceStack fuente = brigCtx.getSource();
        return new Contexto(fuente.getSender(), fuente.getLocation(), brigCtx.getInput(), args);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> obtenerSugerencias(DescriptorMetodo m, DescriptorParametro p,
                                                    Contexto ctx, String parcial) {
        if (p.completador() != null) {
            try {
                Object resultado = p.completador().invoke(m.instancia(), ctx, parcial);
                if (resultado instanceof List<?> lista) {
                    return (List<String>) lista;
                }
                if (resultado instanceof java.util.Collection<?> col) {
                    return new ArrayList<>((java.util.Collection<String>) col);
                }
            } catch (ReflectiveOperationException e) {
                return List.of();
            }
        }
        Convertidor conv = p.convertidor();
        return (List<String>) conv.sugerencias(ctx, parcial);
    }

    private static boolean todosOpcionalesDesde(List<DescriptorParametro> params, int desde) {
        for (int i = desde; i < params.size(); i++) {
            if (!params.get(i).opcional()) return false;
        }
        return true;
    }

    private static Object valorPorDefecto(Class<?> tipo) {
        if (tipo == boolean.class) return false;
        if (tipo == byte.class || tipo == short.class || tipo == int.class) return 0;
        if (tipo == long.class) return 0L;
        if (tipo == float.class) return 0.0f;
        if (tipo == double.class) return 0.0d;
        if (tipo == char.class) return '\0';
        return null;
    }

    private static final class NodoBuilder {
        final LiteralArgumentBuilder<CommandSourceStack> literal;
        final Map<String, NodoBuilder> hijos = new LinkedHashMap<>();
        DescriptorMetodo metodo;

        NodoBuilder(LiteralArgumentBuilder<CommandSourceStack> literal) {
            this.literal = literal;
        }
    }
}
