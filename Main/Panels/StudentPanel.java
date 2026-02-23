package Main.Panels;

import Main.Managers.DataManager;
import Main.Models.Student;
import Main.Models.Program;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class StudentPanel extends JPanel {
    private DataManager dataManager;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JTextField idField, firstNameField, lastNameField;
    private JComboBox<String> programCombo, genderCombo, yearCombo;

    public StudentPanel(DataManager dataManager) {
        this.dataManager = dataManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createTable();
        createSearchPanel();
        createFormPanel();
        createButtonPanel();

        refreshTable();
    }

    private void createTable() {
        String[] columns = {"ID", "First Name", "Last Name", "Program", "Year", "Gender"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                loadSelectedStudent();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        
        searchField = new JTextField(30);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        });
        searchPanel.add(searchField);

        add(searchPanel, BorderLayout.NORTH);
    }

    private void createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("ID (YYYY-NNNN):"), gbc);
        gbc.gridx = 1;
        idField = new JTextField(15);
        idField.setDocument(new LengthRestrictedDocument(9));
        formPanel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        firstNameField = new JTextField(15);
        firstNameField.setDocument(new LengthRestrictedDocument(50));
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        lastNameField = new JTextField(15);
        lastNameField.setDocument(new LengthRestrictedDocument(50));
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Program:"), gbc);
        gbc.gridx = 1;
        programCombo = new JComboBox<>();
        updateProgramCombo();
        formPanel.add(programCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        String[] years = {"1", "2", "3", "4", "5", "6"};
        yearCombo = new JComboBox<>(years);
        yearCombo.setEditable(false);
        formPanel.add(yearCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"M", "F", "Other"});
        formPanel.add(genderCombo, gbc);

        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(formPanel, BorderLayout.NORTH);
        add(eastPanel, BorderLayout.EAST);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addStudent());
        buttonPanel.add(addButton);

        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(e -> updateStudent());
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteStudent());
        buttonPanel.add(deleteButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAll());
        buttonPanel.add(refreshButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void updateProgramCombo() {
        programCombo.removeAllItems();
        for (Program program : dataManager.getPrograms()) {
            programCombo.addItem(program.getCode());
        }
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Student> students = dataManager.getStudents();
        for (Student student : students) {
            Object[] row = {
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getProgramCode(),
                student.getYear(),
                student.getGender()
            };
            tableModel.addRow(row);
        }
    }

    private void performSearch() {
        String query = searchField.getText();
        tableModel.setRowCount(0);
        List<Student> results = dataManager.searchStudents(query);
        for (Student student : results) {
            Object[] row = {
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getProgramCode(),
                student.getYear(),
                student.getGender()
            };
            tableModel.addRow(row);
        }
    }

    private void loadSelectedStudent() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            idField.setText((String) tableModel.getValueAt(modelRow, 0));
            firstNameField.setText((String) tableModel.getValueAt(modelRow, 1));
            lastNameField.setText((String) tableModel.getValueAt(modelRow, 2));
            programCombo.setSelectedItem((String) tableModel.getValueAt(modelRow, 3));
            yearCombo.setSelectedItem((String) tableModel.getValueAt(modelRow, 4));
            genderCombo.setSelectedItem((String) tableModel.getValueAt(modelRow, 5));
        }
    }

    private void addStudent() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String programCode = (String) programCombo.getSelectedItem();
        String year = (String) yearCombo.getSelectedItem();
        String gender = (String) genderCombo.getSelectedItem();

        if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Student student = new Student(id, firstName, lastName, programCode, year, gender);
            if (dataManager.addStudent(student)) {
                JOptionPane.showMessageDialog(this, "Student added successfully!");
                clearForm();
                refreshTable();
            } else {
                DataManager.ValidationResult result = dataManager.getLastValidationResult();
                String error = result.toMessage();
                JOptionPane.showMessageDialog(this, 
                    error,
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
                clearInvalidStudentFields(result);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, 
                "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                "Save Failed", JOptionPane.ERROR_MESSAGE);
            clearForm();
        }
    }

    private void updateStudent() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String oldId = (String) tableModel.getValueAt(modelRow, 0);

        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String programCode = (String) programCombo.getSelectedItem();
        String year = (String) yearCombo.getSelectedItem();
        String gender = (String) genderCombo.getSelectedItem();

        if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Student student = new Student(id, firstName, lastName, programCode, year, gender);
            if (dataManager.updateStudent(oldId, student)) {
                JOptionPane.showMessageDialog(this, "Student updated successfully!");
                clearForm();
                refreshTable();
            } else {
                DataManager.ValidationResult result = dataManager.getLastValidationResult();
                String error = result.toMessage();
                JOptionPane.showMessageDialog(this, 
                    error,
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
                clearInvalidStudentFields(result);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, 
                "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                "Save Failed", JOptionPane.ERROR_MESSAGE);
            clearForm();
        }
    }

    private void clearInvalidStudentFields(DataManager.ValidationResult result) {
        if (result == null) {
            return;
        }
        if (result.getFieldErrors().containsKey("id")) {
            idField.setText("");
        }
        if (result.getFieldErrors().containsKey("firstName")) {
            firstNameField.setText("");
        }
        if (result.getFieldErrors().containsKey("lastName")) {
            lastNameField.setText("");
        }
        if (result.getFieldErrors().containsKey("programCode")) {
            if (programCombo.getItemCount() > 0) {
                programCombo.setSelectedIndex(0);
            }
        }
        if (result.getFieldErrors().containsKey("year")) {
            if (yearCombo.getItemCount() > 0) {
                yearCombo.setSelectedIndex(0);
            }
        }
        if (result.getFieldErrors().containsKey("gender")) {
            genderCombo.setSelectedIndex(0);
        }
    }

    private void deleteStudent() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRowPreview = table.convertRowIndexToModel(selectedRow);
        String idPreview = (String) tableModel.getValueAt(modelRowPreview, 0);
        String namePreview = (String) tableModel.getValueAt(modelRowPreview, 1) + " " +
                             (String) tableModel.getValueAt(modelRowPreview, 2);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete student " + idPreview + " (" + namePreview + ")?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            String id = (String) tableModel.getValueAt(modelRow, 0);
            
            try {
                if (dataManager.deleteStudent(id)) {
                    JOptionPane.showMessageDialog(this, "Student deleted successfully!");
                    clearForm();
                    refreshTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete student", "Error", JOptionPane.ERROR_MESSAGE);
                    clearForm();
                }
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, 
                    "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
                clearForm();
            }
        }
    }

    private void clearForm() {
        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        if (programCombo.getItemCount() > 0) {
            programCombo.setSelectedIndex(0);
        }
        if (yearCombo.getItemCount() > 0) {
            yearCombo.setSelectedIndex(0);
        }
        genderCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    private void refreshAll() {
        dataManager.loadData();
        updateProgramCombo();
        refreshTable();
        clearForm();
    }

    // Inner class for restricting text field length
    class LengthRestrictedDocument extends PlainDocument {
        private int maxLength;
        
        public LengthRestrictedDocument(int maxLength) {
            this.maxLength = maxLength;
        }
        
        @Override
        public void insertString(int offset, String str, AttributeSet attr) 
                throws BadLocationException {
            if (str == null) return;
            
            if ((getLength() + str.length()) <= maxLength) {
                super.insertString(offset, str, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
