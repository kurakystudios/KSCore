package studio.kuraky.kSCore.utilidades;

public final class Textos {

    private Textos() {}

    public static boolean esVacio(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean esBlanco(String s) {
        return s == null || s.isBlank();
    }

    public static String primeraMayuscula(String s) {
        if (esVacio(s)) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static String repetir(String s, int veces) {
        if (esVacio(s) || veces <= 0) return "";
        return s.repeat(veces);
    }

    public static String truncar(String s, int max) {
        if (s == null) return "";
        if (max <= 0) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    public static String unirNoVacios(String separador, String... partes) {
        StringBuilder sb = new StringBuilder();
        boolean primero = true;
        for (String p : partes) {
            if (esBlanco(p)) continue;
            if (!primero) sb.append(separador);
            sb.append(p);
            primero = false;
        }
        return sb.toString();
    }
}
