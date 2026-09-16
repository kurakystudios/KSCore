package studio.kuraky.kSCore.comandos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructorArbolTest {

    @Comando(nombre = "rango", alias = {"rank"}, permiso = "core.rango",
            descripcion = "Gestiona rangos")
    public static final class ComandoRango {

        @Principal
        public void raiz(Contexto ctx) {}

        @Sub(nombre = "dar", permiso = "core.rango.dar", uso = "<jugador> <rango>")
        public void dar(Contexto ctx,
                        @Arg String jugador,
                        @Arg("rango") String nivel) {}

        @Sub(nombre = "quitar")
        public void quitar(Contexto ctx, @Arg String jugador) {}

        @Sub(nombre = "lista")
        public void lista(Contexto ctx,
                          @Arg(opcional = true, defecto = "1") int pagina) {}

        @Sub(nombre = "grupo crear")
        public void grupoCrear(Contexto ctx, @Arg String nombre) {}

        @Sub(nombre = "grupo eliminar")
        public void grupoEliminar(Contexto ctx, @Arg String nombre) {}

        @Cooldown(segundos = 5)
        @Async
        @Sub(nombre = "buscar")
        public void buscar(Contexto ctx, @Arg String texto) {}

        @Completar(sub = "dar", arg = "rango")
        @SuppressWarnings("unused")
        public List<String> completarRangos(Contexto ctx, String parcial) {
            return List.of("miembro", "vip", "admin");
        }
    }

    private static ConstructorArbol nuevo() {
        return new ConstructorArbol(null);
    }

    private static Comando meta() {
        return ComandoRango.class.getAnnotation(Comando.class);
    }

    @Test
    void descubre_todos_los_metodos_anotados() {
        List<DescriptorMetodo> ds = nuevo().analizar(ComandoRango.class, new ComandoRango(), meta());
        assertEquals(7, ds.size());
    }

    @Test
    void principal_tiene_ruta_vacia() {
        List<DescriptorMetodo> ds = nuevo().analizar(ComandoRango.class, new ComandoRango(), meta());
        DescriptorMetodo principal = buscar(ds, "raiz");
        assertNotNull(principal);
        assertEquals(0, principal.partesRuta().length);
    }

    @Test
    void sub_con_espacios_produce_ruta_anidada() {
        List<DescriptorMetodo> ds = nuevo().analizar(ComandoRango.class, new ComandoRango(), meta());
        DescriptorMetodo crear = buscar(ds, "grupoCrear");
        assertNotNull(crear);
        assertEquals(2, crear.partesRuta().length);
        assertEquals("grupo", crear.partesRuta()[0]);
        assertEquals("crear", crear.partesRuta()[1]);
    }

    @Test
    void argumento_con_valor_toma_nombre_del_alias() {
        DescriptorMetodo dar = buscar(analizar(), "dar");
        assertNotNull(dar);
        assertEquals(2, dar.parametros().size());
        assertEquals("jugador", dar.parametros().get(0).nombre());
        assertEquals("rango", dar.parametros().get(1).nombre());
    }

    @Test
    void completar_asocia_completador_al_parametro() {
        DescriptorMetodo dar = buscar(analizar(), "dar");
        assertNull(dar.parametros().get(0).completador());
        assertNotNull(dar.parametros().get(1).completador());
    }

    @Test
    void opcional_y_defecto_capturados() {
        DescriptorMetodo lista = buscar(analizar(), "lista");
        DescriptorParametro p = lista.parametros().get(0);
        assertTrue(p.opcional());
        assertEquals("1", p.defecto());
        assertEquals(int.class, p.tipo());
    }

    @Test
    void permiso_de_sub_o_de_clase() {
        List<DescriptorMetodo> ds = analizar();
        assertEquals("core.rango.dar", buscar(ds, "dar").permiso());
        assertEquals("core.rango", buscar(ds, "quitar").permiso());
    }

    @Test
    void cooldown_y_async_capturados() {
        DescriptorMetodo buscar = buscar(analizar(), "buscar");
        assertEquals(5, buscar.segundosCooldown());
        assertTrue(buscar.async());
    }

    @Test
    void metodos_sin_contexto_fallan() {
        @Comando(nombre = "mal")
        class Malo {
            @SuppressWarnings("unused") @Principal public void raiz() {}
        }
        assertThrows(IllegalStateException.class,
                () -> nuevo().analizar(Malo.class, new Malo(), Malo.class.getAnnotation(Comando.class)));
    }

    @Test
    void construir_arbol_no_lanza_excepcion() {
        ConstructorArbol c = nuevo();
        assertNotNull(c.construir(ComandoRango.class, new ComandoRango(), meta()));
    }

    @Test
    void construir_arbol_incluye_hijos_correctos() {
        ConstructorArbol c = nuevo();
        var raiz = c.construir(ComandoRango.class, new ComandoRango(), meta());
        assertEquals("rango", raiz.getName());
        // Subcomandos esperados: dar, quitar, lista, grupo, buscar (5 literales)
        assertEquals(5, raiz.getChildren().size());
        // El literal "grupo" a su vez tiene dos hijos: crear, eliminar
        var grupo = raiz.getChild("grupo");
        assertNotNull(grupo);
        assertEquals(2, grupo.getChildren().size());
    }

    @Test
    void alias_sin_efecto_en_el_arbol_pero_disponibles_en_metadata() {
        // Los alias se registran al pasar al Commands API; aquí sólo comprobamos meta.
        assertEquals(1, meta().alias().length);
        assertEquals("rank", meta().alias()[0]);
    }

    @Test
    void metodos_sin_anotaciones_se_ignoran() {
        @Comando(nombre = "solo-ruido")
        class SoloRuido {
            @SuppressWarnings("unused") public void algo(Contexto ctx) {}
        }
        List<DescriptorMetodo> ds = nuevo().analizar(SoloRuido.class, new SoloRuido(),
                SoloRuido.class.getAnnotation(Comando.class));
        assertTrue(ds.isEmpty());
    }

    @Test
    void arg_faltante_en_parametros_falla() {
        @Comando(nombre = "sin-arg")
        class SinArg {
            @SuppressWarnings("unused") @Principal
            public void raiz(Contexto ctx, String texto) {}
        }
        assertFalse(false); // guard para el linter
        assertThrows(IllegalStateException.class,
                () -> nuevo().analizar(SinArg.class, new SinArg(),
                        SinArg.class.getAnnotation(Comando.class)));
    }

    private static DescriptorMetodo buscar(List<DescriptorMetodo> ds, String nombreMetodo) {
        for (DescriptorMetodo d : ds) if (d.metodo().getName().equals(nombreMetodo)) return d;
        return null;
    }

    private static List<DescriptorMetodo> analizar() {
        return nuevo().analizar(ComandoRango.class, new ComandoRango(), meta());
    }
}
