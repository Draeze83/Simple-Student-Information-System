package Main.Managers;

import Main.Models.Student;
import Main.Models.Program;
import Main.Models.College;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;

public class DataManager {
    private List<Student> students;
    private List<Program> programs;
    private List<College> colleges;
    private CSVManager csvManager;
    // Structured validation result for the last operation
    private ValidationResult lastValidationResult = ValidationResult.ok();
    // Backwards‑compatible message representation for callers that still use strings
    private String lastValidationError = "";
    
    public DataManager(CSVManager csvManager) {
        this.csvManager = csvManager;
        loadData();
    }

    public void loadData() {
        colleges = csvManager.loadColleges();
        programs = csvManager.loadPrograms();
        students = csvManager.loadStudents();

        enforceIntegrityAndUniqueness();
    }

    // Basic integrity checks and duplicate detection after loading from CSV
    private void enforceIntegrityAndUniqueness() {
        // Allow duplicate college codes; just filter out null entries
        List<College> validColleges = new ArrayList<>();
        for (College c : colleges) {
            if (c == null || c.getCode() == null) {
                continue;
            }
            validColleges.add(c);
        }
        colleges = validColleges;

        // Deduplicate programs by code (keep first)
        Map<String, Program> uniquePrograms = new LinkedHashMap<>();
        for (Program p : programs) {
            if (p == null || p.getCode() == null) continue;
            String code = p.getCode();
            if (uniquePrograms.containsKey(code)) {
                System.err.println("Duplicate program code detected in CSV, keeping first: " + code);
            } else {
                uniquePrograms.put(code, p);
            }
        }
        programs = new ArrayList<>(uniquePrograms.values());

        // Deduplicate students by ID (keep first)
        Map<String, Student> uniqueStudents = new LinkedHashMap<>();
        for (Student s : students) {
            if (s == null || s.getId() == null) continue;
            String id = s.getId();
            if (uniqueStudents.containsKey(id)) {
                System.err.println("Duplicate student ID detected in CSV, keeping first: " + id);
            } else {
                uniqueStudents.put(id, s);
            }
        }
        students = new ArrayList<>(uniqueStudents.values());

        // Referential checks: program.college must exist
        Set<String> collegeCodes = colleges.stream()
            .filter(c -> c.getCode() != null)
            .map(College::getCode)
            .collect(Collectors.toSet());
        for (Program p : programs) {
            if (p.getCollege() != null && !collegeCodes.contains(p.getCollege())) {
                System.err.println("Program references missing college code: " + p.getCode() +
                    " -> " + p.getCollege());
            }
        }

        // Referential checks: student.programCode must exist
        Set<String> programCodes = programs.stream()
            .filter(p -> p.getCode() != null)
            .map(Program::getCode)
            .collect(Collectors.toSet());
        for (Student s : students) {
            if (s.getProgramCode() != null && !programCodes.contains(s.getProgramCode())) {
                System.err.println("Student references missing program code: " + s.getId() +
                    " -> " + s.getProgramCode());
            }
        }
    }

