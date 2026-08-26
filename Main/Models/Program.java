package Main.Models;

public class Program {
    private String code;
    private String name;
    private String college;

    public Program(String code, String name, String college) {
        this.code = code;
        this.name = name;
        this.college = college;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCollege() { return college; }

    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setCollege(String college) { this.college = college; }

    public String toCSV() {
        return escapeCSV(code) + "," + 
        escapeCSV(name) + "," + 
        encodeCollege(college);
    }

    // Encodes a null or blank college code as the literal "NULL" in the CSV
    private static String encodeCollege(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "NULL";
        }
        return escapeCSV(value);
    }

    public static Program fromCSV(String csvLine) {
        // Handle commas in quotes; keep trailing empty fields
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length >= 3) {
            String college = unescapeCSV(parts[2]);
            // Treat the literal token "NULL" as a null college code
            if (college != null && college.equalsIgnoreCase("NULL")) {
                college = null;
            }
            return new Program(
                unescapeCSV(parts[0]),
                unescapeCSV(parts[1]),
                college
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
