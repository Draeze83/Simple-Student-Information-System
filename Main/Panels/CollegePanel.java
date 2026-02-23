package Main.Panels;

import Main.Managers.DataManager;
import Main.Models.College;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class CollegePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private DataManager dataManager;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JTextField codeField, nameField;
    private ProgramPanel programPanel;

    public CollegePanel(DataManager dataManager) {
        this.dataManager = dataManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createTable();
        createSearchPanel();
        createFormPanel();
        createButtonPanel();

        refreshTable();
    }

    public void setProgramPanel(ProgramPanel programPanel) {
        this.programPanel = programPanel;
    }

    private void createTable() {
        String[] columns = {"Code", "Name"};
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
                loadSelectedCollege();
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

        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(formPanel, BorderLayout.NORTH);
        add(eastPanel, BorderLayout.EAST);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> addCollege());
        buttonPanel.add(addButton);

        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(e -> updateCollege());
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteCollege());
        buttonPanel.add(deleteButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAll());
        buttonPanel.add(refreshButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<College> colleges = dataManager.getColleges();
        for (College college : colleges) {
            Object[] row = {
                college.getCode(),
                college.getName()
            };
            tableModel.addRow(row);
        }
    }

    private void performSearch() {
        String query = searchField.getText();
        tableModel.setRowCount(0);
        List<College> results = dataManager.searchColleges(query);
        for (College college : results) {
            Object[] row = {
                college.getCode(),
                college.getName()
            };
            tableModel.addRow(row);
        }
    }

    private void loadSelectedCollege() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            codeField.setText((String) tableModel.getValueAt(modelRow, 0));
            nameField.setText((String) tableModel.getValueAt(modelRow, 1));
        }
    }

    private void addCollege() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            College college = new College(code, name);
            if (dataManager.addCollege(college)) {
                JOptionPane.showMessageDialog(this, "College added successfully!");
                clearForm();
                refreshTable();
                if (programPanel != null) {
                    programPanel.updateCollegeCombo();
                }
            } else {
                DataManager.ValidationResult result = dataManager.getLastValidationResult();
                String error = result.toMessage();
                JOptionPane.showMessageDialog(this, 
                    error,
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
                clearInvalidCollegeFields(result);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, 
                "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                "Save Failed", JOptionPane.ERROR_MESSAGE);
            clearForm();
        }
    }

    private void updateCollege() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a college to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String oldCode = (String) tableModel.getValueAt(modelRow, 0);

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            College college = new College(code, name);
            if (dataManager.updateCollege(oldCode, college)) {
                JOptionPane.showMessageDialog(this, "College updated successfully!");
                clearForm();
                refreshTable();
                if (programPanel != null) {
                    programPanel.updateCollegeCombo();
                    programPanel.refreshTable();
                }
            } else {
                DataManager.ValidationResult result = dataManager.getLastValidationResult();
                String error = result.toMessage();
                JOptionPane.showMessageDialog(this, 
                    error,
                    "Validation Failed", JOptionPane.ERROR_MESSAGE);
                clearInvalidCollegeFields(result);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, 
                "CRITICAL ERROR: Could not save to file!\n" + e.getMessage(), 
                "Save Failed", JOptionPane.ERROR_MESSAGE);
            clearForm();
        }
    }

    private void deleteCollege() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a college to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRowPreview = table.convertRowIndexToModel(selectedRow);
        String codePreview = (String) tableModel.getValueAt(modelRowPreview, 0);
        String namePreview = (String) tableModel.getValueAt(modelRowPreview, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete college " + codePreview + " (" + namePreview + ")?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            String code = (String) tableModel.getValueAt(modelRow, 0);
            
            try {
                if (dataManager.deleteCollege(code)) {
                    JOptionPane.showMessageDialog(this, "College deleted successfully!");
                    clearForm();
                    refreshTable();
                    if (programPanel != null) {
                        programPanel.updateCollegeCombo();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete college. Programs are associated with this college.", 
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

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        table.clearSelection();
    }

    private void clearInvalidCollegeFields(DataManager.ValidationResult result) {
        if (result == null) {
            return;
        }
        if (result.getFieldErrors().containsKey("code")) {
            codeField.setText("");
        }
        if (result.getFieldErrors().containsKey("name")) {
            nameField.setText("");
        }
    }

    private void refreshAll() {
        dataManager.loadData();
        refreshTable();
        clearForm();
        if (programPanel != null) {
            programPanel.updateCollegeCombo();
            programPanel.refreshTable();
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
