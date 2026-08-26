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
    // Warnings from the most recent loadData() call (e.g. skipped malformed rows)
    private List<String> lastLoadWarnings = new ArrayList<>();
    
    public DataManager(CSVManager csvManager) {
        this.csvManager = csvManager;
        loadData();
    }

    public void loadData() {
        List<String> warnings = new ArrayList<>();
        colleges = csvManager.loadColleges();
        warnings.addAll(csvManager.getLastLoadWarnings());
        programs = csvManager.loadPrograms();
        warnings.addAll(csvManager.getLastLoadWarnings());
        students = csvManager.loadStudents();
        warnings.addAll(csvManager.getLastLoadWarnings());
        lastLoadWarnings = warnings;

        enforceIntegrityAndUniqueness();
    }

    public List<String> getLastLoadWarnings() {
        return new ArrayList<>(lastLoadWarnings);
    }

    // Basic integrity checks and duplicate detection after loading from CSV
    private void enforceIntegrityAndUniqueness() {
        // Deduplicate colleges by normalized code (keep first)
        Map<String, College> uniqueColleges = new LinkedHashMap<>();
        for (College c : colleges) {
            if (c == null || c.getCode() == null) {
                continue;
            }
            String normalizedCode = normalizeComparisonKey(c.getCode());
            if (uniqueColleges.containsKey(normalizedCode)) {
                System.err.println("Duplicate college code detected in CSV, keeping first: " + c.getCode());
            } else {
                uniqueColleges.put(normalizedCode, c);
            }
        }
        colleges = new ArrayList<>(uniqueColleges.values());

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

        // Referential checks: program.college must exist (null is allowed)
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

        // Referential checks: student.programCode must exist (null is allowed)
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
        normalizeProgramForPersistence(program);
        if (validateProgram(program, null)) {
            programs.add(program);
            try {
                csvManager.savePrograms(programs);
                return true;
            } catch (IOException e) {
                programs.remove(program);
                throw new RuntimeException("Failed to save program: " + e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean updateProgram(String oldCode, Program newProgram) {
        for (int i = 0; i < programs.size(); i++) {
            if (Objects.equals(programs.get(i).getCode(), oldCode)) {
                lastValidationResult = ValidationResult.ok();
                lastValidationError = "";
                normalizeProgramForPersistence(newProgram);
                if (validateProgram(newProgram, oldCode)) {
                    Program oldProgram = programs.get(i);
                    List<Student> affectedStudents = new ArrayList<>();
                    for (Student s : students) {
                        if (Objects.equals(oldCode, s.getProgramCode())) {
                            affectedStudents.add(s);
                            s.setProgramCode(newProgram.getCode());
                        }
                    }

                    programs.set(i, newProgram);
                    try {
                        csvManager.savePrograms(programs);
                        csvManager.saveStudents(students);
                        return true;
                    } catch (IOException e) {
                        // Restore in-memory state, then re-sync disk so a partial
                        // write (one file saved, the other failed) can't persist.
                        programs.set(i, oldProgram);
                        for (Student s : affectedStudents) {
                            s.setProgramCode(oldCode);
                        }
                        try {
                            csvManager.savePrograms(programs);
                            csvManager.saveStudents(students);
                        } catch (IOException ignored) {
                            // best-effort resync; original failure is reported below
                        }
                        throw new RuntimeException("Failed to update program: " + e.getMessage(), e);
                    }
                }
            }
        }
        return false;
    }

    public boolean deleteProgram(String code) {
        // Null out program code for all students enrolled in this program
        List<Student> affectedStudents = new ArrayList<>();
        for (Student s : students) {
            if (code.equals(s.getProgramCode())) {
                affectedStudents.add(s);
                s.setProgramCode(null);
            }
        }
        if (!affectedStudents.isEmpty()) {
            try {
                csvManager.saveStudents(students);
            } catch (IOException e) {
                for (Student s : affectedStudents) {
                    s.setProgramCode(code);
                }
                throw new RuntimeException("Failed to update students after program deletion: " + e.getMessage(), e);
            }
        }

        int removedIndex = -1;
        Program removedProgram = null;
        for (int i = 0; i < programs.size(); i++) {
            if (Objects.equals(programs.get(i).getCode(), code)) {
                removedIndex = i;
                removedProgram = programs.get(i);
                break;
            }
        }
        if (removedProgram != null) {
            programs.remove(removedIndex);
            try {
                csvManager.savePrograms(programs);
            } catch (IOException e) {
                // Roll back the removal and the student un-enrollments, then re-sync disk.
                programs.add(removedIndex, removedProgram);
                for (Student s : affectedStudents) {
                    s.setProgramCode(code);
                }
                try {
                    csvManager.saveStudents(students);
                } catch (IOException ignored) {
                    // best-effort resync; original failure is reported below
                }
                throw new RuntimeException("Failed to delete program: " + e.getMessage(), e);
            }
            return true;
        }
        return false;
    }

    // College CRUD
    public List<College> getColleges() {
        return new ArrayList<>(colleges);
    }

    public boolean addCollege(College college) {
        lastValidationResult = ValidationResult.ok();
        lastValidationError = "";
        normalizeCollegeForPersistence(college);
        if (validateCollege(college, null)) {
            colleges.add(college);
            try {
                csvManager.saveColleges(colleges);
                return true;
            } catch (IOException e) {
                colleges.remove(college);
                throw new RuntimeException("Failed to save college: " + e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean updateCollege(String oldCode, String oldName, College newCollege) {
        for (int i = 0; i < colleges.size(); i++) {
            if (Objects.equals(colleges.get(i).getCode(), oldCode)) {
                lastValidationResult = ValidationResult.ok();
                lastValidationError = "";
                normalizeCollegeForPersistence(newCollege);
                if (validateCollege(newCollege, oldCode, oldName)) {
                    College oldCollege = colleges.get(i);
                    colleges.set(i, newCollege);

                    List<Program> affectedPrograms = new ArrayList<>();
                    for (Program p : programs) {
                        if (Objects.equals(oldCode, p.getCollege())) {
                            affectedPrograms.add(p);
                            p.setCollege(newCollege.getCode());
                        }
                    }

                    try {
                        csvManager.saveColleges(colleges);
                        csvManager.savePrograms(programs);
                        return true;
                    } catch (IOException e) {
                        // Restore in-memory state, then re-sync disk so a partial
                        // write (one file saved, the other failed) can't persist.
                        colleges.set(i, oldCollege);
                        for (Program p : affectedPrograms) {
                            p.setCollege(oldCode);
                        }
                        try {
                            csvManager.saveColleges(colleges);
                            csvManager.savePrograms(programs);
                        } catch (IOException ignored) {
                            // best-effort resync; original failure is reported below
                        }
                        throw new RuntimeException("Failed to update college: " + e.getMessage(), e);
                    }
                }
            }
        }
        return false;
    }

    public boolean deleteCollege(String code) {
        // Find all programs belonging to this college and null out their college reference.
        // Also cascade: any students enrolled in those programs get their program code nulled.
        List<Program> affectedPrograms = new ArrayList<>();
        List<String> affectedProgramCodes = new ArrayList<>();
        for (Program p : programs) {
            if (Objects.equals(code, p.getCollege())) {
                affectedPrograms.add(p);
                affectedProgramCodes.add(p.getCode());
                p.setCollege(null);
            }
        }

        // Track affected students and their old program codes for rollback.
        List<Student> affectedStudents = new ArrayList<>();
        List<String> affectedStudentOldCodes = new ArrayList<>();

        if (!affectedPrograms.isEmpty()) {
            try {
                csvManager.savePrograms(programs);
            } catch (IOException e) {
                for (Program p : affectedPrograms) {
                    p.setCollege(code);
                }
                throw new RuntimeException("Failed to update programs after college deletion: " + e.getMessage(), e);
            }

            // Cascade: students enrolled in any affected program lose their program code
            for (Student s : students) {
                if (s.getProgramCode() != null && affectedProgramCodes.contains(s.getProgramCode())) {
                    affectedStudents.add(s);
                    affectedStudentOldCodes.add(s.getProgramCode());
                    s.setProgramCode(null);
                }
            }
            if (!affectedStudents.isEmpty()) {
                try {
                    csvManager.saveStudents(students);
                } catch (IOException e) {
                    // Roll back students and programs, then re-sync the program file.
                    for (int k = 0; k < affectedStudents.size(); k++) {
                        affectedStudents.get(k).setProgramCode(affectedStudentOldCodes.get(k));
                    }
                    for (Program p : affectedPrograms) {
                        p.setCollege(code);
                    }
                    try {
                        csvManager.savePrograms(programs);
                    } catch (IOException ignored) {
                        // best-effort resync; original failure is reported below
                    }
                    throw new RuntimeException("Failed to update students after college deletion: " + e.getMessage(), e);
                }
            }
        }

        int removedIndex = -1;
        College removedCollege = null;
        for (int i = 0; i < colleges.size(); i++) {
            if (Objects.equals(colleges.get(i).getCode(), code)) {
                removedIndex = i;
                removedCollege = colleges.get(i);
                break;
            }
        }
        if (removedCollege != null) {
            colleges.remove(removedIndex);
            try {
                csvManager.saveColleges(colleges);
            } catch (IOException e) {
                // Roll back the whole cascade, then re-sync the other two files.
                colleges.add(removedIndex, removedCollege);
                for (int k = 0; k < affectedStudents.size(); k++) {
                    affectedStudents.get(k).setProgramCode(affectedStudentOldCodes.get(k));
                }
                for (Program p : affectedPrograms) {
                    p.setCollege(code);
                }
                try {
                    csvManager.savePrograms(programs);
                    csvManager.saveStudents(students);
                } catch (IOException ignored) {
                    // best-effort resync; original failure is reported below
                }
                throw new RuntimeException("Failed to delete college: " + e.getMessage(), e);
            }
            return true;
        }
        return false;
    }

    private String normalizeTextValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void normalizeProgramForPersistence(Program program) {
        if (program == null) {
            return;
        }
        program.setCode(normalizeTextValue(program.getCode()));
        program.setName(normalizeTextValue(program.getName()));
        program.setCollege(normalizeTextValue(program.getCollege()));
    }

    private void normalizeCollegeForPersistence(College college) {
        if (college == null) {
            return;
        }
        college.setCode(normalizeTextValue(college.getCode()));
        college.setName(normalizeTextValue(college.getName()));
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
        return validateCollege(college, oldCode, null);
    }

    private boolean validateCollege(College college, String oldCode, String oldName) {
        ValidationResult result = validateCollegeDetailed(college, oldCode, oldName);
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

        // Validate year is a single ASCII digit between 1-6 (rejects "+3", " 3",
        // "00", leading zeros, and non-ASCII digits that Integer.parseInt accepts).
        if (student.getYear() != null && !student.getYear().trim().isEmpty()) {
            if (!student.getYear().matches("[1-6]")) {
                result.addFieldError("year", "Year: must be a number between 1 and 6.");
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
                .anyMatch(p -> Objects.equals(p.getCode(), student.getProgramCode()));
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
        if (program.getCode() != null && program.getCode().length() > 20) {
            result.addFieldError("code", "Program Code: must be at most 20 characters.");
        }
        if (program.getName() == null || program.getName().length() > 100) {
            result.addFieldError("name", "Program Name: must not be empty and must be at most 100 characters.");
        }
        if (program.getCollege() != null && program.getCollege().length() > 20) {
            result.addFieldError("college", "College Code: must be at most 20 characters.");
        }
        
        // Check if empty
        if (program.getName() != null && program.getName().trim().isEmpty()) {
            result.addFieldError("name", "Program Name: must not be empty.");
        }
        
        // Validate code format: all letters capitalized, no numbers
        if (program.getCode() != null && !program.getCode().trim().isEmpty()
                && !program.getCode().matches("^[A-Z]{2,20}$")) {
            result.addFieldError("code", "Program Code: all alphabetical characters must be capitalized and must not contain any numbers.");
        }

        // "NULL" is the CSV sentinel for an absent code; reject it as a real code.
        if (program.getCode() != null && program.getCode().equalsIgnoreCase("NULL")) {
            result.addFieldError("code", "Program Code: \"NULL\" is a reserved value and cannot be used as a code.");
        }

        if (program.getCode() != null && program.getCode().trim().isEmpty()) {
            program.setCode(null);
        }
        
        // Validate name (letters, spaces, hyphens, apostrophes, parentheses only)
        if (program.getName() != null && !program.getName().trim().isEmpty()
                && !program.getName().matches("^[a-zA-Z\\s'\\-()]+$")) {
            result.addFieldError("name", "Program Name: letters, spaces, hyphens, apostrophes, and parentheses only.");
        }
        
        // Check for duplicate code (unless updating same program)
        if (program.getCode() != null && (oldCode == null || !Objects.equals(oldCode, program.getCode()))) {
            boolean codeExists = programs.stream()
                .anyMatch(p -> Objects.equals(p.getCode(), program.getCode()));
            if (codeExists) {
                result.addFieldError("code", "Program Code: must be unique.");
            }
        }
        
        // Check if college exists
        if (program.getCollege() != null && !program.getCollege().trim().isEmpty()) {
            boolean collegeExists = colleges.stream()
                .anyMatch(c -> Objects.equals(c.getCode(), program.getCollege()));
            if (!collegeExists) {
                result.addFieldError("college", "College Code: must refer to an existing college.");
            }
        }
        
        return result;
    }

    private ValidationResult validateCollegeDetailed(College college, String oldCode, String oldName) {
        ValidationResult result = ValidationResult.ok();

        if (college == null) {
            result.addGlobalError("College: data is missing.");
            return result;
        }
        
        // Length validation
        if (college.getCode() != null && college.getCode().length() > 20) {
            result.addFieldError("code", "College Code: must be at most 20 characters.");
        }
        if (college.getName() == null || college.getName().length() > 100) {
            result.addFieldError("name", "College Name: must not be empty and must be at most 100 characters.");
        }
        
        // Check empty
        if (college.getName() != null && college.getName().trim().isEmpty()) {
            result.addFieldError("name", "College Name: must not be empty.");
        }
        
        // Validate code format: capitalized, no numbers
        if (college.getCode() != null && !college.getCode().trim().isEmpty()
                && !college.getCode().matches("^[A-Z]{2,20}$")) {
            result.addFieldError("code", "College Code: must be capitalized and must not contain any numbers.");
        }

        // "NULL" is the CSV sentinel for an absent code; reject it as a real code.
        if (college.getCode() != null && college.getCode().equalsIgnoreCase("NULL")) {
            result.addFieldError("code", "College Code: \"NULL\" is a reserved value and cannot be used as a code.");
        }

        if (college.getCode() != null && college.getCode().trim().isEmpty()) {
            college.setCode(null);
        }
        
        // Validate name (letters, spaces, hyphens, apostrophes, parentheses only; no numbers)
        if (college.getName() != null && !college.getName().trim().isEmpty()
                && !college.getName().matches("^[a-zA-Z\\s'\\-()]+$")) {
            result.addFieldError("name", "College Name: must not contain any numbers and may only contain letters, spaces, hyphens, apostrophes, and parentheses.");
        }

        // Check for duplicate college code (unless updating same college)
        String normalizedNewCode = normalizeComparisonKey(college.getCode());
        String normalizedOldCode = oldCode == null ? null : normalizeComparisonKey(oldCode);
        if (normalizedNewCode != null && (oldCode == null || !normalizedNewCode.equals(normalizedOldCode))) {
            boolean codeExists = colleges.stream()
                .anyMatch(c -> normalizedNewCode.equals(normalizeComparisonKey(c.getCode()))
                    && (oldCode == null || !normalizeComparisonKey(c.getCode()).equals(normalizedOldCode)));
            if (codeExists) {
                result.addFieldError("code", "College Code: must be unique. Whitespace and punctuation differences are ignored.");
            }
        }

        String normalizedNewName = normalizeComparisonKey(college.getName());

        // Find the original college record for proper self-exclusion
        String normalizedOldName = oldName == null ? null : normalizeComparisonKey(oldName);
        final String lookupOldCode = normalizedOldCode;
        final String lookupOldName = normalizedOldName;
        final College originalCollege;
        if (lookupOldCode != null) {
            originalCollege = colleges.stream()
                .filter(c -> Objects.equals(normalizeComparisonKey(c.getCode()), lookupOldCode))
                .findFirst()
                .orElse(null);
        } else if (lookupOldName != null) {
            originalCollege = colleges.stream()
                .filter(c -> Objects.equals(normalizeComparisonKey(c.getName()), lookupOldName)
                    && normalizeComparisonKey(c.getCode()) == null)
                .findFirst()
                .orElse(null);
        } else {
            originalCollege = null;
        }

        String effectiveOldName = normalizedOldName;
        if (effectiveOldName == null && originalCollege != null) {
            effectiveOldName = normalizeComparisonKey(originalCollege.getName());
        }

        if (normalizedNewName != null && (normalizedOldName == null || !normalizedNewName.equals(normalizedOldName))) {
            boolean nameExists = colleges.stream()
                .anyMatch(c -> normalizedNewName.equals(normalizeComparisonKey(c.getName()))
                    && c != originalCollege);
            if (nameExists) {
                result.addFieldError("name", "College Name: must be unique. Whitespace and punctuation differences are ignored.");
            }
        }

        return result;
    }

    private String normalizeComparisonKey(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("[\\p{Punct}\\s]+", " ").toLowerCase();
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
        if (sanitizedQuery.trim().isEmpty()) {
            return getStudents();
        }

        String lowerQuery = sanitizedQuery.toLowerCase();
        return students.stream()
            .filter(s -> s.getId().toLowerCase().contains(lowerQuery)
                || s.getFirstName().toLowerCase().contains(lowerQuery)
                || s.getLastName().toLowerCase().contains(lowerQuery)
                || (s.getProgramCode() != null && s.getProgramCode().toLowerCase().contains(lowerQuery))
                || (s.getProgramCode() == null && "null".equals(lowerQuery))
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
                || (p.getCollege() != null && p.getCollege().toLowerCase().contains(lowerQuery)))
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
