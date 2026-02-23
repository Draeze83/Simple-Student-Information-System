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
        escapeCSV(college);
    }

    public static Program fromCSV(String csvLine) {
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Handle commas in quotes
        if (parts.length >= 3) {
            return new Program(
                parts[0].trim(), 
                parts[1].trim(), 
                parts[2].trim()
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
