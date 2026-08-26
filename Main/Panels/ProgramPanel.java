package Main.Panels;

import Main.Managers.DataManager;
import Main.Models.Program;
import Main.Models.College;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ProgramPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private DataManager dataManager;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JTextField codeField, nameField;
    private JComboBox<String> collegeCombo;
    private StudentPanel studentPanel;
    private List<Program> currentResults;
    private int currentPage = 1;
    private int pageSize = 15;
    private JLabel pageInfoLabel;
    private JTextField pageField;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JComboBox<Integer> pageSizeCombo;

    public ProgramPanel(DataManager dataManager) {
        this.dataManager = dataManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createTable();
        createSearchPanel();
        createFormPanel();
        createButtonPanel();

        refreshTable();
    }

    public void setStudentPanel(StudentPanel studentPanel) {
        this.studentPanel = studentPanel;
    }

    private void createTable() {
        String[] columns = {"Code", "Name", "College"};
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
                loadSelectedProgram();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 300));

        JPanel tableContainer = new JPanel(new BorderLayout(5, 5));
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        tableContainer.add(createNavigationPanel(), BorderLayout.SOUTH);

        add(tableContainer, BorderLayout.CENTER);
    }

    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));

        prevPageButton = new JButton("Prev");
        prevPageButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateTablePage();
            }
        });

        nextPageButton = new JButton("Next");
        nextPageButton.addActionListener(e -> {
            if (currentPage < getTotalPages()) {
                currentPage++;
                updateTablePage();
            }
        });

        pageInfoLabel = new JLabel("Page 1 of 1");
        pageField = new JTextField(3);
        JButton goPageButton = new JButton("Go");
        goPageButton.addActionListener(e -> goToPage());
        pageField.addActionListener(e -> goToPage());

        pageSizeCombo = new JComboBox<>(new Integer[]{10, 15, 20, 30, 50});
        pageSizeCombo.setSelectedItem(pageSize);
        pageSizeCombo.addActionListener(e -> {
            pageSize = (Integer) pageSizeCombo.getSelectedItem();
            currentPage = 1;
            updateTablePage();
        });

        navPanel.add(new JLabel("Page Size:"));
        navPanel.add(pageSizeCombo);
        navPanel.add(prevPageButton);
        navPanel.add(pageInfoLabel);
        navPanel.add(nextPageButton);
        navPanel.add(new JLabel("Go to page:"));
        navPanel.add(pageField);
        navPanel.add(goPageButton);

        return navPanel;
    }

    private int getTotalPages() {
        if (currentResults == null || currentResults.isEmpty()) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) currentResults.size() / pageSize));
    }

    private void goToPage() {
        try {
            int requested = Integer.parseInt(pageField.getText().trim());
            if (requested < 1 || requested > getTotalPages()) {
                JOptionPane.showMessageDialog(this, "Page number must be between 1 and " + getTotalPages(), "Invalid Page", JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentPage = requested;
            updateTablePage();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid page number.", "Invalid Page", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void setCurrentResults(List<Program> results) {
        currentResults = results != null ? results : new java.util.ArrayList<>();
        currentPage = 1;
        updateTablePage();
    }

    private void updateTablePage() {
        tableModel.setRowCount(0);
        int totalItems = currentResults.size();
        int totalPages = getTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        int startIndex = Math.max(0, (currentPage - 1) * pageSize);
        int endIndex = Math.min(startIndex + pageSize, totalItems);
        for (int i = startIndex; i < endIndex; i++) {
            Program program = currentResults.get(i);
            String codeDisplay = program.getCode() != null ? program.getCode() : "NULL";
            String collegeDisplay = program.getCollege() != null ? program.getCollege() : "NULL";
            tableModel.addRow(new Object[]{
                codeDisplay,
                program.getName(),
                collegeDisplay
            });
        }
        String rangeText = totalItems == 0 ? "0" : String.valueOf(startIndex + 1);
        pageInfoLabel.setText(String.format("Page %d of %d (%s-%d of %d)", currentPage, totalPages, rangeText, endIndex, totalItems));
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
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
        formPanel.add(new JLabel("Code:"), gbc);
        gbc.gridx = 1;
        codeField = new JTextField(15);
        codeField.setDocument(new LengthRestrictedDocument(20));
        formPanel.add(codeField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setDocument(new LengthRestrictedDocument(100));
        formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("College:"), gbc);
        gbc.gridx = 1;
        collegeCombo = new JComboBox<>();
        updateCollegeCombo();
        formPanel.add(collegeCombo, gbc);

        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(formPanel, BorderLayout.NORTH);
        add(eastPanel, BorderLayout.EAST);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addProgram());
        buttonPanel.add(addButton);

        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(e -> updateProgram());
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteProgram());
        buttonPanel.add(deleteButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAll());
        buttonPanel.add(refreshButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void updateCollegeCombo() {
        collegeCombo.removeAllItems();
        for (College college : dataManager.getColleges()) {
            collegeCombo.addItem(college.getCode());
        }
    }

    public void refreshTable() {
        setCurrentResults(dataManager.getPrograms());
    }

    private void performSearch() {
        String query = searchField.getText();
        List<Program> results = dataManager.searchPrograms(query);
        setCurrentResults(results);
    }

    private void loadSelectedProgram() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            Object codeValue = tableModel.getValueAt(modelRow, 0);
            codeField.setText(codeValue != null && !"NULL".equals(codeValue) ? (String) codeValue : "");
            nameField.setText((String) tableModel.getValueAt(modelRow, 1));
            Object collegeValue = tableModel.getValueAt(modelRow, 2);
            if (collegeValue == null || "NULL".equals(collegeValue)) {
                collegeCombo.setSelectedItem(null);
            } else {
                collegeCombo.setSelectedItem(collegeValue);
            }
        }
    }

    private void addProgram() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String college = (String) collegeCombo.getSelectedItem();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Program program = new Program(code, name, college);
            if (dataManager.addProgram(program)) {
                JOptionPane.showMessageDialog(this, "Program added successfully!");
                clearForm();
                refreshTable();
                if (studentPanel != null) {
                    studentPanel.updateProgramCombo();
                }
            } else {
                DataManager.ValidationResult result = dataManager.getLastValidationResult();
                String error = result.toMessage();
                JOptionPane.showMessageDialog(this, 
                    error,
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
                clearInvalidProgramFields(result);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, 
                "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                "Save Failed", JOptionPane.ERROR_MESSAGE);
            clearForm();
        }
    }

    private void updateProgram() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a program to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Program selectedProgram = getSelectedProgramFromTable(selectedRow);
        String oldCode = selectedProgram != null ? selectedProgram.getCode() : null;

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String college = (String) collegeCombo.getSelectedItem();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Program program = new Program(code, name, college);
            if (dataManager.updateProgram(oldCode, program)) {
                JOptionPane.showMessageDialog(this, "Program updated successfully!");
                clearForm();
                refreshTable();
                if (studentPanel != null) {
                    studentPanel.updateProgramCombo();
                    studentPanel.refreshTable();
                }
            } else {
                DataManager.ValidationResult result = dataManager.getLastValidationResult();
                String error = result.toMessage();
                JOptionPane.showMessageDialog(this, 
                    error,
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
                clearInvalidProgramFields(result);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, 
                "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                "Save Failed", JOptionPane.ERROR_MESSAGE);
            clearForm();
        }
    }

    private void clearInvalidProgramFields(DataManager.ValidationResult result) {
        if (result == null) {
            return;
        }
        if (result.getFieldErrors().containsKey("code")) {
            codeField.setText("");
        }
        if (result.getFieldErrors().containsKey("name")) {
            nameField.setText("");
        }
        if (result.getFieldErrors().containsKey("college")) {
            if (collegeCombo.getItemCount() > 0) {
                collegeCombo.setSelectedIndex(0);
            }
        }
    }

    private void deleteProgram() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a program to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRowPreview = table.convertRowIndexToModel(selectedRow);
        String codePreview = (String) tableModel.getValueAt(modelRowPreview, 0);
        String namePreview = (String) tableModel.getValueAt(modelRowPreview, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete program " + codePreview + " (" + namePreview + ")?\n\n"
            + "Warning: Students currently enrolled in this program will have their program set to NULL.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            Program selectedProgram = getSelectedProgramFromTable(selectedRow);
            String code = selectedProgram != null ? selectedProgram.getCode() : null;
            
            try {
                if (dataManager.deleteProgram(code)) {
                    JOptionPane.showMessageDialog(this, "Program deleted successfully!");
                    clearForm();
                    refreshTable();
                    if (studentPanel != null) {
                        studentPanel.updateProgramCombo();
                        studentPanel.refreshTable();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to delete program.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
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

    private Program getSelectedProgramFromTable(int selectedRow) {
        if (selectedRow == -1 || currentResults == null || currentResults.isEmpty()) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int dataIndex = ((currentPage - 1) * pageSize) + modelRow;
        if (dataIndex < 0 || dataIndex >= currentResults.size()) {
            return null;
        }
        return currentResults.get(dataIndex);
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        if (collegeCombo.getItemCount() > 0) {
            collegeCombo.setSelectedIndex(0);
        }
        table.clearSelection();
    }

    private void refreshAll() {
        dataManager.loadData();
        updateCollegeCombo();
        refreshTable();
        clearForm();
        if (studentPanel != null) {
            studentPanel.updateProgramCombo();
            studentPanel.refreshTable();
        }
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