    // Student CRUD
    public List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    public boolean addStudent(Student student) {
        lastValidationResult = ValidationResult.ok();
        lastValidationError = "";
        if (validateStudent(student, null)) {
            students.add(student);
            try {
                csvManager.saveStudents(students);
                return true;
            } catch (IOException e) {
                // Remove the student we just added since save failed
                students.remove(student);
                // Show error to user
                throw new RuntimeException("Failed to save student: " + e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean updateStudent(String oldId, Student newStudent) {
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getId().equals(oldId)) {
                lastValidationResult = ValidationResult.ok();
                lastValidationError = "";
                if (validateStudent(newStudent, oldId)) {
                    Student oldStudent = students.get(i);
                    students.set(i, newStudent);
                    try {
                        csvManager.saveStudents(students);
                        return true;
                    } catch (IOException e) {
                        // Restore the old student since save failed
                        students.set(i, oldStudent);
                        throw new RuntimeException("Failed to update student: " + e.getMessage(), e);
                    }
                }
            }
        }
        return false;
    }

    public boolean deleteStudent(String id) {
        boolean removed = false;
        Student removedStudent = null;
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getId().equals(id)) {
                removedStudent = students.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            try {
                csvManager.saveStudents(students);
            } catch (IOException e) {
                // Restore the student since save failed
                students.add(removedStudent);
                throw new RuntimeException("Failed to delete student: " + e.getMessage(), e);
            }
        }
        return removed;
    }

    // Program CRUD
    public List<Program> getPrograms() {
        return new ArrayList<>(programs);
    }

    public boolean addProgram(Program program) {
        lastValidationResult = ValidationResult.ok();
        lastValidationError = "";
        if (validateProgram(program, null)) {
            programs.add(program);
            csvManager.savePrograms(programs);
            return true;
        }
        return false;
    }

    public boolean updateProgram(String oldCode, Program newProgram) {
        for (int i = 0; i < programs.size(); i++) {
            if (programs.get(i).getCode().equals(oldCode)) {
                lastValidationResult = ValidationResult.ok();
                lastValidationError = "";
                if (validateProgram(newProgram, oldCode)) {
                    programs.set(i, newProgram);
                    csvManager.savePrograms(programs);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deleteProgram(String code) {
        boolean hasStudents = students.stream()
            .anyMatch(s -> s.getProgramCode().equals(code));
        
        if (hasStudents) {
            return false;
        }

        boolean removed = programs.removeIf(p -> p.getCode().equals(code));
        if (removed) {
            csvManager.savePrograms(programs);
        }
        return removed;
    }

    // College CRUD
    public List<College> getColleges() {
        return new ArrayList<>(colleges);
    }

    public boolean addCollege(College college) {
        lastValidationResult = ValidationResult.ok();
        lastValidationError = "";
        if (validateCollege(college, null)) {
            colleges.add(college);
            csvManager.saveColleges(colleges);
            return true;
        }
        return false;
    }

    public boolean updateCollege(String oldCode, College newCollege) {
        for (int i = 0; i < colleges.size(); i++) {
            if (colleges.get(i).getCode().equals(oldCode)) {
                lastValidationResult = ValidationResult.ok();
                lastValidationError = "";
                if (validateCollege(newCollege, oldCode)) {
                    colleges.set(i, newCollege);
                    csvManager.saveColleges(colleges);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deleteCollege(String code) {
        boolean hasPrograms = programs.stream()
            .anyMatch(p -> p.getCollege().equals(code));
        
        if (hasPrograms) {
            return false;
        }

        boolean removed = colleges.removeIf(c -> c.getCode().equals(code));
        if (removed) {
            csvManager.saveColleges(colleges);
        }
        return removed;
    }

    // ------------ ValidationResult helper type ------------
    public static class ValidationResult {
        private boolean ok;
        private Map<String, List<String>> fieldErrors;
        private List<String> globalErrors;

        private ValidationResult(boolean ok) {
            this.ok = ok;
        }

        public static ValidationResult ok() {
            ValidationResult vr = new ValidationResult(true);
            vr.fieldErrors = new HashMap<>();
            vr.globalErrors = new ArrayList<>();
            return vr;
        }

        public static ValidationResult fail() {
            ValidationResult vr = new ValidationResult(false);
            vr.fieldErrors = new HashMap<>();
            vr.globalErrors = new ArrayList<>();
            return vr;
        }

        public boolean isOk() {
            return ok;
        }

        public Map<String, List<String>> getFieldErrors() {
            return fieldErrors;
        }

        public List<String> getGlobalErrors() {
            return globalErrors;
        }

        public void addFieldError(String field, String message) {
            ok = false;
            fieldErrors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
        }

        public void addGlobalError(String message) {
            ok = false;
            globalErrors.add(message);
        }

        public String toMessage() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : fieldErrors.entrySet()) {
                for (String msg : entry.getValue()) {
                    sb.append("- ").append(msg).append("\n");
                }
            }
            for (String msg : globalErrors) {
                sb.append("- ").append(msg).append("\n");
            }
            return sb.toString().trim();
        }
    }

    // Validation methods
    private boolean validateStudent(Student student, String oldId) {
        ValidationResult result = validateStudentDetailed(student, oldId);
        lastValidationResult = result;
        lastValidationError = result.toMessage();
        return result.isOk();
    }

    private boolean validateProgram(Program program, String oldCode) {
        ValidationResult result = validateProgramDetailed(program, oldCode);
        lastValidationResult = result;
        lastValidationError = result.toMessage();
        return result.isOk();
    }

    private boolean validateCollege(College college, String oldCode) {
        ValidationResult result = validateCollegeDetailed(college, oldCode);
        lastValidationResult = result;
        lastValidationError = result.toMessage();
        return result.isOk();
    }

    // Detailed validation builders (populate structured errors)
    private ValidationResult validateStudentDetailed(Student student, String oldId) {
        ValidationResult result = ValidationResult.ok();

        if (student == null) {
            result.addGlobalError("Student: data is missing.");
            return result;
        }

        // Length validation FIRST
        if (student.getFirstName() == null || student.getFirstName().length() > 50) {
            result.addFieldError("firstName", "First name: must not be empty and must be at most 50 characters.");
        }
        if (student.getLastName() == null || student.getLastName().length() > 50) {
            result.addFieldError("lastName", "Last name: must not be empty and must be at most 50 characters.");
        }
        if (student.getProgramCode() == null || student.getProgramCode().length() > 20) {
            result.addFieldError("programCode", "Program code: must not be empty and must be at most 20 characters.");
        }
        if (student.getYear() == null || student.getYear().length() > 2) {
            result.addFieldError("year", "Year: must not be empty and must be at most 2 digits.");
        }
        if (student.getGender() == null || student.getGender().length() > 10) {
            result.addFieldError("gender", "Gender: invalid length.");
        }
        
        // Check for empty after trimming
        if (student.getFirstName() != null && student.getFirstName().trim().isEmpty()) {
            result.addFieldError("firstName", "First name: must not be empty.");
        }
        if (student.getLastName() != null && student.getLastName().trim().isEmpty()) {
            result.addFieldError("lastName", "Last name: must not be empty.");
        }
        if (student.getProgramCode() != null && student.getProgramCode().trim().isEmpty()) {
            result.addFieldError("programCode", "Program code: must not be empty.");
        }
        if (student.getYear() != null && student.getYear().trim().isEmpty()) {
            result.addFieldError("year", "Year: must not be empty.");
        }
        if (student.getGender() != null && student.getGender().trim().isEmpty()) {
            result.addFieldError("gender", "Gender: must not be empty.");
        }
        
        // Validate names contain only letters, spaces, hyphens, apostrophes
        if (student.getFirstName() != null && !student.getFirstName().trim().isEmpty()
                && !student.getFirstName().matches("^[a-zA-Z\\s'\\-]+$")) {
            result.addFieldError("firstName", "First name: letters, spaces, hyphens, and apostrophes only.");
        }
        if (student.getLastName() != null && !student.getLastName().trim().isEmpty()
                && !student.getLastName().matches("^[a-zA-Z\\s'\\-]+$")) {
            result.addFieldError("lastName", "Last name: letters, spaces, hyphens, and apostrophes only.");
        }
        
        // Validate ID format (YYYY-NNNN)
        if (student.getId() == null || !student.getId().matches("\\d{4}-\\d{4}")) {
            result.addFieldError("id", "Student ID: must match format YYYY-NNNN.");
        }

        // Check for duplicate ID (unless updating same student)
        if (student.getId() != null && (oldId == null || !oldId.equals(student.getId()))) {
            boolean idExists = students.stream()
                .anyMatch(s -> s.getId().equals(student.getId()));
            if (idExists) {
                result.addFieldError("id", "Student ID: must be unique.");
            }
        }

        // Validate year is integer between 1-6
        if (student.getYear() != null && !student.getYear().trim().isEmpty()) {
            try {
                int yearNum = Integer.parseInt(student.getYear());
                if (yearNum < 1 || yearNum > 6) {
                    result.addFieldError("year", "Year: must be a number between 1 and 6.");
                }
            } catch (NumberFormatException e) {
                result.addFieldError("year", "Year: must be a valid number.");
            }
        }

        // Validate gender against allowed values (match UI options)
        if (student.getGender() != null && !student.getGender().trim().isEmpty()) {
            if (!student.getGender().equals("M") && 
                !student.getGender().equals("F") && 
                !student.getGender().equals("Other")) {
                result.addFieldError("gender", "Gender: must be one of M, F, or Other.");
            }
        }

        // Check if program exists
        if (student.getProgramCode() != null && !student.getProgramCode().trim().isEmpty()) {
            boolean programExists = programs.stream()
                .anyMatch(p -> p.getCode().equals(student.getProgramCode()));
            if (!programExists) {
                result.addFieldError("programCode", "Program code: must refer to an existing program.");
            }
        }

        return result;
    }

    private ValidationResult validateProgramDetailed(Program program, String oldCode) {
        ValidationResult result = ValidationResult.ok();

        if (program == null) {
            result.addGlobalError("Program: data is missing.");
            return result;
        }
        
        // Length validation
        if (program.getCode() == null || program.getCode().length() > 20) {
            result.addFieldError("code", "Program Code: must not be empty and must be at most 20 characters.");
        }
        if (program.getName() == null || program.getName().length() > 100) {
            result.addFieldError("name", "Program Name: must not be empty and must be at most 100 characters.");
        }
        if (program.getCollege() == null || program.getCollege().length() > 20) {
            result.addFieldError("college", "College Code: must not be empty and must be at most 20 characters.");
        }
        
        // Check if empty
        if (program.getCode() != null && program.getCode().trim().isEmpty()) {
            result.addFieldError("code", "Program Code: must not be empty.");
        }
        if (program.getName() != null && program.getName().trim().isEmpty()) {
            result.addFieldError("name", "Program Name: must not be empty.");
        }
        if (program.getCollege() != null && program.getCollege().trim().isEmpty()) {
            result.addFieldError("college", "College Code: must not be empty.");
        }
        
        // Validate code format: all letters capitalized, no numbers
        if (program.getCode() != null && !program.getCode().trim().isEmpty()
                && !program.getCode().matches("^[A-Z]{2,20}$")) {
            result.addFieldError("code", "Program Code: all alphabetical characters must be capitalized and must not contain any numbers.");
        }
        
        // Validate name (letters, spaces, hyphens, apostrophes, parentheses only)
        if (program.getName() != null && !program.getName().trim().isEmpty()
                && !program.getName().matches("^[a-zA-Z\\s'\\-()]+$")) {
            result.addFieldError("name", "Program Name: letters, spaces, hyphens, apostrophes, and parentheses only.");
        }
        
        // Check for duplicate code (unless updating same program)
        if (program.getCode() != null && (oldCode == null || !oldCode.equals(program.getCode()))) {
            boolean codeExists = programs.stream()
                .anyMatch(p -> p.getCode().equals(program.getCode()));
            if (codeExists) {
                result.addFieldError("code", "Program Code: must be unique.");
            }
        }
        
        // Check if college exists
        if (program.getCollege() != null && !program.getCollege().trim().isEmpty()) {
            boolean collegeExists = colleges.stream()
                .anyMatch(c -> c.getCode().equals(program.getCollege()));
            if (!collegeExists) {
                result.addFieldError("college", "College Code: must refer to an existing college.");
            }
        }
        
        return result;
    }

    private ValidationResult validateCollegeDetailed(College college, String oldCode) {
        ValidationResult result = ValidationResult.ok();

        if (college == null) {
            result.addGlobalError("College: data is missing.");
            return result;
        }
        
        // Length validation
        if (college.getCode() == null || college.getCode().length() > 20) {
            result.addFieldError("code", "College Code: must not be empty and must be at most 20 characters.");
        }
        if (college.getName() == null || college.getName().length() > 100) {
            result.addFieldError("name", "College Name: must not be empty and must be at most 100 characters.");
        }
        
        // Check empty
        if (college.getCode() != null && college.getCode().trim().isEmpty()) {
            result.addFieldError("code", "College Code: must not be empty.");
        }
        if (college.getName() != null && college.getName().trim().isEmpty()) {
            result.addFieldError("name", "College Name: must not be empty.");
        }
        
        // Validate code format: capitalized, no numbers
        if (college.getCode() != null && !college.getCode().trim().isEmpty()
                && !college.getCode().matches("^[A-Z]{2,20}$")) {
            result.addFieldError("code", "College Code: must be capitalized and must not contain any numbers.");
        }
        
        // Validate name (letters, spaces, hyphens, apostrophes, parentheses only; no numbers)
        if (college.getName() != null && !college.getName().trim().isEmpty()
                && !college.getName().matches("^[a-zA-Z\\s'\\-()]+$")) {
            result.addFieldError("name", "College Name: must not contain any numbers and may only contain letters, spaces, hyphens, apostrophes, and parentheses.");
        }
        
        return result;
    }

    // Search functionality
    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getStudents();
        }

        // Sanitization of search input and
        // Limit query length
        String sanitizedQuery = sanitizeSearchQuery(query);
        if (sanitizedQuery.length() > 100) {
            sanitizedQuery = sanitizedQuery.substring(0, 100);
        }
        
        String lowerQuery = query.toLowerCase();
        return students.stream()
            .filter(s -> s.getId().toLowerCase().contains(lowerQuery)
                || s.getFirstName().toLowerCase().contains(lowerQuery)
                || s.getLastName().toLowerCase().contains(lowerQuery)
                || s.getProgramCode().toLowerCase().contains(lowerQuery)
                || s.getYear().toLowerCase().contains(lowerQuery)
                || s.getGender().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }

    private String sanitizeSearchQuery(String query) {
        // Remove potentially dangerous characters
        return query.replaceAll("[<>\"';\\\\]", "");
    }

    public List<Program> searchPrograms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getPrograms();
        }
        
        String lowerQuery = query.toLowerCase();
        return programs.stream()
            .filter(p -> p.getCode().toLowerCase().contains(lowerQuery)
                || p.getName().toLowerCase().contains(lowerQuery)
                || p.getCollege().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }

    public List<College> searchColleges(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getColleges();
        }
        
        String lowerQuery = query.toLowerCase();
        return colleges.stream()
            .filter(c -> c.getCode().toLowerCase().contains(lowerQuery)
                || c.getName().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }

    public String getValidationError() {
        if (lastValidationError == null || lastValidationError.trim().isEmpty()) {
            return "Validation failed. Check all required fields and referential integrity.";
        }
        return lastValidationError;
    }

    public String getLastValidationError() {
        return getValidationError();
    }

    public ValidationResult getLastValidationResult() {
        return lastValidationResult;
    }
}
