package Main.Managers;

import Main.Models.Student;
import Main.Models.Program;
import Main.Models.College;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CSVManager {
    private String studentFile;
    private String programFile;
    private String collegeFile;

public CSVManager(String studentFile, String programFile, String collegeFile) 
        throws SecurityException {
    this.studentFile = validateFilePath(studentFile);
    this.programFile = validateFilePath(programFile);
    this.collegeFile = validateFilePath(collegeFile);
}

    // File path validation to prevent directory traversal
    private String validateFilePath(String filePath) throws SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }
        
        try {
            File file = new File(filePath);
            String canonicalPath = file.getCanonicalPath();
            
            // Ensure file is within the current working directory tree
            File allowedDir = new File(".").getCanonicalFile();
            if (!new File(canonicalPath).toPath().normalize().startsWith(allowedDir.toPath().normalize())) {
                throw new SecurityException("File path outside allowed directory: " + filePath);
            }
            
            // Check for suspicious patterns
            if (filePath.contains("..") || filePath.contains("~")) {
                throw new SecurityException("Invalid file path pattern: " + filePath);
            }
            
            // Ensure that the extension is CSV
            if (!filePath.toLowerCase().endsWith(".csv")) {
                throw new SecurityException("Only CSV files allowed");
            }
            
            return canonicalPath;
            
        } catch (IOException e) {
            throw new SecurityException("Invalid file path: " + filePath, e);
        }
    }

    // Student operations
    public List<Student> loadStudents() {
        List<Student> students = new ArrayList<>();
        File file = new File(studentFile);
        if (!file.exists()) {
            return students;
        }

        // Simple read without OS-level locks to avoid cross-handle conflicts,
        // especially on Windows where locked regions can block backup/move.
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                Student student = Student.fromCSV(line);
                if (student != null) {
                    students.add(student);
                } else {
                    System.err.println("Skipping malformed student CSV row: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading students: " + e.getMessage());
        }
        return students;
    }

    // Atomic file write with file backup strategy (no OS-level file locks)
    public void saveStudents(List<Student> students) throws IOException {
        File file = new File(studentFile);
        File backupFile = new File(studentFile + ".backup");
        File timestampedBackupFile = new File(studentFile + "." + System.currentTimeMillis() + ".bak");
        File tempFile = new File(studentFile + ".tmp");

        boolean existed = file.exists();

        // Ensure the parent directory exists so we can create/write files safely.
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // Write to temporary file first so we never partially overwrite
        // the main CSV; only a fully written temp file will be moved into place.
        try (PrintWriter pw = new PrintWriter(new FileWriter(tempFile))) {
            pw.println("id,first_name,last_name,program_code,year,gender");
            for (Student student : students) {
                pw.println(student.toCSV());
            }
        }

        // Create backup of existing file (based on state before this save)
        if (existed) {
            Files.copy(file.toPath(), backupFile.toPath(),
                      StandardCopyOption.REPLACE_EXISTING);
            // Also keep a timestamped backup history
            Files.copy(file.toPath(), timestampedBackupFile.toPath(),
                      StandardCopyOption.REPLACE_EXISTING);
        }

        // Atomically replace original with temp file; readers will see either the
        // old full file or the new full file, never a half-written one.
        Files.move(tempFile.toPath(), file.toPath(),
                  StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.ATOMIC_MOVE);
    }

    // Program operations
    public List<Program> loadPrograms() {
        List<Program> programs = new ArrayList<>();
        File file = new File(programFile);
        if (!file.exists()) {
            return programs;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                Program program = Program.fromCSV(line);
                if (program != null) {
                    programs.add(program);
                } else {
                    System.err.println("Skipping malformed program CSV row: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading programs: " + e.getMessage());
        }
        return programs;
    }

    public void savePrograms(List<Program> programs) {
        File file = new File(programFile);
        File backupFile = new File(programFile + ".backup");
        File timestampedBackupFile = new File(programFile + "." + System.currentTimeMillis() + ".bak");
        File tempFile = new File(programFile + ".tmp");
        boolean existed = file.exists();

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(tempFile))) {
            pw.println("code,name,college");
            for (Program program : programs) {
                pw.println(program.toCSV());
            }
        } catch (IOException e) {
            System.err.println("Error saving programs (write temp): " + e.getMessage());
            return;
        }

        try {
            if (existed) {
                Files.copy(file.toPath(), backupFile.toPath(),
                          StandardCopyOption.REPLACE_EXISTING);
                Files.copy(file.toPath(), timestampedBackupFile.toPath(),
                          StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(tempFile.toPath(), file.toPath(),
                      StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Error saving programs (backup/move): " + e.getMessage());
        }
    }

    // College operations
    public List<College> loadColleges() {
        List<College> colleges = new ArrayList<>();
        File file = new File(collegeFile);
        if (!file.exists()) {
            return colleges;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                College college = College.fromCSV(line);
                if (college != null) {
                    colleges.add(college);
                } else {
                    System.err.println("Skipping malformed college CSV row: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading colleges: " + e.getMessage());
        }
        return colleges;
    }

    public void saveColleges(List<College> colleges) {
        File file = new File(collegeFile);
        File backupFile = new File(collegeFile + ".backup");
        File timestampedBackupFile = new File(collegeFile + "." + System.currentTimeMillis() + ".bak");
        File tempFile = new File(collegeFile + ".tmp");
        boolean existed = file.exists();

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(tempFile))) {
            pw.println("code,name");
            for (College college : colleges) {
                pw.println(college.toCSV());
            }
        } catch (IOException e) {
            System.err.println("Error saving colleges (write temp): " + e.getMessage());
            return;
        }

        try {
            if (existed) {
                Files.copy(file.toPath(), backupFile.toPath(),
                          StandardCopyOption.REPLACE_EXISTING);
                Files.copy(file.toPath(), timestampedBackupFile.toPath(),
                          StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(tempFile.toPath(), file.toPath(),
                      StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Error saving colleges (backup/move): " + e.getMessage());
        }
    }
}
