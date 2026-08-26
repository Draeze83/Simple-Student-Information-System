package Main.Models;

public class College {
    private String code;
    private String name;

    public College(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }

    public String toCSV() {
        return escapeCSV(code) + "," + escapeCSV(name);
    }

    public static College fromCSV(String csvLine) {
        // Split on commas outside quotes; keep trailing empty fields
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length >= 2) {
            return new College(
                unescapeCSV(parts[0]),
                unescapeCSV(parts[1])
            );
        }
        return null;
    }

    // Reverses escapeCSV: strips surrounding quotes, collapses doubled quotes,
    // and removes the leading apostrophe added as an injection guard.
    private static String unescapeCSV(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).replace("\"\"", "\"");
        } else if (v.startsWith("'=") || v.startsWith("'+")) {
            v = v.substring(1);
        }
        return v;
    }

    // CSV injection prevention
    private static String escapeCSV(String value) {
        if (value == null) return "";

        value = value.replace("\"", "\"\"");
        
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value + "\"";
        }
        if (value.startsWith("=") || value.startsWith("+")) {
            value = "'" + value;
        }
        return value;
    }
}
