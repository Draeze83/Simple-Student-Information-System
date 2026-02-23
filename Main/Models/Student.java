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
        escapeCSV(programCode) + "," + 
        escapeCSV(year) + "," + 
        escapeCSV(gender);
    }

    public static Student fromCSV(String csvLine) {
        // Split on commas that are not inside double quotes
        String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        if (parts.length >= 6) {
            return new Student(
                parts[0].trim(), 
                parts[1].trim(), 
                parts[2].trim(), 
                parts[3].trim(), 
                parts[4].trim(), 
                parts[5].trim()
            );
        }
        return null;
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
