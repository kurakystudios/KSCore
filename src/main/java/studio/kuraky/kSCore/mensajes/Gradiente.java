package studio.kuraky.kSCore.mensajes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;

public final class Gradiente {

    private Gradiente() {}

    public static Component aplicar(String texto, List<TextColor> paradas, Style estiloBase) {
        if (texto == null || texto.isEmpty()) return Component.empty();
        if (paradas == null || paradas.isEmpty()) return Component.text(texto, estiloBase);
        if (paradas.size() == 1) return Component.text(texto, estiloBase.color(paradas.get(0)));

        int longitud = texto.length();
        TextComponent.Builder salida = Component.text();
        for (int i = 0; i < longitud; i++) {
            double t = longitud == 1 ? 0.0 : (double) i / (longitud - 1);
            TextColor color = interpolar(paradas, t);
            salida.append(Component.text(String.valueOf(texto.charAt(i)), estiloBase.color(color)));
        }
        return salida.build();
    }

    static TextColor interpolar(List<TextColor> paradas, double t) {
        int segmentos = paradas.size() - 1;
        double posicion = t * segmentos;
        int indice = (int) Math.floor(posicion);
        if (indice >= segmentos) indice = segmentos - 1;
        if (indice < 0) indice = 0;
        double local = posicion - indice;
        TextColor a = paradas.get(indice);
        TextColor b = paradas.get(indice + 1);
        int r = (int) Math.round(a.red()   + (b.red()   - a.red())   * local);
        int g = (int) Math.round(a.green() + (b.green() - a.green()) * local);
        int azul = (int) Math.round(a.blue()  + (b.blue()  - a.blue())  * local);
        return TextColor.color(limitar(r), limitar(g), limitar(azul));
    }

    private static int limitar(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }
}
