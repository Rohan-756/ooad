package com.hrms.ui;

import com.hrms.controller.AttritionController;
import com.hrms.controller.DashboardController;
import com.hrms.controller.EmployeeController;
import com.hrms.controller.RiskController;
import com.hrms.exception.HRMSException;
import com.hrms.model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * MainDashboard — Primary Swing UI for Employee Data Management & Exit Management.
 *
 * Features:
 *  - Modern dark theme with glassmorphism-inspired cards
 *  - Employee Form (Add / View / Update / Delete)
 *  - Exit Interview Form
 *  - Employee Table View with live filtering
 *  - KPI summary cards
 */
public class MainDashboard extends JFrame {

    // ── Color Palette ────────────────────────────────────────────────────────
    private static final Color BG_DARK        = new Color(13, 17, 30);
    private static final Color BG_CARD        = new Color(22, 27, 44);
    private static final Color BG_CARD_HOVER  = new Color(30, 36, 58);
    private static final Color ACCENT_BLUE    = new Color(99, 179, 237);
    private static final Color ACCENT_PURPLE  = new Color(159, 122, 234);
    private static final Color ACCENT_GREEN   = new Color(72, 199, 142);
    private static final Color ACCENT_ORANGE  = new Color(246, 173, 85);
    private static final Color ACCENT_RED     = new Color(252, 129, 116);
    private static final Color TEXT_PRIMARY   = new Color(237, 242, 247);
    private static final Color TEXT_SECONDARY = new Color(160, 174, 192);
    private static final Color BORDER_COLOR   = new Color(45, 55, 72);
    private static final Color INPUT_BG       = new Color(17, 24, 39);
    private static final Color TABLE_ALT      = new Color(17, 22, 36);

    // ── Font ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_KPI     = new Font("Segoe UI", Font.BOLD, 32);

    // ── Controllers ──────────────────────────────────────────────────────────
    private final EmployeeController   controller;
    private final AttritionController  attritionController;
    private final RiskController       riskController;
    private final DashboardController  dashboardController;

    // ── Table Model ──────────────────────────────────────────────────────────
    private DefaultTableModel employeeTableModel;
    private DefaultTableModel exitTableModel;
    private JTable employeeTable;
    private JTable exitTable;

    // ── KPI Labels ───────────────────────────────────────────────────────────
    private JLabel totalLabel, activeLabel, exitedLabel;

    // ── Search ───────────────────────────────────────────────────────────────
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> rowSorter;

