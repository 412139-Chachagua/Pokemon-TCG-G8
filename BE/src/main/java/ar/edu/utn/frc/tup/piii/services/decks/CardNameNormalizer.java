package ar.edu.utn.frc.tup.piii.services.decks;

public class CardNameNormalizer {

    public static String normalize(String name) {
        if (name == null) return null;
        String result = name.trim();
        result = result.replaceAll("\\s+Nv\\..*", "");
        result = result.replaceAll("(?i)\\s+del\\s+Equipo\\s+Plasma", "");
        result = result.replaceAll("(?i)\\s+Equipo\\s+Plasma", "");
        result = result.replaceAll("\\s+δ", "");
        result = result.replaceAll("\\s*δ\\s*", "");
        return result.trim();
    }
}
