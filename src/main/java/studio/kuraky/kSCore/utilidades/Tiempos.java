package studio.kuraky.kSCore.utilidades;

import java.time.Duration;
import java.util.Optional;

public final class Tiempos {

    private Tiempos() {}

    public static Optional<Duration> parsear(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        long total = 0L;
        long numero = 0L;
        boolean numeroVisto = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) continue;
            if (Character.isDigit(c)) {
                numero = numero * 10L + (c - '0');
                numeroVisto = true;
                continue;
            }
            if (!numeroVisto) return Optional.empty();
            long segundos = switch (Character.toLowerCase(c)) {
                case 's' -> numero;
                case 'm' -> numero * 60L;
                case 'h' -> numero * 3600L;
                case 'd' -> numero * 86_400L;
                case 'w' -> numero * 604_800L;
                default -> -1L;
            };
            if (segundos < 0L) return Optional.empty();
            total += segundos;
            numero = 0L;
            numeroVisto = false;
        }
        if (numeroVisto) total += numero;
        return Optional.of(Duration.ofSeconds(total));
    }

    public static String formatear(Duration d) {
        long segundos = Math.max(0L, d.getSeconds());
        long dias = segundos / 86_400L; segundos %= 86_400L;
        long horas = segundos / 3600L; segundos %= 3600L;
        long minutos = segundos / 60L; segundos %= 60L;
        StringBuilder sb = new StringBuilder();
        if (dias > 0) sb.append(dias).append("d");
        if (horas > 0) sb.append(horas).append("h");
        if (minutos > 0) sb.append(minutos).append("m");
        if (segundos > 0 || sb.isEmpty()) sb.append(segundos).append("s");
        return sb.toString();
    }
}
