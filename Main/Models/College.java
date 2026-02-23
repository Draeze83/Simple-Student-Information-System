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
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        if (parts.length >= 2) {
            return new College(
                parts[0].trim(), 
                parts[1].trim()
            );
        }
        return null;
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