    public MainDashboard() {
        this.controller           = new EmployeeController();
        this.attritionController  = new AttritionController();
        this.riskController       = new RiskController();
        this.dashboardController  = new DashboardController();
        // Observer: register UI refresh callback so dashboard auto-updates
        this.dashboardController.setRefreshCallback(this::refreshAll);
        setupFrame();
        buildUI();
        refreshAll();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FRAME SETUP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFrame() {
        setTitle("HRMS — Employee & Exit Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // Shutdown hook to close DB connection gracefully
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                com.hrms.db.DBConnection.getInstance().closeConnection();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildTopBar(), BorderLayout.NORTH);

        JTabbedPane tabs = buildTabbedPane();
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BG_DARK);
        centerWrapper.setBorder(new EmptyBorder(0, 16, 16, 16));
        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_CARD);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(16, 24, 16, 24)
        ));

        // Left: Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        JLabel icon = new JLabel("⚙");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        icon.setForeground(ACCENT_BLUE);
        JLabel title = new JLabel("HRMS Attrition Analysis");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        JLabel subtitle = new JLabel("  |  Employee & Exit Management");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(TEXT_SECONDARY);
        titlePanel.add(icon);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        // Right: KPI Cards
        JPanel kpiPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        kpiPanel.setOpaque(false);
        kpiPanel.add(buildMiniKpi("Total", "0", ACCENT_BLUE, true));
        kpiPanel.add(buildMiniKpi("Active", "0", ACCENT_GREEN, false));
        kpiPanel.add(buildMiniKpi("Exited", "0", ACCENT_RED, false));

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(kpiPanel, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildMiniKpi(String label, String value, Color accent, boolean isFirst) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(INPUT_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                new EmptyBorder(6, 16, 6, 16)
        ));
        card.setCursor(Cursor.getDefaultCursor());

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valLabel.setForeground(accent);
        valLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valLabel);
        card.add(lbl);

        // Store reference
        if (label.equals("Total")) totalLabel = valLabel;
        else if (label.equals("Active")) activeLabel = valLabel;
        else if (label.equals("Exited")) exitedLabel = valLabel;

        return card;
    }

    // ── Tabbed Pane ───────────────────────────────────────────────────────────

    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG_DARK);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setFont(FONT_BODY);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        UIManager.put("TabbedPane.selected", BG_CARD);
        UIManager.put("TabbedPane.background", BG_DARK);

        tabs.addTab("  📋  Employee Table  ", buildEmployeeTableTab());
        tabs.addTab("  ➕  Add Employee  ", buildAddEmployeeTab());
        tabs.addTab("  ✏️  Edit Employee  ", buildEditEmployeeTab());
        tabs.addTab("  🚪  Exit Interview  ", buildExitInterviewTab());
        tabs.addTab("  📈  Attrition Analysis  ", buildAttritionTab());
        tabs.addTab("  ⚠️  Risk Evaluation  ", buildRiskTab());
        tabs.addTab("  📊  Analytics Dashboard  ", buildDashboardTab());

        // Style tabs
        for (int i = 0; i < tabs.getTabCount(); i++) {
            JLabel lbl = new JLabel(tabs.getTitleAt(i));
            lbl.setFont(FONT_BODY);
            lbl.setForeground(TEXT_PRIMARY);
            lbl.setBorder(new EmptyBorder(4, 4, 4, 4));
        }

        return tabs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 1: Employee Table
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildEmployeeTableTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        // ── Search bar ──
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(BG_DARK);
        searchBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        searchField = styledTextField("🔍  Search by name, department, or status...");
        JButton refreshBtn = styledButton("↺  Refresh", ACCENT_BLUE);
        refreshBtn.addActionListener(e -> refreshAll());

        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(refreshBtn, BorderLayout.EAST);

        // ── Employee Table ──
        String[] empCols = {"ID", "Name", "Department", "Attendance %",
                            "Years of Service", "Promotions", "Status"};
        employeeTableModel = new DefaultTableModel(empCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        employeeTable = styledTable(employeeTableModel);
        rowSorter = new TableRowSorter<>(employeeTableModel);
        employeeTable.setRowSorter(rowSorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) { rowSorter.setRowFilter(null); return; }
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        JScrollPane scroll = styledScrollPane(employeeTable);

        // ── Exit Interviews Table ──
        JLabel exitTitle = sectionLabel("Exit Interviews");
        String[] exitCols = {"Interview ID", "Employee ID", "Exit Reason", "Feedback", "Date"};
        exitTableModel = new DefaultTableModel(exitCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        exitTable = styledTable(exitTableModel);
        JScrollPane exitScroll = styledScrollPane(exitTable);
        exitScroll.setPreferredSize(new Dimension(0, 180));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, exitScroll);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(6);
        splitPane.setBackground(BG_DARK);
        splitPane.setBorder(null);

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 2: Add Employee
    // ─────────────────────────────────────────────────────────────────────────

    private JScrollPane buildAddEmployeeTab() {
        JPanel card = formCard("Add New Employee", ACCENT_GREEN);

        JTextField nameField = styledTextField("e.g. Jane Doe");
        JComboBox<String> deptCombo = styledCombo(new String[]{
            "Engineering", "Sales", "Marketing", "HR", "Finance",
            "Operations", "Legal", "Product", "Design", "Support"
        });
        JTextField attendanceField = styledTextField("0–100");
        JTextField yearsField = styledTextField("e.g. 3");
        JTextField promotionField = styledTextField("e.g. 1");
        JComboBox<Employee.EmploymentStatus> statusCombo =
                new JComboBox<>(Employee.EmploymentStatus.values());
        styleComboBox(statusCombo);

        JButton addBtn = styledButton("➕  Add Employee", ACCENT_GREEN);

        addBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String dept = (String) deptCombo.getSelectedItem();
                double att = Double.parseDouble(attendanceField.getText().trim());
                int years = Integer.parseInt(yearsField.getText().trim());
                int promo = Integer.parseInt(promotionField.getText().trim());
                Employee.EmploymentStatus status =
                        (Employee.EmploymentStatus) statusCombo.getSelectedItem();

                int id = controller.handleAddEmployee(name, dept, att, years, promo, status);
                showSuccess(card, "✅ Employee added successfully! ID assigned: " + id);
                clearFields(nameField, attendanceField, yearsField, promotionField);
                refreshAll();

            } catch (NumberFormatException ex) {
                showError(card, "❌ INVALID_INPUT: Please enter valid numbers for attendance, years, and promotions.");
            } catch (HRMSException ex) {
                showError(card, "❌ " + ex.getErrorCode() + ": " + ex.getMessage());
            }
        });

        card.add(formRow("Full Name:", nameField));
        card.add(Box.createVerticalStrut(10));
        card.add(formRow("Department:", deptCombo));
        card.add(Box.createVerticalStrut(10));
        card.add(formRow("Attendance % :", attendanceField));
        card.add(Box.createVerticalStrut(10));
        card.add(formRow("Years of Service:", yearsField));
        card.add(Box.createVerticalStrut(10));
        card.add(formRow("Promotion Count:", promotionField));
        card.add(Box.createVerticalStrut(10));
        card.add(formRow("Status:", statusCombo));
        card.add(Box.createVerticalStrut(20));
        card.add(addBtn);

        return centeredFormPanel(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 3: Edit / View / Delete Employee
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildEditEmployeeTab() {
        JPanel outer = new JPanel(new BorderLayout(16, 0));
        outer.setBackground(BG_DARK);
        outer.setBorder(new EmptyBorder(16, 0, 0, 0));

        // Left: lookup panel
        JPanel lookupCard = formCard("Lookup Employee", ACCENT_BLUE);
        JTextField idField = styledTextField("Enter Employee ID");
        JButton lookupBtn = styledButton("🔍  Lookup", ACCENT_BLUE);

        // Right: edit form
        JPanel editCard = formCard("Edit / Delete Employee", ACCENT_PURPLE);
        JLabel idDisplay = new JLabel("—");
        idDisplay.setForeground(ACCENT_BLUE);
        idDisplay.setFont(FONT_HEADING);

        JTextField nameField = styledTextField("");
        JComboBox<String> deptCombo = styledCombo(new String[]{
            "Engineering", "Sales", "Marketing", "HR", "Finance",
            "Operations", "Legal", "Product", "Design", "Support"
        });
        JTextField attendanceField = styledTextField("");
        JTextField yearsField = styledTextField("");
        JTextField promotionField = styledTextField("");
        JComboBox<Employee.EmploymentStatus> statusCombo =
                new JComboBox<>(Employee.EmploymentStatus.values());
        styleComboBox(statusCombo);

        JButton updateBtn = styledButton("💾  Save Changes", ACCENT_PURPLE);
        JButton deleteBtn = styledButton("🗑  Delete Employee", ACCENT_RED);

        // Lookup action
        lookupBtn.addActionListener(e -> {
            try {
                int empId = Integer.parseInt(idField.getText().trim());
                Employee emp = controller.handleGetEmployee(empId);
                idDisplay.setText("ID: " + emp.getEmployeeId());
                nameField.setText(emp.getName());
                deptCombo.setSelectedItem(emp.getDepartment());
                attendanceField.setText(String.valueOf(emp.getAttendanceRate()));
                yearsField.setText(String.valueOf(emp.getYearsOfService()));
                promotionField.setText(String.valueOf(emp.getMonthsSincePromotion()));
                statusCombo.setSelectedItem(emp.getEmploymentStatus());
                showSuccess(editCard, "✅ Employee found.");
            } catch (NumberFormatException ex) {
                showError(lookupCard, "❌ INVALID_INPUT: Please enter a valid numeric Employee ID.");
            } catch (HRMSException ex) {
                showError(lookupCard, "❌ " + ex.getErrorCode() + ": " + ex.getMessage());
            }
        });

        // Update action
        updateBtn.addActionListener(e -> {
            try {
                String idText = idDisplay.getText().replace("ID: ", "").trim();
                if (idText.equals("—")) { showError(editCard, "❌ No employee loaded. Use Lookup first."); return; }
                int empId = Integer.parseInt(idText);
                double att = Double.parseDouble(attendanceField.getText().trim());
                int years = Integer.parseInt(yearsField.getText().trim());
                int promo = Integer.parseInt(promotionField.getText().trim());
                controller.handleUpdateEmployee(empId, nameField.getText().trim(),
                        (String) deptCombo.getSelectedItem(), att, years, promo,
                        (Employee.EmploymentStatus) statusCombo.getSelectedItem());
                showSuccess(editCard, "✅ Employee updated successfully.");
                refreshAll();
            } catch (NumberFormatException ex) {
                showError(editCard, "❌ INVALID_INPUT: Ensure attendance, years, and promotions are valid numbers.");
            } catch (HRMSException ex) {
                showError(editCard, "❌ " + ex.getErrorCode() + ": " + ex.getMessage());
            }
        });

        // Delete action
        deleteBtn.addActionListener(e -> {
            String idText = idDisplay.getText().replace("ID: ", "").trim();
            if (idText.equals("—")) { showError(editCard, "❌ No employee loaded. Use Lookup first."); return; }
            int empId = Integer.parseInt(idText);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to permanently delete Employee ID " + empId + "?\nThis cannot be undone.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    controller.handleDeleteEmployee(empId);
                    showSuccess(editCard, "✅ Employee deleted.");
                    idDisplay.setText("—");
                    clearFields(nameField, attendanceField, yearsField, promotionField);
                    refreshAll();
                } catch (HRMSException ex) {
                    showError(editCard, "❌ " + ex.getErrorCode() + ": " + ex.getMessage());
                }
            }
        });

        lookupCard.add(formRow("Employee ID:", idField));
        lookupCard.add(Box.createVerticalStrut(16));
        lookupCard.add(lookupBtn);
        lookupCard.add(Box.createVerticalGlue());

        editCard.add(formRow("Employee:", idDisplay));
        editCard.add(Box.createVerticalStrut(10));
        editCard.add(formRow("Full Name:", nameField));
        editCard.add(Box.createVerticalStrut(10));
        editCard.add(formRow("Department:", deptCombo));
        editCard.add(Box.createVerticalStrut(10));
        editCard.add(formRow("Attendance %:", attendanceField));
        editCard.add(Box.createVerticalStrut(10));
        editCard.add(formRow("Years of Service:", yearsField));
        editCard.add(Box.createVerticalStrut(10));
        editCard.add(formRow("Promotion Count:", promotionField));
        editCard.add(Box.createVerticalStrut(10));
        editCard.add(formRow("Status:", statusCombo));
        editCard.add(Box.createVerticalStrut(20));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        editCard.add(btnRow);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(BG_DARK);
        left.add(lookupCard, BorderLayout.NORTH);

        outer.add(left, BorderLayout.WEST);
        outer.add(editCard, BorderLayout.CENTER);
        return outer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 4: Exit Interview
    // ─────────────────────────────────────────────────────────────────────────

    private JScrollPane buildExitInterviewTab() {
        JPanel card = formCard("Record Exit Interview", ACCENT_ORANGE);

        JTextField empIdField = styledTextField("Employee ID");
        JLabel empNameDisplay = new JLabel("— look up employee first —");
        empNameDisplay.setForeground(TEXT_SECONDARY);
        empNameDisplay.setFont(FONT_BODY);

        JButton lookupBtn = styledButton("🔍  Verify Employee", ACCENT_BLUE);

        String[] reasons = {
            "Better Opportunity", "Relocation", "Personal Reasons",
            "Work-Life Balance", "Compensation", "Management Issues",
            "Career Growth", "Health Reasons", "Retirement", "Other"
        };
        JComboBox<String> reasonCombo = styledCombo(reasons);

        JTextArea feedbackArea = new JTextArea(4, 30);
        feedbackArea.setBackground(INPUT_BG);
        feedbackArea.setForeground(TEXT_PRIMARY);
        feedbackArea.setCaretColor(TEXT_PRIMARY);
        feedbackArea.setFont(FONT_BODY);
        feedbackArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        JScrollPane feedbackScroll = styledScrollPane(feedbackArea);
        feedbackScroll.setPreferredSize(new Dimension(0, 100));

        JTextField dateField = styledTextField(LocalDate.now().toString());

        JButton submitBtn = styledButton("✅  Submit Exit Interview", ACCENT_ORANGE);

        lookupBtn.addActionListener(e -> {
            try {
                int empId = Integer.parseInt(empIdField.getText().trim());
                Employee emp = controller.handleGetEmployee(empId);
                empNameDisplay.setText(emp.getName() + " — " + emp.getDepartment()
                        + " (" + emp.getEmploymentStatus() + ")");
                empNameDisplay.setForeground(ACCENT_GREEN);
            } catch (NumberFormatException ex) {
                showError(card, "❌ INVALID_INPUT: Enter a valid numeric Employee ID.");
            } catch (HRMSException ex) {
                showError(card, "❌ " + ex.getErrorCode() + ": " + ex.getMessage());
                empNameDisplay.setText("— employee not found —");
                empNameDisplay.setForeground(ACCENT_RED);
            }
        });

        submitBtn.addActionListener(e -> {
            try {
                int empId = Integer.parseInt(empIdField.getText().trim());
                String reason = (String) reasonCombo.getSelectedItem();
                String feedback = feedbackArea.getText().trim();
                String date = dateField.getText().trim();
                if (date.isEmpty()) date = LocalDate.now().toString();

                int id = controller.handleSaveExitInterview(empId, reason, feedback, date);
                showSuccess(card, "✅ Exit interview recorded. Interview ID: " + id
                        + ". Employee marked as EXITED.");
                empIdField.setText("");
                feedbackArea.setText("");
                empNameDisplay.setText("— look up employee first —");
                empNameDisplay.setForeground(TEXT_SECONDARY);
                refreshAll();

            } catch (HRMSException.MissingFeedbackException ex) {
                // WARNING — Ask user if they want to proceed without feedback
                int choice = JOptionPane.showConfirmDialog(this,
                        "⚠ WARNING: " + ex.getMessage() + "\n\nSubmit without feedback?",
                        "Missing Feedback Warning",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        int empId = Integer.parseInt(empIdField.getText().trim());
                        String reason = (String) reasonCombo.getSelectedItem();
                        String date = dateField.getText().trim();
                        if (date.isEmpty()) date = LocalDate.now().toString();
                        int id = controller.handleSaveExitInterviewNoFeedback(empId, reason, date);
                        showSuccess(card, "✅ Exit interview recorded (no feedback). ID: " + id);
                        empIdField.setText("");
                        feedbackArea.setText("");
                        empNameDisplay.setText("— look up employee first —");
                        empNameDisplay.setForeground(TEXT_SECONDARY);
                        refreshAll();
                    } catch (HRMSException ex2) {
                        showError(card, "❌ " + ex2.getErrorCode() + ": " + ex2.getMessage());
                    }
                }
            } catch (NumberFormatException ex) {
                showError(card, "❌ INVALID_INPUT: Enter a valid numeric Employee ID.");
            } catch (HRMSException ex) {
                showError(card, "❌ " + ex.getErrorCode() + ": " + ex.getMessage());
            }
        });

        // Build form
        JPanel idRow = new JPanel(new BorderLayout(8, 0));
        idRow.setOpaque(false);
        idRow.add(empIdField, BorderLayout.CENTER);
        idRow.add(lookupBtn, BorderLayout.EAST);

        card.add(formRow("Employee ID:", idRow));
        card.add(Box.createVerticalStrut(6));
        card.add(empNameDisplay);
        card.add(Box.createVerticalStrut(12));
        card.add(formRow("Exit Reason:", reasonCombo));
        card.add(Box.createVerticalStrut(10));

        JLabel fbLabel = new JLabel("Feedback / Comments:");
        fbLabel.setForeground(TEXT_SECONDARY);
        fbLabel.setFont(FONT_BODY);
        card.add(fbLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(feedbackScroll);
        card.add(Box.createVerticalStrut(10));
        card.add(formRow("Interview Date:", dateField));
        card.add(Box.createVerticalStrut(20));
        card.add(submitBtn);

        return centeredFormPanel(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA REFRESH
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            // Refresh employee table
            employeeTableModel.setRowCount(0);
            List<Employee> employees = controller.handleGetAllEmployees();
            for (Employee emp : employees) {
                employeeTableModel.addRow(new Object[]{
                    emp.getEmployeeId(), emp.getName(), emp.getDepartment(),
                    String.format("%.1f%%", emp.getAttendanceRate()),
                    emp.getYearsOfService(), emp.getMonthsSincePromotion(),
                    emp.getEmploymentStatus().name()
                });
            }

            // Refresh exit interviews table
            exitTableModel.setRowCount(0);
            List<ExitInterview> interviews = controller.handleGetAllExitInterviews();
            for (ExitInterview ei : interviews) {
                String fb = ei.getFeedbackText();
                if (fb != null && fb.length() > 40) fb = fb.substring(0, 40) + "...";
                exitTableModel.addRow(new Object[]{
                    ei.getInterviewId(), ei.getEmployeeId(),
                    ei.getPrimaryReason(), fb != null ? fb : "(none)", ei.getExitDate()
                });
            }

            // Update KPI
            int total = controller.getTotalCount();
            int active = controller.getActiveCount();
            int exited = controller.getExitedCount();
            totalLabel.setText(String.valueOf(total));
            activeLabel.setText(String.valueOf(active));
            exitedLabel.setText(String.valueOf(exited));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STYLE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private JScrollPane centeredFormPanel(JPanel card) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(new EmptyBorder(20, 60, 20, 60));
        wrapper.add(card, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = ACCENT_BLUE.darker();
                this.trackColor = INPUT_BG;
            }
        });
        return scroll;
    }

    private JPanel formCard(String title, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                new EmptyBorder(24, 28, 28, 28)
        ));
        // No fixed preferred size — let BoxLayout size from content
        card.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADING);
        titleLabel.setForeground(accent);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(0, 0, 12, 0)
        ));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(16));

        return card;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField field = new JTextField(20) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(100, 116, 139));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 10, getHeight() / 2 + getFont().getSize() / 3);
                    g2.dispose();
                }
            }
        };
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_BLUE);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return field;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        styleComboBox(combo);
        return combo;
    }

    private <T> void styleComboBox(JComboBox<T> combo) {
        combo.setBackground(INPUT_BG);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        ((JLabel) combo.getRenderer()).setBackground(INPUT_BG);
    }

    private JButton styledButton(String text, Color accent) {
        // We desaturate and darken the color to make it "blander"
        Color blandColor = new Color(60, 70, 90); 
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (getModel().isPressed()) {
                    g2.setColor(blandColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(blandColor.brighter());
                } else {
                    g2.setColor(blandColor);
                }
                // Simpler: Square edges instead of rounded
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        // Bigger: Larger font and dimensions
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(blandColor);
        btn.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(280, 50));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(ACCENT_BLUE.darker());
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? BG_CARD : TABLE_ALT);
                    c.setForeground(TEXT_PRIMARY);

                    // Highlight EXITED rows in the employee table
                    if (model.getColumnCount() == 7 && col == 6) {
                        String val = model.getValueAt(row, col) != null
                                ? model.getValueAt(row, col).toString() : "";
                        c.setForeground(val.equals("EXITED") ? ACCENT_RED : ACCENT_GREEN);
                    }
                }
                return c;
            }
        };
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(BORDER_COLOR);
        table.setShowGrid(true);
        table.setSelectionBackground(ACCENT_BLUE.darker());
        table.setSelectionForeground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(INPUT_BG);
        header.setForeground(ACCENT_BLUE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE));
        header.setPreferredSize(new Dimension(0, 38));

        return table;
    }

    private JScrollPane styledScrollPane(Component comp) {
        JScrollPane pane = new JScrollPane(comp);
        pane.setBackground(BG_DARK);
        pane.getViewport().setBackground(BG_CARD);
        pane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        pane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = ACCENT_BLUE.darker();
                this.trackColor = INPUT_BG;
            }
        });
        return pane;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setForeground(ACCENT_ORANGE);
        lbl.setBorder(new EmptyBorder(8, 0, 4, 0));
        return lbl;
    }

    private JPanel formRow(String label, Component field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setFont(FONT_BODY);
        lbl.setPreferredSize(new Dimension(140, 36));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void showError(JPanel card, String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(JPanel card, String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearFields(JTextField... fields) {
        for (JTextField f : fields) f.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 5: Attrition Analysis
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildAttritionTab() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(16, 0, 0, 0));

        // ── Top section: date range form + rate result ───────────────────────
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setBackground(BG_DARK);

        // --- Form card ---
        JPanel formCard = attrCard("Date Range & Period", ACCENT_BLUE);

        JTextField startField   = styledTextField("YYYY-MM-DD  e.g. 2024-01-01");
        JTextField endField     = styledTextField("YYYY-MM-DD  e.g. 2024-12-31");
        JComboBox<String> periodCombo = styledCombo(new String[]{"Monthly", "Quarterly", "Annual"});

        JButton calcBtn  = styledButton("⚡  Calculate Rate",  ACCENT_BLUE);
        JButton trendBtn = styledButton("📊  Generate Trend",  ACCENT_PURPLE);

        calcBtn.setPreferredSize(new Dimension(140, 20));
        trendBtn.setPreferredSize(new Dimension(140, 20));

        formCard.add(formRow("Start Date:",   startField));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(formRow("End Date:",     endField));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(formRow("Period Type:",  periodCombo));
        formCard.add(Box.createVerticalStrut(20));
        JPanel btnRow0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow0.setOpaque(false);
        btnRow0.add(calcBtn);
        btnRow0.add(trendBtn);
        formCard.add(btnRow0);

        // --- Result card ---
        JPanel resultCard = attrCard("Attrition Rate Result", ACCENT_GREEN);
        JLabel rateValueLabel = new JLabel("—");
        rateValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 42));
        rateValueLabel.setForeground(ACCENT_GREEN);
        rateValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rateDetailLabel = new JLabel("Select a date range and click \"Calculate Rate\"");
        rateDetailLabel.setFont(FONT_BODY);
        rateDetailLabel.setForeground(TEXT_SECONDARY);
        rateDetailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultCard.add(rateValueLabel);
        resultCard.add(Box.createVerticalStrut(8));
        resultCard.add(rateDetailLabel);
        resultCard.add(Box.createVerticalGlue());

        topRow.add(formCard,   BorderLayout.WEST);
        topRow.add(resultCard, BorderLayout.CENTER);

        // ── Trend table ─────────────────────────────────────────────────────
        JPanel trendSection = new JPanel(new BorderLayout(0, 6));
        trendSection.setBackground(BG_DARK);
        JLabel trendTitle = sectionLabel("Trend Data");
        trendTitle.setForeground(ACCENT_PURPLE);

        String[] trendCols = {"Period Start", "Period End", "Total Employees", "Exits", "Attrition Rate %"};
        DefaultTableModel trendModel = new DefaultTableModel(trendCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable trendTable = styledTable(trendModel);
        JScrollPane trendScroll = styledScrollPane(trendTable);
        trendScroll.setPreferredSize(new Dimension(0, 160));

        trendSection.add(trendTitle,  BorderLayout.NORTH);
        trendSection.add(trendScroll, BorderLayout.CENTER);

        // ── Chart panel ─────────────────────────────────────────────────────
        JPanel chartSection = new JPanel(new BorderLayout(0, 6));
        chartSection.setBackground(BG_DARK);
        JLabel chartTitle = sectionLabel("Attrition Trend Chart");
        chartTitle.setForeground(ACCENT_ORANGE);

        AttritionChartPanel chartPanel = new AttritionChartPanel();
        chartPanel.setPreferredSize(new Dimension(0, 220));
        chartPanel.setMinimumSize(new Dimension(0, 200));

        chartSection.add(chartTitle, BorderLayout.NORTH);
        chartSection.add(chartPanel, BorderLayout.CENTER);

        // ── Wire button actions ──────────────────────────────────────────────
        calcBtn.addActionListener(e -> {
            try {
                LocalDate start = LocalDate.parse(startField.getText().trim());
                LocalDate end   = LocalDate.parse(endField.getText().trim());
                String period   = (String) periodCombo.getSelectedItem();

                AttritionRecord record = attritionController.calculate(period, start, end);
                rateValueLabel.setText(String.format("%.2f%%", record.getAttritionRate()));
                rateValueLabel.setForeground(
                    record.getAttritionRate() >= 15 ? ACCENT_RED :
                    record.getAttritionRate() >= 8  ? ACCENT_ORANGE : ACCENT_GREEN);
                rateDetailLabel.setText(
                    record.getTotalEmployees() + " employees | " +
                    record.getEmployeesLeft()  + " exits | " +
                    start + " → " + end);

            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ INVALID_DATE_RANGE: Use YYYY-MM-DD format for dates.",
                    "Invalid Date", JOptionPane.ERROR_MESSAGE);
            } catch (HRMSException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ " + ex.getErrorCode() + ": " + ex.getMessage(),
                    ex.getErrorCode().toString(), JOptionPane.ERROR_MESSAGE);
            }
        });

        trendBtn.addActionListener(e -> {
            try {
                LocalDate start = LocalDate.parse(startField.getText().trim());
                LocalDate end   = LocalDate.parse(endField.getText().trim());
                String period   = (String) periodCombo.getSelectedItem();

                java.util.List<AttritionRecord> records =
                    attritionController.trend(period, start, end);

                // Populate table
                trendModel.setRowCount(0);
                for (AttritionRecord r : records) {
                    trendModel.addRow(new Object[]{
                        r.getStartDate(), r.getEndDate(),
                        r.getTotalEmployees(), r.getEmployeesLeft(),
                        String.format("%.2f%%", r.getAttritionRate())
                    });
                }

                // Refresh chart
                chartPanel.setData(records);

                if (records.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "No data found for the selected period.",
                        "No Data", JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ INVALID_DATE_RANGE: Use YYYY-MM-DD format for dates.",
                    "Invalid Date", JOptionPane.ERROR_MESSAGE);
            } catch (HRMSException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ " + ex.getErrorCode() + ": " + ex.getMessage(),
                    ex.getErrorCode().toString(), JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── Bottom split: table + chart ──────────────────────────────────────
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, trendSection, chartSection);
        bottomSplit.setResizeWeight(0.38);
        bottomSplit.setDividerSize(6);
        bottomSplit.setBackground(BG_DARK);
        bottomSplit.setBorder(null);

        root.add(topRow,      BorderLayout.NORTH);
        root.add(bottomSplit, BorderLayout.CENTER);
        return root;
    }

    /** Simple helper to build a card panel for the Attrition tab. */
    private JPanel attrCard(String title, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                new EmptyBorder(18, 22, 22, 22)
        ));
        card.setPreferredSize(new Dimension(370, 240));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADING);
        titleLabel.setForeground(accent);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(0, 0, 10, 0)
        ));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(14));
        return card;
    }

    /**
     * Java2D line chart rendering attrition rate % per trend bucket.
     * No external library required.
     */
    private class AttritionChartPanel extends JPanel {
        private java.util.List<AttritionRecord> data = new java.util.ArrayList<>();

        public AttritionChartPanel() {
            setBackground(BG_CARD);
            setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        }

        public void setData(java.util.List<AttritionRecord> records) {
            this.data = records;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padLeft = 60, padRight = 20, padTop = 20, padBottom = 40;
            int chartW = w - padLeft - padRight;
            int chartH = h - padTop - padBottom;

            // Background grid
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{4, 4}, 0));
            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                int y = padTop + (chartH * i / gridLines);
                g2.drawLine(padLeft, y, padLeft + chartW, y);
                double pct = 25.0 - (25.0 * i / gridLines);
                g2.setColor(TEXT_SECONDARY);
                g2.setFont(FONT_SMALL);
                g2.drawString(String.format("%.0f%%", pct), 4, y + 4);
                g2.setColor(BORDER_COLOR);
            }

            if (data == null || data.isEmpty()) {
                g2.setColor(TEXT_SECONDARY);
                g2.setFont(FONT_BODY);
                String msg = "Generate trend data to see the chart";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            // Find max rate for scaling (min 25%)
            double maxRate = 25.0;
            for (AttritionRecord r : data) maxRate = Math.max(maxRate, r.getAttritionRate());
            maxRate = Math.ceil(maxRate / 5.0) * 5.0; // round up to nearest 5

            // Plot points
            int n = data.size();
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = padLeft + (n == 1 ? chartW / 2 : chartW * i / (n - 1));
                ys[i] = padTop  + chartH - (int)(chartH * data.get(i).getAttritionRate() / maxRate);
            }

            // Area fill under line
            g2.setColor(new Color(ACCENT_PURPLE.getRed(), ACCENT_PURPLE.getGreen(),
                    ACCENT_PURPLE.getBlue(), 40));
            int[] fillX = new int[n + 2];
            int[] fillY = new int[n + 2];
            fillX[0] = xs[0]; fillY[0] = padTop + chartH;
            System.arraycopy(xs, 0, fillX, 1, n);
            System.arraycopy(ys, 0, fillY, 1, n);
            fillX[n + 1] = xs[n - 1]; fillY[n + 1] = padTop + chartH;
            g2.fillPolygon(fillX, fillY, n + 2);

            // Line
            g2.setColor(ACCENT_PURPLE);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);

            // Dots + labels
            g2.setFont(FONT_SMALL);
            for (int i = 0; i < n; i++) {
                double rate = data.get(i).getAttritionRate();
                Color dot = rate >= 15 ? ACCENT_RED : rate >= 8 ? ACCENT_ORANGE : ACCENT_GREEN;
                g2.setColor(dot);
                g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
                g2.setColor(TEXT_PRIMARY);
                String lbl = String.format("%.1f%%", rate);
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString(lbl, xs[i] - fm2.stringWidth(lbl) / 2, ys[i] - 8);

                // X-axis label (month/period start)
                g2.setColor(TEXT_SECONDARY);
                String xLbl = data.get(i).getStartDate().toString();
                if (xLbl.length() > 7) xLbl = xLbl.substring(0, 7); // YYYY-MM
                g2.drawString(xLbl, xs[i] - fm2.stringWidth(xLbl) / 2, h - padBottom + 14);
            }

            // Axes
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(padLeft, padTop, padLeft, padTop + chartH);
            g2.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 6: Risk Evaluation
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildRiskTab() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(16, 0, 0, 0));

        // ── Top action bar ──────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(BG_CARD);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(14, 18, 14, 18)
        ));

        // Left: single-evaluate form
        JPanel singlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        singlePanel.setOpaque(false);
        JLabel singleLbl = new JLabel("Employee ID:");
        singleLbl.setForeground(TEXT_SECONDARY);
        singleLbl.setFont(FONT_BODY);
        JTextField singleIdField = styledTextField("e.g. 3");
        singleIdField.setPreferredSize(new Dimension(120, 34));
        singleIdField.setMaximumSize(new Dimension(120, 34));
        JButton evalOneBtn = styledButton("🔍  Evaluate Employee", ACCENT_BLUE);
        evalOneBtn.setPreferredSize(new Dimension(220, 38));
        singlePanel.add(singleLbl);
        singlePanel.add(singleIdField);
        singlePanel.add(evalOneBtn);

        // Right: bulk + filter
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        JButton evalAllBtn = styledButton("⚠️  Evaluate All Employees", ACCENT_ORANGE);
        evalAllBtn.setPreferredSize(new Dimension(240, 38));

        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setForeground(TEXT_SECONDARY);
        filterLbl.setFont(FONT_BODY);
        JComboBox<String> filterCombo = styledCombo(
                new String[]{"All", "HIGH", "MEDIUM", "LOW"});
        filterCombo.setPreferredSize(new Dimension(120, 34));
        filterCombo.setMaximumSize(new Dimension(120, 34));

        rightPanel.add(filterLbl);
        rightPanel.add(filterCombo);
        rightPanel.add(evalAllBtn);

        topBar.add(singlePanel, BorderLayout.WEST);
        topBar.add(rightPanel,  BorderLayout.EAST);

        // ── Results table ──────────────────────────────────────────────────
        String[] cols = {"Assessment ID", "Employee ID", "Risk Level",
                         "Attendance Score", "Absenteeism %", "Promotions", "Reason", "Evaluated At"};
        DefaultTableModel riskModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Custom renderer: color-code rows by risk level
        JTable riskTable = new JTable(riskModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(ACCENT_BLUE.darker());
                    c.setForeground(Color.WHITE);
                } else {
                    Object levelVal = riskModel.getValueAt(row, 2);
                    String level = levelVal != null ? levelVal.toString() : "";
                    switch (level) {
                        case "HIGH":   c.setBackground(new Color(80, 20, 20));  c.setForeground(ACCENT_RED);    break;
                        case "MEDIUM": c.setBackground(new Color(70, 45, 10));  c.setForeground(ACCENT_ORANGE); break;
                        case "LOW":    c.setBackground(new Color(15, 50, 30));  c.setForeground(ACCENT_GREEN);  break;
                        default:       c.setBackground(BG_CARD);                c.setForeground(TEXT_PRIMARY);  break;
                    }
                }
                return c;
            }
        };
        riskTable.setBackground(BG_CARD);
        riskTable.setForeground(TEXT_PRIMARY);
        riskTable.setFont(FONT_BODY);
        riskTable.setRowHeight(30);
        riskTable.setGridColor(BORDER_COLOR);
        riskTable.setShowGrid(true);
        riskTable.setFillsViewportHeight(true);
        riskTable.setSelectionBackground(ACCENT_BLUE.darker());
        riskTable.setSelectionForeground(Color.WHITE);
        JTableHeader riskHeader = riskTable.getTableHeader();
        riskHeader.setBackground(INPUT_BG);
        riskHeader.setForeground(ACCENT_ORANGE);
        riskHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        riskHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_ORANGE));
        riskHeader.setPreferredSize(new Dimension(0, 38));
        // Widen the Reason column
        riskTable.getColumnModel().getColumn(6).setPreferredWidth(300);
        riskTable.getColumnModel().getColumn(7).setPreferredWidth(180);

        JScrollPane tableScroll = styledScrollPane(riskTable);

        // ── Summary bar ──────────────────────────────────────────────────
        JPanel summaryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 4));
        summaryBar.setBackground(BG_DARK);
        JLabel highCount   = kpiChip("HIGH",   "0", ACCENT_RED);
        JLabel medCount    = kpiChip("MEDIUM", "0", ACCENT_ORANGE);
        JLabel lowCount    = kpiChip("LOW",    "0", ACCENT_GREEN);
        JLabel totalCount  = kpiChip("Total",  "0", ACCENT_BLUE);
        summaryBar.add(totalCount);
        summaryBar.add(highCount);
        summaryBar.add(medCount);
        summaryBar.add(lowCount);

        // Helper to populate the table and update summary chips
        Runnable populateTable = () -> {
            riskModel.setRowCount(0);
            String filterVal = (String) filterCombo.getSelectedItem();
            List<RiskAssessment> results;
            try {
                if (filterVal == null || filterVal.equals("All")) {
                    results = riskController.handleGetAll();
                } else {
                    results = riskController.handleGetFlagged(filterVal);
                }
            } catch (HRMSException ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ " + ex.getErrorCode() + ": " + ex.getMessage(),
                        "Risk Query Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            long h = 0, m = 0, l = 0;
            for (RiskAssessment ra : results) {
                riskModel.addRow(new Object[]{
                    ra.getRiskAssessmentId(),
                    ra.getEmployeeId(),
                    ra.getRiskLevel().name(),
                    String.format("%.1f%%", ra.getPerformanceScore()),
                    String.format("%.1f%%", ra.getAbsenteeismRate()),
                    ra.getPromotionGap(),
                    ra.getReason(),
                    ra.getEvaluatedAt()
                });
                if (ra.getRiskLevel() == RiskLevel.HIGH)   h++;
                else if (ra.getRiskLevel() == RiskLevel.MEDIUM) m++;
                else l++;
            }
            // Update chips (only meaningful when showing All)
            if (filterVal == null || filterVal.equals("All")) {
                updateChip(highCount,  "HIGH",   h);
                updateChip(medCount,   "MEDIUM", m);
                updateChip(lowCount,   "LOW",    l);
                updateChip(totalCount, "Total",  h + m + l);
            } else {
                updateChip(totalCount, "Total", results.size());
            }
        };

        // ── Wire actions ─────────────────────────────────────────────────
        evalOneBtn.addActionListener(e -> {
            try {
                int empId = Integer.parseInt(singleIdField.getText().trim());
                RiskAssessment ra = riskController.handleEvaluateRisk(empId);
                JOptionPane.showMessageDialog(this,
                        String.format("✅ Employee %d evaluated.%nRisk Level: %s%nReason: %s",
                                empId, ra.getRiskLevel(), ra.getReason()),
                        "Risk Evaluated", JOptionPane.INFORMATION_MESSAGE);
                populateTable.run();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ INVALID_INPUT: Enter a valid numeric Employee ID.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (HRMSException ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ " + ex.getErrorCode() + ": " + ex.getMessage(),
                        ex.getErrorCode().toString(), JOptionPane.ERROR_MESSAGE);
            }
        });

        evalAllBtn.addActionListener(e -> {
            try {
                List<RiskAssessment> all = riskController.handleEvaluateAll();
                populateTable.run();
                JOptionPane.showMessageDialog(this,
                        "✅ Bulk evaluation complete. " + all.size() + " employee(s) assessed.",
                        "Evaluation Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (HRMSException ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ " + ex.getErrorCode() + ": " + ex.getMessage(),
                        ex.getErrorCode().toString(), JOptionPane.ERROR_MESSAGE);
            }
        });

        filterCombo.addActionListener(e -> populateTable.run());

        root.add(topBar,      BorderLayout.NORTH);
        root.add(summaryBar,  BorderLayout.SOUTH);
        root.add(tableScroll, BorderLayout.CENTER);
        return root;
    }

    /** Small inline KPI chip label for the risk summary bar. */
    private JLabel kpiChip(String level, String count, Color accent) {
        JLabel lbl = new JLabel(level + ": " + count);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(accent);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                new EmptyBorder(4, 12, 4, 12)
        ));
        return lbl;
    }

    private void updateChip(JLabel chip, String level, long count) {
        // Extract and replace only the count part after the colon
        String text = chip.getText();
        int colon = text.indexOf(":");
        chip.setText(text.substring(0, colon + 2) + count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 7: Analytics Dashboard
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildDashboardTab() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);

        // Mutable snapshot reference for the refresh lambda
        final DashboardSnapshot[] snapRef = {null};

        // ── KPI bar (NORTH) ─────────────────────────────────────────────────
        JPanel kpiBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        kpiBar.setBackground(BG_CARD);
        kpiBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(4, 12, 4, 12)));

        JLabel kpiTotal      = dashKpi("Total Employees", "—", ACCENT_BLUE);
        JLabel kpiActive     = dashKpi("Active",          "—", ACCENT_GREEN);
        JLabel kpiExits      = dashKpi("Exits",           "—", ACCENT_RED);
        JLabel kpiHighRisk   = dashKpi("High Risk",       "—", ACCENT_ORANGE);
        JLabel kpiAttrition  = dashKpi("Attrition Rate",  "—%", ACCENT_PURPLE);
        kpiBar.add(makeKpiCard("Total Employees", kpiTotal,    ACCENT_BLUE));
        kpiBar.add(makeKpiCard("Active",          kpiActive,   ACCENT_GREEN));
        kpiBar.add(makeKpiCard("Exits",           kpiExits,    ACCENT_RED));
        kpiBar.add(makeKpiCard("High Risk",       kpiHighRisk, ACCENT_ORANGE));
        kpiBar.add(makeKpiCard("Attrition Rate",  kpiAttrition,ACCENT_PURPLE));

        // ── Filter bar (below KPI) ───────────────────────────────────────────
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        filterBar.setBackground(BG_DARK);
        filterBar.setBorder(new EmptyBorder(6, 4, 0, 4));

        JLabel fDeptLbl = new JLabel("Department:");
        fDeptLbl.setForeground(TEXT_SECONDARY); fDeptLbl.setFont(FONT_BODY);
        JComboBox<String> deptFilter = styledCombo(new String[]{
                "All Departments","Engineering","Sales","Marketing","HR",
                "Finance","Operations","Legal","Product","Design","Support"});
        deptFilter.setPreferredSize(new Dimension(160, 32));

        JLabel fPeriodLbl = new JLabel("Period:");
        fPeriodLbl.setForeground(TEXT_SECONDARY); fPeriodLbl.setFont(FONT_BODY);
        JComboBox<String> periodFilter = styledCombo(new String[]{"Monthly","Quarterly","Annual"});
        periodFilter.setPreferredSize(new Dimension(120, 32));

        JLabel fStartLbl = new JLabel("From:");
        fStartLbl.setForeground(TEXT_SECONDARY); fStartLbl.setFont(FONT_BODY);
        JTextField dashStartField = styledTextField("YYYY-MM-DD");
        dashStartField.setText(LocalDate.now().minusYears(1).withDayOfMonth(1).toString());
        dashStartField.setPreferredSize(new Dimension(120, 32));

        JLabel fEndLbl = new JLabel("To:");
        fEndLbl.setForeground(TEXT_SECONDARY); fEndLbl.setFont(FONT_BODY);
        JTextField dashEndField = styledTextField("YYYY-MM-DD");
        dashEndField.setText(LocalDate.now().toString());
        dashEndField.setPreferredSize(new Dimension(120, 32));

        JButton refreshDashBtn = styledButton("🔄  Refresh Dashboard", ACCENT_BLUE);
        refreshDashBtn.setPreferredSize(new Dimension(200, 34));

        filterBar.add(fDeptLbl); filterBar.add(deptFilter);
        filterBar.add(fPeriodLbl); filterBar.add(periodFilter);
        filterBar.add(fStartLbl); filterBar.add(dashStartField);
        filterBar.add(fEndLbl); filterBar.add(dashEndField);
        filterBar.add(refreshDashBtn);

        // ── Department bar chart (CENTER-LEFT) ─────────────────────────────
        DeptAttritionChart deptChart = new DeptAttritionChart();
        deptChart.setPreferredSize(new Dimension(460, 0));
        deptChart.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_BLUE.darker()),
                "  Department-wise Attrition %  ",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                FONT_BODY, ACCENT_BLUE));

        // ── Right panel: Correlation + Root Causes ─────────────────────────
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(BG_DARK);
        rightPanel.setBorder(new EmptyBorder(0, 8, 0, 0));

        // Correlation panel
        JPanel corrPanel = new JPanel();
        corrPanel.setLayout(new BoxLayout(corrPanel, BoxLayout.Y_AXIS));
        corrPanel.setBackground(BG_CARD);
        corrPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_PURPLE.darker()),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel corrTitle = new JLabel("Correlation Analysis");
        corrTitle.setFont(FONT_HEADING); corrTitle.setForeground(ACCENT_PURPLE);
        corrTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        corrTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0, BORDER_COLOR),
                new EmptyBorder(0,0,8,0)));
        corrTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel[] corrBars  = new JLabel[3];
        JLabel[] corrVals  = new JLabel[3];
        String[] corrNames = {"Attendance", "Promotion", "Tenure"};
        Color[]  corrColors = {ACCENT_RED, ACCENT_ORANGE, ACCENT_BLUE};

        corrPanel.add(corrTitle);
        corrPanel.add(Box.createVerticalStrut(10));
        for (int i = 0; i < 3; i++) {
            JLabel name = new JLabel(corrNames[i]);
            name.setFont(FONT_BODY); name.setForeground(TEXT_SECONDARY);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            corrVals[i] = new JLabel("—");
            corrVals[i].setFont(new Font("Segoe UI", Font.BOLD, 13));
            corrVals[i].setForeground(corrColors[i]);
            corrVals[i].setAlignmentX(Component.RIGHT_ALIGNMENT);
            corrBars[i] = new JLabel();
            corrBars[i].setOpaque(true);
            corrBars[i].setBackground(corrColors[i].darker());
            corrBars[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            corrBars[i].setPreferredSize(new Dimension(0, 8));
            corrBars[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel row = new JPanel(new BorderLayout(6,0));
            row.setOpaque(false);
            row.add(name, BorderLayout.WEST); row.add(corrVals[i], BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            corrPanel.add(row);
            corrPanel.add(corrBars[i]);
            corrPanel.add(Box.createVerticalStrut(8));
        }

        // Root causes table
        JPanel causesPanel = new JPanel(new BorderLayout(0, 6));
        causesPanel.setBackground(BG_DARK);
        JLabel causesTitle = sectionLabel("🔍 Root Cause Findings");
        causesTitle.setForeground(ACCENT_ORANGE);
        String[] causesCols = {"#", "Cause", "Impact Score", "Description"};
        DefaultTableModel causesModel = new DefaultTableModel(causesCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable causesTable = styledTable(causesModel);
        causesTable.getColumnModel().getColumn(3).setPreferredWidth(300);
        JScrollPane causesScroll = styledScrollPane(causesTable);
        causesPanel.add(causesTitle,  BorderLayout.NORTH);
        causesPanel.add(causesScroll, BorderLayout.CENTER);

        rightPanel.add(corrPanel,   BorderLayout.NORTH);
        rightPanel.add(causesPanel, BorderLayout.CENTER);

        // ── Center: chart + filter stacked ─────────────────────────────────
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                deptChart, rightPanel);
        centerSplit.setResizeWeight(0.55);
        centerSplit.setDividerSize(5);
        centerSplit.setBackground(BG_DARK);
        centerSplit.setBorder(null);

        JPanel northStack = new JPanel(new BorderLayout());
        northStack.setBackground(BG_DARK);
        northStack.add(kpiBar,     BorderLayout.NORTH);
        northStack.add(filterBar,  BorderLayout.SOUTH);

        root.add(northStack,   BorderLayout.NORTH);
        root.add(centerSplit,  BorderLayout.CENTER);

        // ── Refresh logic ─────────────────────────────────────────────────
        Runnable refreshDash = () -> {
            try {
                LocalDate start = LocalDate.parse(dashStartField.getText().trim());
                LocalDate end   = LocalDate.parse(dashEndField.getText().trim());
                String selDept  = (String) deptFilter.getSelectedItem();
                String selPer   = (String) periodFilter.getSelectedItem();
                PeriodType pt   = "Quarterly".equals(selPer) ? PeriodType.QUARTERLY
                                : "Annual".equals(selPer) ? PeriodType.ANNUAL
                                : PeriodType.MONTHLY;

                DashboardFilter df = new DashboardFilter(
                        start, end, pt,
                        "All Departments".equals(selDept) ? null : selDept);

                DashboardSnapshot snap = dashboardController.handleBuildDashboard(df);
                snapRef[0] = snap;

                // KPI
                kpiTotal.setText(String.valueOf(snap.getTotalEmployees()));
                kpiActive.setText(String.valueOf(snap.getActiveEmployees()));
                kpiExits.setText(String.valueOf(snap.getTotalExits()));
                kpiHighRisk.setText(String.valueOf(snap.getHighRiskCount()));
                kpiAttrition.setText(String.format("%.1f%%", snap.getOverallAttritionRate()));
                kpiAttrition.setForeground(
                        snap.getOverallAttritionRate() >= 20 ? ACCENT_RED :
                        snap.getOverallAttritionRate() >= 10 ? ACCENT_ORANGE : ACCENT_GREEN);

                // Dept chart
                if (snap.getDepartmentAttritionRates() != null) {
                    deptChart.setData(snap.getDepartmentAttritionRates());
                }

                // Correlation bars
                if (snap.getCorrelationReport() != null) {
                    CorrelationReport cr = snap.getCorrelationReport();
                    double[] vals = {
                        cr.getAttendanceCorrelation(),
                        cr.getPromotionCorrelation(),
                        cr.getTenureCorrelation()
                    };
                    for (int i = 0; i < 3; i++) {
                        corrVals[i].setText(String.format("%.3f", vals[i]));
                        int barW = (int)(Math.abs(vals[i]) * 200);
                        corrBars[i].setPreferredSize(new Dimension(barW, 8));
                        corrBars[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
                    }
                    corrPanel.revalidate(); corrPanel.repaint();
                }

                // Root causes
                causesModel.setRowCount(0);
                if (snap.getRootCauses() != null) {
                    int idx = 1;
                    for (RootCauseFinding f : snap.getRootCauses()) {
                        causesModel.addRow(new Object[]{
                            idx++,
                            f.getCauseType(),
                            String.format("%.1f/100", f.getImpactScore()),
                            f.getDescription()
                        });
                    }
                }

            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ INVALID_DATE_RANGE: Use YYYY-MM-DD format.",
                    "Date Error", JOptionPane.ERROR_MESSAGE);
            } catch (HRMSException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ " + ex.getErrorCode() + ": " + ex.getMessage(),
                    ex.getErrorCode().toString(), JOptionPane.ERROR_MESSAGE);
            }
        };

        refreshDashBtn.addActionListener(e -> refreshDash.run());
        periodFilter.addActionListener(e -> refreshDash.run());
        deptFilter.addActionListener(e -> refreshDash.run());

        return root;
    }

    // ── Dashboard KPI helpers ─────────────────────────────────────────────────

    private JLabel dashKpi(String label, String init, Color accent) {
        JLabel lbl = new JLabel(init);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lbl.setForeground(accent);
        return lbl;
    }

    private JPanel makeKpiCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(INPUT_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                new EmptyBorder(10, 20, 10, 20)));

        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL); lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valueLabel);
        card.add(lbl);
        return card;
    }

    /**
     * Java2D horizontal bar chart for per-department attrition rates.
     * Drawn inside the Analytics Dashboard tab.
     */
    private class DeptAttritionChart extends JPanel {
        private Map<String, double[]> data;

        public DeptAttritionChart() {
            setBackground(BG_CARD);
        }

        public void setData(Map<String, double[]> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padLeft = 110, padRight = 60, padTop = 20, padBottom = 20;
            int chartW = w - padLeft - padRight;

            if (data == null || data.isEmpty()) {
                g2.setColor(TEXT_SECONDARY); g2.setFont(FONT_BODY);
                String msg = "Click \"Refresh Dashboard\" to load data";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose(); return;
            }

            String[] depts = data.keySet().toArray(new String[0]);
            int n = depts.length;
            int barH  = Math.min(28, (h - padTop - padBottom - (n - 1) * 6) / Math.max(n, 1));
            int gapY  = 6;
            double maxRate = 100.0;

            g2.setFont(FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i < n; i++) {
                String dept = depts[i];
                double[] vals = data.get(dept);
                double rate   = vals[2];
                int y = padTop + i * (barH + gapY);

                // Label
                g2.setColor(TEXT_SECONDARY);
                g2.drawString(dept, padLeft - fm.stringWidth(dept) - 6,
                        y + barH / 2 + fm.getAscent() / 2 - 1);

                // Background track
                g2.setColor(BORDER_COLOR);
                g2.fillRoundRect(padLeft, y, chartW, barH, 4, 4);

                // Filled bar
                Color barColor = rate >= 25 ? ACCENT_RED
                               : rate >= 15 ? ACCENT_ORANGE
                               : rate >= 8  ? new Color(230, 197, 71)
                               : ACCENT_GREEN;
                int fillW = (int)(chartW * rate / maxRate);
                g2.setColor(barColor);
                g2.fillRoundRect(padLeft, y, Math.max(fillW, 4), barH, 4, 4);

                // Rate label
                g2.setColor(TEXT_PRIMARY);
                String rateTxt = String.format("%.1f%%", rate);
                g2.drawString(rateTxt, padLeft + fillW + 4,
                        y + barH / 2 + fm.getAscent() / 2 - 1);
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global UI customizations
        UIManager.put("OptionPane.background", new Color(22, 27, 44));
        UIManager.put("Panel.background", new Color(22, 27, 44));
        UIManager.put("OptionPane.messageForeground", new Color(237, 242, 247));

        SwingUtilities.invokeLater(MainDashboard::new);
    }
}
