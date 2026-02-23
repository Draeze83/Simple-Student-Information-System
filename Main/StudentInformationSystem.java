package Main;

import Main.Panels.StudentPanel;
import Main.Panels.CollegePanel;
import Main.Panels.ProgramPanel;
import Main.Managers.CSVManager;
import Main.Managers.DataManager;
import javax.swing.*;

public class StudentInformationSystem extends JFrame {
    private DataManager dataManager;
    private StudentPanel studentPanel;
    private ProgramPanel programPanel;
    private CollegePanel collegePanel;

    public StudentInformationSystem() {
        setTitle("Student Information System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Initialize data manager with CSV files
        try {
            CSVManager csvManager = new CSVManager(
                "Main/Data/Student.csv",
                "Main/Data/Program.csv",
                "Main/Data/College.csv"
            );
            dataManager = new DataManager(csvManager);
        } catch (SecurityException e) {
            JOptionPane.showMessageDialog(null, 
                "Error initializing CSV files: " + e.getMessage(), 
                "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Create panels
        studentPanel = new StudentPanel(dataManager);
        programPanel = new ProgramPanel(dataManager);
        collegePanel = new CollegePanel(dataManager);

        // Set cross-references for updates
        programPanel.setStudentPanel(studentPanel);
        collegePanel.setProgramPanel(programPanel);

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Students", studentPanel);
        tabbedPane.addTab("Programs", programPanel);
        tabbedPane.addTab("Colleges", collegePanel);

        // Create menu bar
        createMenuBar();

        add(tabbedPane);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        
        JMenuItem exportItem = new JMenuItem("Export All to CSV");
        exportItem.addActionListener(e -> exportData());
        fileMenu.add(exportItem);

        JMenuItem importItem = new JMenuItem("Import from CSV");
        importItem.addActionListener(e -> importData());
        fileMenu.add(importItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void exportData() {
        JOptionPane.showMessageDialog(this, 
            "Data is automatically saved to CSV files:\n" +
            "- Student.csv\n" +
            "- Program.csv\n" +
            "- College.csv", 
            "Export", JOptionPane.INFORMATION_MESSAGE);
    }

    private void importData() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "This will reload data from CSV files.\n" +
            "Any unsaved changes will be lost.\n" +
            "Continue?",
            "Import Data",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dataManager.loadData();
            studentPanel.refreshTable();
            studentPanel.updateProgramCombo();
            programPanel.refreshTable();
            programPanel.updateCollegeCombo();
            collegePanel.refreshTable();
            JOptionPane.showMessageDialog(this, "Data reloaded successfully");
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "Student Information System\n" +
            "Version 1.0\n\n" +
            "Features:\n" +
            "- CRUD operations for Students, Programs, and Colleges\n" +
            "- Search functionality\n" +
            "- Sort by clicking column headers\n" +
            "- CSV file storage\n" +
            "- Data validation and referential integrity",
            "About",
            JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            StudentInformationSystem app = new StudentInformationSystem();
            app.setVisible(true);
        });
    }
}
