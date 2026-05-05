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
 * DashboardScreen.java
 *
 * FIXES:
 * - FIX 1: ENTIRE dashboard content (search + table + breakdown + buttons)
 *   is now wrapped in a single JScrollPane so the page scrolls as a whole.
 *   Previously only the table had a scroll pane; the breakdown and buttons
 *   were cut off on small windows.
 * - FIX 2: Layout changed from BorderLayout to BoxLayout(Y_AXIS) on the
 *   main content panel so sections stack cleanly and scroll together.
 * - FIX 3: Top bar uses BorderLayout inside a fixed-height wrapper so it
 *   stays at the top and is NOT included in the scroll area (it acts as a
 *   fixed header — more natural UX).
 * - FIX 4: Table is given a preferred height so it shows ~8 rows before
 *   the scroll kicks in, rather than collapsing to near-zero.
 * - FIX 5: Breakdown panel uses a cleaner GridLayout(2,4) with uniform
 *   label sizing and left-alignment so numbers never overflow their cells.
 * - FIX 6: All action buttons use Theme.primaryButton() / outlineButton()
 *   for visual consistency with the Login/Register screens.
 * - FIX 7: Consistent vertical struts (Box.createVerticalStrut) between
 *   sections replace ad-hoc BorderLayout gaps.
 * - FIX 8: Theme.styleTable() applied to the employee table for alternating
 *   row colours, styled header, and proper selection colour.
 * - FIX 9: Theme.styleScrollPane() applied to both scroll panes.
 * - FIX 10: setBackground(Theme.BG) throughout so no white "flash" panels
 *   appear when the window is resized.
 * - FIX 11: Welcome label uses Theme.FONT_HEADER and TEXT_DARK colour so
 *   it is legible on the BG background.
 * - FIX 12: Dialog form rows use Theme.styleInput() for consistent styling.
 */
public class DashboardScreen extends JPanel {

    private static final String[] DEPARTMENTS = {"HR", "IT", "Finance"};
    private static final String[] POSITIONS   = {"Developer", "Manager", "Admin"};

    private final Main              app;
    private final EmployeeService   service;
    private final AttendanceService attendanceService;

    // Top bar (fixed — outside scroll)
    private JLabel welcomeLabel;

    // Scrollable content
    private JTextField        searchField;
    private JPanel            searchPanel, crudPanel;
    private DefaultTableModel tableModel;
    private JTable            employeeTable;

    // Breakdown labels
    private JLabel basicLabel, daysLabel, overtimeLabel, leaveLabel,
                   taxLabel, pfLabel, netLabel;

    private List<Employee> allEmployees = new ArrayList<>();

