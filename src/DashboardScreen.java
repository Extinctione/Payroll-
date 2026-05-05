import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * DashboardScreen.java — Main application screen after login.
 *
 * Admin users see the full employee list with CRUD + export controls.
 * Regular employees see only their own record and payroll breakdown.
 *
 * FIXES IN THIS VERSION:
 * - Full-page scroll works correctly (BorderLayout.CENTER on pageScroll)
 * - Table width uses reasonable preferred sizes, not Integer.MAX_VALUE
 * - JPasswordField(String) deprecated constructor removed — use setText()
 * - Breakdown panel uses GridBagLayout for clean alignment
 * - Search bar and CRUD buttons are hidden for non-admin users
 * - Export works for both admin (all employees) and regular (self only)
 * - All file paths are consistent via EmployeeService.DATA_DIR
 * - No unsafe unchecked casts (scoped @SuppressWarnings per method)
 * - Breakdown section always visible; clears when no row selected
 */
public class DashboardScreen extends JPanel {

    private static final String[] DEPARTMENTS = {"HR", "IT", "Finance"};
    private static final String[] POSITIONS   = {"Developer", "Manager", "Admin"};

    private final Main              app;
    private final EmployeeService   service;
    private final AttendanceService attendanceService;

    // ── Fixed top bar ──────────────────────────────────────────────────────────
    private JLabel welcomeLabel;

    // ── Scrollable content refs ────────────────────────────────────────────────
    private JTextField    searchField;
    private JPanel        searchPanel, crudPanel, exportPanel;
    private DefaultTableModel tableModel;
    private JTable        employeeTable;

    // ── Breakdown labels ──────────────────────────────────────────────────────
    private JLabel basicLabel, daysLabel, overtimeLabel, leaveLabel,
                   taxLabel, pfLabel, netLabel;

    private List<Employee> allEmployees = new ArrayList<>();

    public DashboardScreen(Main app, EmployeeService service) {
        this.app               = app;
        this.service           = service;
        this.attendanceService = new AttendanceService();
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        // Fixed top bar — never scrolls
        JPanel topBar = buildTopBar();
        add(topBar, BorderLayout.NORTH);

        // Everything below scrolls together
        JPanel content = buildScrollableContent();
        JScrollPane pageScroll = new JScrollPane(content);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);
        pageScroll.getViewport().setBackground(Theme.BG);
        add(pageScroll, BorderLayout.CENTER); // CENTER = fills remaining frame height
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Theme.PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        welcomeLabel = new JLabel("Welcome");
        welcomeLabel.setFont(Theme.FONT_HEADER);
        welcomeLabel.setForeground(Color.WHITE);
        panel.add(welcomeLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(Theme.PRIMARY);

        JButton editBtn = new JButton("Edit Profile");
        editBtn.setFont(Theme.FONT_BTN);
        editBtn.setForeground(Theme.PRIMARY);
        editBtn.setBackground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editBtn.addActionListener(e -> showEditProfileDialog());

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(Theme.FONT_BTN);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(Theme.PRIMARY_HO);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> app.logout());

        right.add(editBtn);
        right.add(logoutBtn);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ── Scrollable Content ────────────────────────────────────────────────────

    private JPanel buildScrollableContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        // Search bar (admin only — hidden for regular employees via init())
        searchPanel = buildSearchBar();
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(searchPanel);
        content.add(Box.createVerticalStrut(8));

