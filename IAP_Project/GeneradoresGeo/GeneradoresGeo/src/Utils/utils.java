package Utils;

public class utils {
    public static String limpiar(String texto) {
        if (texto == null) return null;
        return texto.replaceAll("['\"]", "").trim();
    }
    public static String normalizar(String texto) {
        if (texto == null) return null;
        return texto.replace(",", ".").trim();
    }
}