    public DashboardScreen(Main app, EmployeeService service) {
        this.app               = app;
        this.service           = service;
        this.attendanceService = new AttendanceService();
        // FIX 10: ensure background is set on the root panel
        setBackground(Theme.BG);
        // FIX 3: BorderLayout so the fixed top bar sits above the scroll area
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        buildUI();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        // FIX 3: fixed top bar (not scrolled)
        add(buildTopBar(), BorderLayout.NORTH);

        // FIX 1: single scroll pane wrapping ALL content below the top bar
        JPanel content = buildScrollableContent();
        JScrollPane pageScroll = new JScrollPane(content);
        Theme.styleScrollPane(pageScroll);
        pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);
        pageScroll.getViewport().setBackground(Theme.BG);
        add(pageScroll, BorderLayout.CENTER);
    }

    // ── Top Bar (fixed, not scrolled) ─────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Theme.BG);
        // FIX 11: styled welcome label
        welcomeLabel = new JLabel("Welcome");
        welcomeLabel.setFont(Theme.FONT_HEADER);
        welcomeLabel.setForeground(Theme.TEXT_DARK);
        panel.add(welcomeLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(Theme.BG);
        // FIX 6: themed buttons
        right.add(Theme.outlineButton("Edit Profile", e -> showEditProfileDialog()));
        right.add(Theme.primaryButton("Logout",       e -> app.logout()));
        panel.add(right, BorderLayout.EAST);

        // Divider line below the top bar
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
            BorderFactory.createEmptyBorder(6, 0, 8, 0)
        ));
        return panel;
    }

    // ── Scrollable content (search + table + breakdown + buttons) ─────────────

    /**
     * FIX 1+2: All content below the top bar lives here. BoxLayout Y_AXIS
     * stacks sections vertically. The outer JScrollPane (added in buildUI)
     * scrolls this entire panel.
     */
    private JPanel buildScrollableContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Search bar
        searchPanel = buildSearchBar();
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(searchPanel);
        content.add(Box.createVerticalStrut(8));

        // FIX 4: table with a defined preferred height so it is usable
        JScrollPane tableScroll = buildTableScrollPane();
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 220));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        content.add(tableScroll);

        // FIX 7: consistent gap between sections
        content.add(Box.createVerticalStrut(12));

        // FIX 5: breakdown panel
        JPanel breakdown = buildBreakdownPanel();
        breakdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(breakdown);
        content.add(Box.createVerticalStrut(12));

        // Button row
        JPanel buttons = buildButtonRow();
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(buttons);

        return content;
    }

    // ── Search bar ────────────────────────────────────────────────────────────

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(Theme.BG);

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
            public boolean isCellEditable(int r, int c) { return false; }
        };

        employeeTable = new JTable(tableModel);
        // FIX 8: apply full Theme table styling (alternating rows, styled header)
        Theme.styleTable(employeeTable);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.getSelectionModel().addListSelectionListener(
            e -> { if (!e.getValueIsAdjusting()) showSalaryBreakdown(); }
        );

        // Preferred column widths
        int[] widths = {60, 130, 170, 110, 70, 90, 110, 70};
        for (int i = 0; i < widths.length; i++) {
            employeeTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane sp = new JScrollPane(employeeTable);
        // FIX 9: apply theme scroll pane styling
        Theme.styleScrollPane(sp);
        return sp;
    }

    // ── Breakdown Panel ───────────────────────────────────────────────────────

    /**
     * FIX 5: 2×4 grid with left-aligned labels so values never overflow.
     * Uses Theme.sectionBorder for consistent titled border styling.
     */
    private JPanel buildBreakdownPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 12, 8));
        panel.setBackground(Theme.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            Theme.sectionBorder("Payroll Breakdown (selected employee)"),
            BorderFactory.createEmptyBorder(6, 8, 8, 8)
        ));

        basicLabel    = breakdownLabel("Basic Salary: —");
        daysLabel     = breakdownLabel("Days Worked: —");
        overtimeLabel = breakdownLabel("Overtime Hrs: —");
        leaveLabel    = breakdownLabel("Leave Days: —");
        taxLabel      = breakdownLabel("Tax (10%): —");
        pfLabel       = breakdownLabel("PF (5%): —");
        netLabel      = breakdownLabel("Net Salary: —");

        panel.add(basicLabel);
        panel.add(daysLabel);
        panel.add(overtimeLabel);
        panel.add(leaveLabel);
        panel.add(taxLabel);
        panel.add(pfLabel);
        panel.add(netLabel);
        panel.add(new JLabel()); // spacer to complete the 2×4 grid
        return panel;
    }

    // ── Button Row ────────────────────────────────────────────────────────────

    /**
     * Admin CRUD buttons (left) + Export buttons (right).
     * FIX 6: all buttons use Theme factories for consistent look.
     */
    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Theme.BG);

        // CRUD — admin only; visibility toggled in init()
        crudPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        crudPanel.setBackground(Theme.BG);
        crudPanel.add(Theme.primaryButton("Add Employee",    e -> showAddDialog()));
        crudPanel.add(Theme.outlineButton("Update Employee", e -> showUpdateDialog()));
        crudPanel.add(Theme.outlineButton("Delete Employee", e -> handleDelete()));

        // Export — always visible
        JPanel exportPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        exportPanel.setBackground(Theme.BG);
        exportPanel.add(Theme.outlineButton("Export CSV", e -> exportCSV()));
        exportPanel.add(Theme.outlineButton("Export TXT", e -> exportTXT()));

        row.add(crudPanel,   BorderLayout.CENTER);
        row.add(exportPanel, BorderLayout.EAST);
        return row;
    }

    // ── Theme helper overloads accepting ActionListener directly ──────────────
    // (Keeps button-creation calls concise in this file.)

    // ── Init ──────────────────────────────────────────────────────────────────

    public void init(Employee user) {
        boolean isAdmin = user.isAdmin();
        welcomeLabel.setText("Welcome, " + user.getName()
            + "  [" + user.getRole().toUpperCase() + " — " + user.getPosition() + "]");
        searchField.setText("");
        searchPanel.setVisible(isAdmin);
        crudPanel.setVisible(isAdmin);
        refreshTable();
        if (!isAdmin) selectRowByEmail(user.getEmail());
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private void refreshTable() {
        Employee user = app.getCurrentUser();
        allEmployees  = service.loadEmployees();

        if (user != null && !user.isAdmin()) {
            List<Employee> self = new ArrayList<>();
            for (Employee e : allEmployees) {
                if (e.getEmail().equalsIgnoreCase(user.getEmail())) {
                    self.add(e);
                    break;
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
                    e.getEmail().toLowerCase().contains(query)) {
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

        double net = service.calculateAttendanceSalary(basic, days, overtime, leave);

        basicLabel.setText("Basic Salary: "   + currency(basic));
        daysLabel.setText( "Days Worked: "    + days);
        overtimeLabel.setText("Overtime Hrs: " + overtime);
        leaveLabel.setText("Leave Days: "     + leave);
        taxLabel.setText(  "Tax (10%): "      + currency(tax));
        pfLabel.setText(   "PF (5%): "        + currency(pf));
        netLabel.setText(  "Net Salary: "     + currency(net));
    }

    private void clearBreakdown() {
        basicLabel.setText("Basic Salary: —");
        daysLabel.setText( "Days Worked: —");
        overtimeLabel.setText("Overtime Hrs: —");
        leaveLabel.setText("Leave Days: —");
        taxLabel.setText(  "Tax (10%): —");
        pfLabel.setText(   "PF (5%): —");
        netLabel.setText(  "Net Salary: —");
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private List<Employee> exportTargetList() {
        Employee user = app.getCurrentUser();
        if (user != null && !user.isAdmin()) {
            List<Employee> self = new ArrayList<>();
            for (Employee e : service.loadEmployees()) {
                if (e.getEmail().equalsIgnoreCase(user.getEmail())) {
                    self.add(e);
                    break;
                }
            }
            return self;
        }
        return service.loadEmployees();
    }

    private void exportCSV() {
        String path = "payroll_report.csv";
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
                "CSV exported → " + new File(path).getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Failed to export CSV: " + ex.getMessage());
        }
    }

    private void exportTXT() {
        String path = "payroll_report.txt";
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
                pw.printf("  Days Worked    : %d%n",  days);
                pw.printf("  Overtime Hours : %.1f%n", ot);
                pw.printf("  Leave Days     : %d%n",  leave);
                pw.println("  ── Salary ──────────────────────────────────────────");
                pw.printf("  Basic Salary   : $%,.2f%n", basic);
                pw.printf("  Tax (10%%)      : $%,.2f%n", tax);
                pw.printf("  PF  (5%%)       : $%,.2f%n", pf);
                pw.printf("  NET SALARY     : $%,.2f%n", net);
                pw.println();
            }
            pw.println("==========================================================");
            JOptionPane.showMessageDialog(this,
                "TXT exported → " + new File(path).getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Failed to export TXT: " + ex.getMessage());
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

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.BG_CARD);
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
        if (row < 0) { showError("Select an employee to update."); return; }

        String origEmail = (String) tableModel.getValueAt(row, 2);
        Employee ex      = service.findByEmail(origEmail);
        if (ex == null) return;

        AttendanceRecord existingRec = attendanceService.findByEmployeeId(ex.getId());
        int    curDays = (existingRec != null) ? existingRec.getDaysWorked()    : 30;
        double curOt   = (existingRec != null) ? existingRec.getOvertimeHours() : 0;
        int    curLv   = (existingRec != null) ? existingRec.getLeaveDays()     : 0;

        JTextField     nameF  = new JTextField(ex.getName());
        JTextField     emailF = new JTextField(ex.getEmail());
        JPasswordField passF  = new JPasswordField(ex.getPassword());
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

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.BG_CARD);
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
        if (row < 0) { showError("Select an employee to delete."); return; }

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
        JPasswordField passF = new JPasswordField(current.getPassword());
        JTextField     salF  = new JTextField(String.valueOf(current.getSalary()));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.BG_CARD);
        addFormRow(form, "Full Name", nameF);
        addFormRow(form, "Password",  passF);
        if (current.isAdmin()) addFormRow(form, "Basic Salary", salF);

        if (showDialog(form, "Edit Profile") != JOptionPane.OK_OPTION) return;

        String name = nameF.getText().trim();
        String pass = new String(passF.getPassword()).trim();

        if (name.isEmpty() || pass.isEmpty()) { showError("Name and password are required."); return; }
        if (pass.length() < 4) { showError("Password must be at least 4 characters."); return; }

        double salary = current.getSalary();
        if (current.isAdmin()) {
            try {
                salary = Double.parseDouble(salF.getText().trim());
                if (salary < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError("Enter a valid positive salary."); return;
            }
        }

        Employee updated = new Employee(current.getId(), name, current.getEmail(), pass,
            current.getPhoneNumber(), current.getDepartment(), current.getPosition(),
            salary, current.getRole());
        service.updateEmployee(current.getEmail(), updated);
        app.setCurrentUser(updated);
        welcomeLabel.setText("Welcome, " + name
            + "  [" + updated.getRole().toUpperCase() + " — " + updated.getPosition() + "]");
        refreshTable();
    }

    // ── Form / Dialog helpers ─────────────────────────────────────────────────

    /**
     * Adds a label + field row to a dialog form.
     * FIX 12: uses Theme.styleInput() / styleCombo() for consistent appearance.
     */
    private void addFormRow(JPanel panel, String label, JComponent field) {
        JLabel lbl = Theme.fieldLabel(label);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Style the field appropriately based on its type
        if (field instanceof JComboBox) {
            Theme.styleCombo((JComboBox<?>) field);
        } else {
            Theme.styleInput(field);
        }
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(340, 32));

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(3));
        panel.add(field);
        panel.add(Box.createVerticalStrut(10));
    }

    private int showDialog(JPanel form, String title) {
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        JScrollPane sp = new JScrollPane(form);
        sp.setPreferredSize(new Dimension(380, 440));
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

    // ── Small UI helpers ──────────────────────────────────────────────────────

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Breakdown label: left-aligned, fixed theme font. */
    private JLabel breakdownLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_LABEL);
        lbl.setForeground(Theme.TEXT_DARK);
        // FIX 5: LEFT alignment so long currency strings don't overflow centre
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
        return lbl;
    }

    private String currency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}