        // Employee table
        JScrollPane tableScroll = buildTableScrollPane();
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(600, 220));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        content.add(tableScroll);
        content.add(Box.createVerticalStrut(14));

        // Salary breakdown
        JPanel breakdown = buildBreakdownPanel();
        breakdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        breakdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        content.add(breakdown);
        content.add(Box.createVerticalStrut(14));

        // Button row (CRUD admin-only + export)
        JPanel buttons = buildButtonRow();
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(buttons);
        content.add(Box.createVerticalStrut(10));

        return content;
    }

    // ── Search Bar ────────────────────────────────────────────────────────────

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(Theme.BG);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = new JLabel("Search:");
        lbl.setFont(Theme.FONT_LABEL);
        lbl.setForeground(Theme.TEXT_MID);
        panel.add(lbl, BorderLayout.WEST);

        searchField = new JTextField();
        Theme.styleInput(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filterTable(); }
            public void removeUpdate(DocumentEvent e)  { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });
        panel.add(searchField, BorderLayout.CENTER);
        return panel;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private JScrollPane buildTableScrollPane() {
        tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Email", "Phone", "Dept", "Position", "Basic Salary", "Role"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        employeeTable = new JTable(tableModel);
        Theme.styleTable(employeeTable);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.getSelectionModel().addListSelectionListener(
            e -> { if (!e.getValueIsAdjusting()) showSalaryBreakdown(); }
        );

        // Reasonable column widths (no Integer.MAX_VALUE)
        int[] widths = {55, 120, 165, 105, 65, 90, 105, 65};
        for (int i = 0; i < widths.length; i++) {
            employeeTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane sp = new JScrollPane(employeeTable);
        Theme.styleScrollPane(sp);
        sp.getViewport().setBackground(Theme.BG_CARD);
        return sp;
    }

    // ── Breakdown Panel ───────────────────────────────────────────────────────

    private JPanel buildBreakdownPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.BG_CARD);
        outer.setBorder(Theme.sectionBorder("Payroll Breakdown  (click a row to view)"));

        // Use GridLayout 2×4 — clean and stable regardless of label widths
        JPanel grid = new JPanel(new GridLayout(2, 4, 16, 6));
        grid.setBackground(Theme.BG_CARD);
        grid.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));

        basicLabel    = breakdownLabel("Basic: —");
        daysLabel     = breakdownLabel("Days Worked: —");
        overtimeLabel = breakdownLabel("Overtime Hrs: —");
        leaveLabel    = breakdownLabel("Leave Days: —");
        taxLabel      = breakdownLabel("Tax (10%): —");
        pfLabel       = breakdownLabel("PF (5%): —");
        netLabel      = breakdownLabel("Net Salary: —");

        grid.add(basicLabel);
        grid.add(daysLabel);
        grid.add(overtimeLabel);
        grid.add(leaveLabel);
        grid.add(taxLabel);
        grid.add(pfLabel);
        grid.add(netLabel);
        grid.add(new JLabel()); // filler cell

        outer.add(grid, BorderLayout.CENTER);
        return outer;
    }

    // ── Button Row ────────────────────────────────────────────────────────────

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Theme.BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // CRUD — admin only
        crudPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        crudPanel.setBackground(Theme.BG);
        crudPanel.add(Theme.primaryButton("Add Employee",    e -> showAddDialog()));
        crudPanel.add(Theme.outlineButton("Update Employee", e -> showUpdateDialog()));
        crudPanel.add(Theme.outlineButton("Delete Employee", e -> handleDelete()));

        // Export — available to all (employees only see their own data)
        exportPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        exportPanel.setBackground(Theme.BG);
        exportPanel.add(Theme.outlineButton("Export CSV", e -> exportCSV()));
        exportPanel.add(Theme.outlineButton("Export TXT", e -> exportTXT()));

        row.add(crudPanel,   BorderLayout.CENTER);
        row.add(exportPanel, BorderLayout.EAST);
        return row;
    }

    // ── Init (called on every login) ──────────────────────────────────────────

    public void init(Employee user) {
        boolean isAdmin = user.isAdmin();
        welcomeLabel.setText("Welcome, " + user.getName()
            + "   [" + user.getRole().toUpperCase() + "  —  " + user.getPosition() + "]");
        searchField.setText("");
        searchPanel.setVisible(isAdmin);
        crudPanel.setVisible(isAdmin);
        refreshTable();
        if (!isAdmin) selectRowByEmail(user.getEmail());
    }

    // ── Table Refresh / Filter ────────────────────────────────────────────────

    private void refreshTable() {
        Employee user = app.getCurrentUser();
        allEmployees  = service.loadEmployees();

        if (user != null && !user.isAdmin()) {
            List<Employee> self = new ArrayList<>();
            for (Employee e : allEmployees) {
                if (e.getEmail().equalsIgnoreCase(user.getEmail())) {
                    self.add(e); break;
                }
            }
            repopulateTable(self);
        } else {
            repopulateTable(allEmployees);
        }
        clearBreakdown();
    }

    private void filterTable() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            repopulateTable(allEmployees);
        } else {
            List<Employee> filtered = new ArrayList<>();
            for (Employee e : allEmployees) {
                if (e.getName().toLowerCase().contains(query) ||
                    e.getEmail().toLowerCase().contains(query) ||
                    e.getDepartment().toLowerCase().contains(query)) {
                    filtered.add(e);
                }
            }
            repopulateTable(filtered);
        }
        clearBreakdown();
    }

    private void repopulateTable(List<Employee> list) {
        tableModel.setRowCount(0);
        for (Employee e : list) {
            tableModel.addRow(new Object[]{
                e.getId(), e.getName(), e.getEmail(),
                e.getPhoneNumber(), e.getDepartment(), e.getPosition(),
                currency(e.getSalary()), e.getRole()
            });
        }
    }

    private void selectRowByEmail(String email) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (email.equalsIgnoreCase((String) tableModel.getValueAt(i, 2))) {
                employeeTable.setRowSelectionInterval(i, i);
                employeeTable.scrollRectToVisible(employeeTable.getCellRect(i, 0, true));
                break;
            }
        }
    }

    // ── Salary Breakdown ──────────────────────────────────────────────────────

    private void showSalaryBreakdown() {
        int row = employeeTable.getSelectedRow();
        if (row < 0) { clearBreakdown(); return; }

        String email = (String) tableModel.getValueAt(row, 2);
        Employee emp = service.findByEmail(email);
        if (emp == null) { clearBreakdown(); return; }

        double basic   = emp.getSalary();
        double tax     = service.calculateTax(basic);
        double pf      = service.calculatePF(basic);

        AttendanceRecord rec = attendanceService.findByEmployeeId(emp.getId());
        int    days     = (rec != null) ? rec.getDaysWorked()    : 30;
        double overtime = (rec != null) ? rec.getOvertimeHours() : 0;
        int    leave    = (rec != null) ? rec.getLeaveDays()     : 0;
        double net      = service.calculateAttendanceSalary(basic, days, overtime, leave);

        basicLabel.setText("Basic: "         + currency(basic));
        daysLabel.setText( "Days Worked: "   + days);
        overtimeLabel.setText("Overtime: "   + overtime + " hrs");
        leaveLabel.setText("Leave Days: "    + leave);
        taxLabel.setText(  "Tax (10%): "     + currency(tax));
        pfLabel.setText(   "PF (5%): "       + currency(pf));
        netLabel.setText(  "Net Salary: "    + currency(net));
        netLabel.setForeground(Theme.PRIMARY);
        netLabel.setFont(Theme.FONT_HEADER);
    }

    private void clearBreakdown() {
        basicLabel.setText("Basic: —");
        daysLabel.setText( "Days Worked: —");
        overtimeLabel.setText("Overtime: —");
        leaveLabel.setText("Leave Days: —");
        taxLabel.setText(  "Tax (10%): —");
        pfLabel.setText(   "PF (5%): —");
        netLabel.setText(  "Net Salary: —");
        netLabel.setForeground(Theme.TEXT_DARK);
        netLabel.setFont(Theme.FONT_LABEL);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private List<Employee> exportTargetList() {
        Employee user = app.getCurrentUser();
        if (user != null && !user.isAdmin()) {
            List<Employee> self = new ArrayList<>();
            for (Employee e : service.loadEmployees()) {
                if (e.getEmail().equalsIgnoreCase(user.getEmail())) {
                    self.add(e); break;
                }
            }
            return self;
        }
        return service.loadEmployees();
    }

    /** Resolves export file to the same directory as employees.txt. */
    private String resolveExportPath(String filename) {
        return new File(EmployeeService.DATA_DIR, filename).getAbsolutePath();
    }

    private void exportCSV() {
        String path = resolveExportPath("payroll_report.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("id,name,email,department,position,basic,tax,pf,daysWorked,overtimeHours,leaveDays,netSalary");
            for (Employee e : exportTargetList()) {
                double basic = e.getSalary();
                double tax   = service.calculateTax(basic);
                double pf    = service.calculatePF(basic);
                AttendanceRecord rec = attendanceService.findByEmployeeId(e.getId());
                int    days  = (rec != null) ? rec.getDaysWorked()    : 30;
                double ot    = (rec != null) ? rec.getOvertimeHours() : 0;
                int    leave = (rec != null) ? rec.getLeaveDays()     : 0;
                double net   = service.calculateAttendanceSalary(basic, days, ot, leave);
                pw.printf("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%d,%.1f,%d,%.2f%n",
                    e.getId(), e.getName(), e.getEmail(),
                    e.getDepartment(), e.getPosition(),
                    basic, tax, pf, days, ot, leave, net);
            }
            JOptionPane.showMessageDialog(this,
                "CSV saved to:\n" + path, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Failed to export CSV:\n" + ex.getMessage());
        }
    }

    private void exportTXT() {
        String path = resolveExportPath("payroll_report.txt");
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("==========================================================");
            pw.println("           EMPLOYEE PAYROLL REPORT");
            pw.println("           Generated: " + date);
            pw.println("==========================================================");
            pw.println();
            for (Employee e : exportTargetList()) {
                double basic = e.getSalary();
                double tax   = service.calculateTax(basic);
                double pf    = service.calculatePF(basic);
                AttendanceRecord rec = attendanceService.findByEmployeeId(e.getId());
                int    days  = (rec != null) ? rec.getDaysWorked()    : 30;
                double ot    = (rec != null) ? rec.getOvertimeHours() : 0;
                int    leave = (rec != null) ? rec.getLeaveDays()     : 0;
                double net   = service.calculateAttendanceSalary(basic, days, ot, leave);

                pw.println("----------------------------------------------------------");
                pw.printf("  ID         : %s%n", e.getId());
                pw.printf("  Name       : %s%n", e.getName());
                pw.printf("  Email      : %s%n", e.getEmail());
                pw.printf("  Department : %s%n", e.getDepartment());
                pw.printf("  Position   : %s%n", e.getPosition());
                pw.println("  ── Attendance ──────────────────────────────────────");
                pw.printf("  Days Worked    : %d%n",   days);
                pw.printf("  Overtime Hours : %.1f%n", ot);
                pw.printf("  Leave Days     : %d%n",   leave);
                pw.println("  ── Salary ──────────────────────────────────────────");
                pw.printf("  Basic Salary   : $%,.2f%n", basic);
                pw.printf("  Tax (10%%)      : $%,.2f%n", tax);
                pw.printf("  PF  (5%%)       : $%,.2f%n", pf);
                pw.printf("  NET SALARY     : $%,.2f%n", net);
                pw.println();
            }
            pw.println("==========================================================");
            JOptionPane.showMessageDialog(this,
                "TXT saved to:\n" + path, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Failed to export TXT:\n" + ex.getMessage());
        }
    }

    // ── CRUD Dialogs (Admin only) ─────────────────────────────────────────────

    private void showAddDialog() {
        JTextField     idF    = new JTextField();
        JTextField     nameF  = new JTextField();
        JTextField     emailF = new JTextField();
        JPasswordField passF  = new JPasswordField();
        JTextField     phoneF = new JTextField();
        JTextField     salF   = new JTextField();
        JComboBox<String> deptC = new JComboBox<>(DEPARTMENTS);
        JComboBox<String> posC  = new JComboBox<>(POSITIONS);
        JPasswordField keyF   = new JPasswordField();
        JTextField daysF = new JTextField("30");
        JTextField otF   = new JTextField("0");
        JTextField lvF   = new JTextField("0");

        JPanel form = buildDialogForm();
        addFormRow(form, "Employee ID",          idF);
        addFormRow(form, "Full Name",            nameF);
        addFormRow(form, "Email",                emailF);
        addFormRow(form, "Password",             passF);
        addFormRow(form, "Phone",                phoneF);
        addFormRow(form, "Basic Salary",         salF);
        addFormRow(form, "Department",           deptC);
        addFormRow(form, "Position",             posC);
        addFormRow(form, "Admin Key (if Admin)", keyF);
        addFormRow(form, "Days Worked",          daysF);
        addFormRow(form, "Overtime Hours",       otF);
        addFormRow(form, "Leave Days",           lvF);

        if (showDialog(form, "Add Employee") != JOptionPane.OK_OPTION) return;

        String id    = idF.getText().trim();
        String name  = nameF.getText().trim();
        String email = emailF.getText().trim();
        String pass  = new String(passF.getPassword()).trim();
        String phone = phoneF.getText().trim();
        String sal   = salF.getText().trim();
        String dept  = (String) deptC.getSelectedItem();
        String pos   = (String) posC.getSelectedItem();

        String err = validateAll(id, name, email, pass, phone, sal);
        if (err != null) { showError(err); return; }

        String role = "employee";
        if ("Admin".equals(pos)) {
            if (!service.validateAdminKey(new String(keyF.getPassword()).trim())) {
                showError("Invalid admin key."); return;
            }
            role = "admin";
        }

        boolean ok = service.register(
            new Employee(id, name, email, pass, phone, dept, pos, Double.parseDouble(sal), role));
        if (!ok) { showError("Email already registered."); return; }

        saveAttendanceFromFields(id, daysF, otF, lvF);
        refreshTable();
    }

    private void showUpdateDialog() {
        int row = employeeTable.getSelectedRow();
        if (row < 0) { showError("Please select an employee to update."); return; }

        String origEmail = (String) tableModel.getValueAt(row, 2);
        Employee ex      = service.findByEmail(origEmail);
        if (ex == null) return;

        AttendanceRecord existingRec = attendanceService.findByEmployeeId(ex.getId());
        int    curDays = (existingRec != null) ? existingRec.getDaysWorked()    : 30;
        double curOt   = (existingRec != null) ? existingRec.getOvertimeHours() : 0;
        int    curLv   = (existingRec != null) ? existingRec.getLeaveDays()     : 0;

        JTextField     nameF  = new JTextField(ex.getName());
        JTextField     emailF = new JTextField(ex.getEmail());
        JPasswordField passF  = new JPasswordField();
        passF.setText(ex.getPassword());                       // no deprecated constructor
        JTextField     phoneF = new JTextField(ex.getPhoneNumber());
        JTextField     salF   = new JTextField(String.valueOf(ex.getSalary()));
        JComboBox<String> deptC = new JComboBox<>(DEPARTMENTS);
        JComboBox<String> posC  = new JComboBox<>(POSITIONS);
        JPasswordField keyF   = new JPasswordField();
        JTextField daysF = new JTextField(String.valueOf(curDays));
        JTextField otF   = new JTextField(String.valueOf(curOt));
        JTextField lvF   = new JTextField(String.valueOf(curLv));

        deptC.setSelectedItem(ex.getDepartment());
        posC.setSelectedItem(ex.getPosition());

        JPanel form = buildDialogForm();
        addFormRow(form, "Full Name",            nameF);
        addFormRow(form, "Email",                emailF);
        addFormRow(form, "Password",             passF);
        addFormRow(form, "Phone",                phoneF);
        addFormRow(form, "Basic Salary",         salF);
        addFormRow(form, "Department",           deptC);
        addFormRow(form, "Position",             posC);
        addFormRow(form, "Admin Key (if Admin)", keyF);
        addFormRow(form, "Days Worked",          daysF);
        addFormRow(form, "Overtime Hours",       otF);
        addFormRow(form, "Leave Days",           lvF);

        if (showDialog(form, "Update Employee") != JOptionPane.OK_OPTION) return;

        String name  = nameF.getText().trim();
        String email = emailF.getText().trim();
        String pass  = new String(passF.getPassword()).trim();
        String phone = phoneF.getText().trim();
        String sal   = salF.getText().trim();
        String dept  = (String) deptC.getSelectedItem();
        String pos   = (String) posC.getSelectedItem();

        String err = validateAll(ex.getId(), name, email, pass, phone, sal);
        if (err != null) { showError(err); return; }

        String role = "employee";
        if ("Admin".equals(pos)) {
            if (!service.validateAdminKey(new String(keyF.getPassword()).trim())) {
                showError("Invalid admin key."); return;
            }
            role = "admin";
        }

        service.updateEmployee(origEmail,
            new Employee(ex.getId(), name, email, pass, phone, dept, pos, Double.parseDouble(sal), role));
        saveAttendanceFromFields(ex.getId(), daysF, otF, lvF);
        refreshTable();
    }

    private void saveAttendanceFromFields(String empId,
            JTextField daysF, JTextField otF, JTextField lvF) {
        try {
            int    days = Integer.parseInt(daysF.getText().trim());
            double ot   = Double.parseDouble(otF.getText().trim());
            int    lv   = Integer.parseInt(lvF.getText().trim());
            attendanceService.saveOrUpdate(new AttendanceRecord(empId, days, ot, lv));
        } catch (NumberFormatException ignored) {}
    }

    private void handleDelete() {
        int row = employeeTable.getSelectedRow();
        if (row < 0) { showError("Please select an employee to delete."); return; }

        String email = (String) tableModel.getValueAt(row, 2);
        if (email.equalsIgnoreCase(app.getCurrentUser().getEmail())) {
            showError("You cannot delete your own account."); return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete employee: " + email + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Employee target = service.findByEmail(email);
            service.deleteEmployee(email);
            if (target != null) {
                List<AttendanceRecord> recs = attendanceService.loadAll();
                recs.removeIf(r -> r.getEmployeeId().equalsIgnoreCase(target.getId()));
                attendanceService.saveAll(recs);
            }
            refreshTable();
        }
    }

    // ── Edit Profile ──────────────────────────────────────────────────────────

    private void showEditProfileDialog() {
        Employee current = app.getCurrentUser();

        JTextField     nameF = new JTextField(current.getName());
        JPasswordField passF = new JPasswordField();
        passF.setText(current.getPassword());                  // no deprecated constructor
        JTextField     salF  = new JTextField(String.valueOf(current.getSalary()));

        JPanel form = buildDialogForm();
        addFormRow(form, "Full Name", nameF);
        addFormRow(form, "Password",  passF);
        if (current.isAdmin()) addFormRow(form, "Basic Salary", salF);

        if (showDialog(form, "Edit Profile") != JOptionPane.OK_OPTION) return;

        String name = nameF.getText().trim();
        String pass = new String(passF.getPassword()).trim();

        if (name.isEmpty() || pass.isEmpty()) { showError("Name and password are required."); return; }
        if (pass.length() < 4)               { showError("Password must be at least 4 characters."); return; }

        double salary = current.getSalary();
        if (current.isAdmin()) {
            try {
                salary = Double.parseDouble(salF.getText().trim());
                if (salary < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError("Enter a valid positive salary."); return;
            }
        }

        Employee updated = new Employee(
            current.getId(), name, current.getEmail(), pass,
            current.getPhoneNumber(), current.getDepartment(),
            current.getPosition(), salary, current.getRole());
        service.updateEmployee(current.getEmail(), updated);
        app.setCurrentUser(updated);
        welcomeLabel.setText("Welcome, " + name
            + "   [" + updated.getRole().toUpperCase() + "  —  " + updated.getPosition() + "]");
        refreshTable();
    }

    // ── Form / Dialog Helpers ─────────────────────────────────────────────────

    private JPanel buildDialogForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.BG_CARD);
        return form;
    }

    @SuppressWarnings("unchecked")
    private void addFormRow(JPanel panel, String label, JComponent field) {
        JLabel lbl = Theme.fieldLabel(label);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (field instanceof JComboBox) {
            Theme.styleCombo((JComboBox<String>) field);
        } else {
            Theme.styleInput(field);
        }
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(360, 34));

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(3));
        panel.add(field);
        panel.add(Box.createVerticalStrut(10));
    }

    private int showDialog(JPanel form, String title) {
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        JScrollPane sp = new JScrollPane(form);
        sp.setPreferredSize(new Dimension(400, 460));
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        sp.getViewport().setBackground(Theme.BG_CARD);
        return JOptionPane.showConfirmDialog(this, sp, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    private String validateAll(String id, String name, String email,
                                String password, String phone, String salTxt) {
        if (id.isEmpty() || name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || phone.isEmpty() || salTxt.isEmpty())
            return "All fields are required.";
        if (!email.contains("@") || !email.contains("."))
            return "Enter a valid email address.";
        if (password.length() < 4)
            return "Password must be at least 4 characters.";
        if (!phone.matches("\\d{7,15}"))
            return "Phone must be 7–15 digits.";
        try {
            double s = Double.parseDouble(salTxt);
            if (s < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "Enter a valid positive salary.";
        }
        return null;
    }

    // ── Small UI Helpers ──────────────────────────────────────────────────────

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel breakdownLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_LABEL);
        lbl.setForeground(Theme.TEXT_DARK);
        return lbl;
    }

    private String currency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}
