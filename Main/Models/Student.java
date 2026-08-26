package Main.Models;

public class Student {
    private String id;
    private String firstName;
    private String lastName;
    private String programCode;
    private String year;
    private String gender;

    public Student(String id, String firstName, String lastName, String programCode, String year, String gender) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.programCode = programCode;
        this.year = year;
        this.gender = gender;
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProgramCode() { return programCode; }
    public String getYear() { return year; }
    public String getGender() { return gender; }

    public void setId(String id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public void setYear(String year) { this.year = year; }
    public void setGender(String gender) { this.gender = gender; }

    public String toCSV() {
        return escapeCSV(id) + "," +    
        escapeCSV(firstName) + "," + 
        escapeCSV(lastName) + "," + 
        encodeProgramCode(programCode) + "," + 
        escapeCSV(year) + "," + 
        escapeCSV(gender);
    }

    // Encodes a null or blank program code as the literal "NULL" in the CSV
    private static String encodeProgramCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "NULL";
        }
        return escapeCSV(value);
    }

    public static Student fromCSV(String csvLine) {
        // Split on commas that are not inside double quotes; keep trailing empty fields
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length >= 6) {
            String programCode = unescapeCSV(parts[3]);
            // Treat the literal token "NULL" as a null program code
            if (programCode != null && programCode.equalsIgnoreCase("NULL")) {
                programCode = null;
            }
            return new Student(
                unescapeCSV(parts[0]),
                unescapeCSV(parts[1]),
                unescapeCSV(parts[2]),
                programCode,
                unescapeCSV(parts[4]),
                unescapeCSV(parts[5])
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

    // This prevents CSV injection
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
