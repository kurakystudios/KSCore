package studio.kuraky.kSCore.mensajes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalizadorTest {

    private final Analizador analizador = new Analizador();

    private String plano(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    private TextColor primerColor(Component c) {
        if (c.color() != null) return c.color();
        for (Component hijo : c.children()) {
            TextColor color = primerColor(hijo);
            if (color != null) return color;
        }
        return null;
    }

    private boolean tieneDecoracion(Component c, TextDecoration decoracion) {
        if (c.decoration(decoracion) == TextDecoration.State.TRUE) return true;
        for (Component hijo : c.children()) {
            if (tieneDecoracion(hijo, decoracion)) return true;
        }
        return false;
    }

    @Test
    @DisplayName("1: texto plano")
    void textoPlano() {
        assertEquals("hola mundo", plano(analizador.analizar("hola mundo")));
    }

    @Test
    @DisplayName("2: color legado &a")
    void colorLegado() {
        Component c = analizador.analizar("&aHola");
        assertEquals("Hola", plano(c));
        assertEquals(NamedTextColor.GREEN, primerColor(c));
    }

    @Test
    @DisplayName("3: reset &r elimina color previo")
    void resetLimpia() {
        Component c = analizador.analizar("&aHola&rMundo");
        assertEquals("HolaMundo", plano(c));
    }

    @Test
    @DisplayName("4: formato negrita &l")
    void negrita() {
        Component c = analizador.analizar("&lTexto");
        assertEquals("Texto", plano(c));
        assertTrue(tieneDecoracion(c, TextDecoration.BOLD));
    }

    @Test
    @DisplayName("5: combinación &l&a")
    void combinacionFormatoColor() {
        Component c = analizador.analizar("&l&aTexto");
        assertEquals("Texto", plano(c));
        assertEquals(NamedTextColor.GREEN, primerColor(c));
        assertTrue(tieneDecoracion(c, TextDecoration.BOLD));
    }

    @Test
    @DisplayName("6: color hex #ff00aa")
    void colorHex() {
        Component c = analizador.analizar("#ff00aaTexto");
        assertEquals("Texto", plano(c));
        TextColor color = primerColor(c);
        assertNotNull(color);
        assertEquals(0xff00aa, color.value());
    }

    @Test
    @DisplayName("7: gradiente de 2 paradas")
    void gradienteDosStops() {
        Component c = analizador.analizar("<ff0000>ABC<0000ff>");
        assertEquals("ABC", plano(c));
        // El primer carácter arranca en la primera parada.
        TextColor color = primerColor(c);
        assertNotNull(color);
        assertEquals(0xff0000, color.value());
    }

    @Test
    @DisplayName("8: gradiente de 3 paradas")
    void gradienteTresStops() {
        Component c = analizador.analizar("<ff0000><00ff00>ABCD<0000ff>");
        assertEquals("ABCD", plano(c));
    }

    @Test
    @DisplayName("9: gradiente combinado con negrita")
    void gradienteConNegrita() {
        Component c = analizador.analizar("&l<ff0000>Neg<00ff00>");
        assertEquals("Neg", plano(c));
        assertTrue(tieneDecoracion(c, TextDecoration.BOLD));
    }

    @Test
    @DisplayName("10: escape \\n produce salto de línea")
    void saltoDeLinea() {
        Component c = analizador.analizar("linea1\\nlinea2");
        assertEquals("linea1\nlinea2", plano(c));
    }

    @Test
    @DisplayName("11: placeholder con valor")
    void placeholderConValor() {
        Component c = analizador.analizar("Hola %nombre%", Map.of("nombre", "Ana"));
        assertEquals("Hola Ana", plano(c));
    }

    @Test
    @DisplayName("12: placeholder faltante se deja literal")
    void placeholderFaltante() {
        Component c = analizador.analizar("Hola %nombre%", Map.of());
        assertEquals("Hola %nombre%", plano(c));
    }

    @Test
    @DisplayName("13: hex mal formado se deja literal sin excepción")
    void hexMalFormado() {
        Component c = analizador.analizar("#zz0000texto");
        assertEquals("#zz0000texto", plano(c));
    }

    @Test
    @DisplayName("14: gradiente mal formado se deja literal")
    void gradienteMalFormado() {
        Component c = analizador.analizar("<gg0000>texto");
        assertEquals("<gg0000>texto", plano(c));
    }

    @Test
    @DisplayName("15: cadena vacía → Component.empty()")
    void cadenaVacia() {
        assertEquals(Component.empty(), analizador.analizar(""));
    }

    @Test
    @DisplayName("16: null → Component.empty()")
    void nulo() {
        assertEquals(Component.empty(), analizador.analizar(null));
    }

    @Test
    @DisplayName("17: gradiente sin texto entre paradas")
    void gradienteSinTexto() {
        Component c = analizador.analizar("<ff0000><00ff00>");
        assertEquals("", plano(c));
    }

    @Test
    @DisplayName("18: color seguido de reset y texto")
    void colorLuegoReset() {
        Component c = analizador.analizar("&aa&rb");
        assertEquals("ab", plano(c));
    }

    @Test
    @DisplayName("19: formatos combinados &l&o&n")
    void formatosCombinados() {
        Component c = analizador.analizar("&l&o&nTexto");
        assertEquals("Texto", plano(c));
        assertTrue(tieneDecoracion(c, TextDecoration.BOLD));
        assertTrue(tieneDecoracion(c, TextDecoration.ITALIC));
        assertTrue(tieneDecoracion(c, TextDecoration.UNDERLINED));
    }

    @Test
    @DisplayName("20: cache hit — dos llamadas al mismo string retornan el mismo Component")
    void cacheHitMismoComponent() {
        Component a = analizador.analizar("&l<ff0000>Hola<00ff00>");
        Component b = analizador.analizar("&l<ff0000>Hola<00ff00>");
        assertSame(a, b);
    }

    @Test
    @DisplayName("21: sinFormato quita códigos y devuelve texto plano")
    void sinFormato() {
        assertEquals("Hola mundo", analizador.sinFormato("&a&l<ff0000>Hola mundo<00ff00>"));
    }

    @Test
    @DisplayName("22: 100 000 parseos cacheados quedan por debajo del umbral")
    void perfCacheHits() {
        String texto = "&l<ff0000>Hola gradiente<00ff00>";
        // Warmup para que el JIT se active y la entrada quede cacheada.
        for (int i = 0; i < 10_000; i++) analizador.analizar(texto);
        long inicio = System.nanoTime();
        for (int i = 0; i < 100_000; i++) analizador.analizar(texto);
        long ms = (System.nanoTime() - inicio) / 1_000_000L;
        // El plan aspira a < 5 ms; permitimos margen amplio para CI compartidos.
        assertTrue(ms < 200L, "100k parseos tardaron " + ms + "ms (esperado < 200ms)");
    }

    @Test
    @DisplayName("Interpolación de gradiente en centro devuelve color medio")
    void interpolacionGradienteCentro() {
        TextColor color = Gradiente.interpolar(
                java.util.List.of(TextColor.color(0xff0000), TextColor.color(0x0000ff)), 0.5);
        // A t=0.5 con paso completo entre rojo y azul, el punto medio es (128, 0, 128).
        assertEquals(128, color.red());
        assertEquals(0, color.green());
        assertEquals(128, color.blue());
    }

    @Test
    @DisplayName("sin color por defecto: primer color de texto plano es null")
    void textoSinColor() {
        assertNull(primerColor(analizador.analizar("plano")));
    }
}
