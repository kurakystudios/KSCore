package studio.kuraky.kSCore.utilidades;

import java.util.Locale;
import java.util.Optional;

public final class Numeros {

    private Numeros() {}

    public static Optional<Integer> parseEntero(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> parseLargo(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDoble(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static int limitar(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }

    public static long limitar(long valor, long min, long max) {
        return Math.max(min, Math.min(max, valor));
    }

    public static double limitar(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
    }

    public static String formatear(double valor) {
        return String.format(Locale.ROOT, "%,.2f", valor);
    }

    public static String formatear(long valor) {
        return String.format(Locale.ROOT, "%,d", valor);
    }
}
