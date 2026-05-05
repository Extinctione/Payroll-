import javax.swing.*;
import java.awt.*;

/**
 * RegistrationScreen.java
 *
 * FIXES:
 * - FIX 1: Replaced raw BoxLayout-on-screen with a centred card (same
 *   pattern as LoginScreen). The old approach caused left-side cramping
 *   because BoxLayout placed the panel flush to the left edge.
 * - FIX 2: Consistent header stripe (matches LoginScreen) for visual
 *   coherence across screens.
 * - FIX 3: Form content is inside a JScrollPane so it never gets clipped
 *   on small screens or when the admin-key row appears.
 * - FIX 4: All labels use Theme.fieldLabel() for consistent font/colour.
 * - FIX 5: All inputs use Theme.styleInput() / Theme.styleCombo() for
 *   consistent border, font, and background.
 * - FIX 6: Buttons use Theme.primaryButton() / Theme.outlineButton() to
 *   match the Login screen exactly.
 * - FIX 7: setBackground(Theme.BG) on the outer panel so the green
 *   background fills the full card area.
 * - FIX 8: adminKeyRow background set to BG_CARD so it blends in.
 */
public class RegistrationScreen extends JPanel {

    private static final String[] DEPARTMENTS = {"HR", "IT", "Finance"};
    private static final String[] POSITIONS   = {"Developer", "Manager", "Admin"};

    private JTextField     idField, nameField, emailField, phoneField, salaryField;
    private JPasswordField passwordField, adminKeyField;
    private JComboBox<String> deptCombo, posCombo;
    private JLabel         errorLabel;
    private JPanel         adminKeyRow;

    private final Main            app;
    private final EmployeeService service;

    public RegistrationScreen(Main app, EmployeeService service) {
        this.app     = app;
        this.service = service;
        // FIX 7: outer panel fills the frame with the theme background
        setBackground(Theme.BG);
        setLayout(new GridBagLayout()); // centres the card
        buildUI();
    }

    private void buildUI() {
        // ── Card wrapper ──────────────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.DIVIDER, 1),
            BorderFactory.createEmptyBorder(0, 0, 20, 0) // bottom padding inside scroll
        ));
        card.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));

        // FIX 2: header stripe matching LoginScreen
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel titleLbl = new JLabel("Register Employee", SwingConstants.CENTER);
        titleLbl.setFont(Theme.FONT_TITLE.deriveFont(16f));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.CENTER);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        card.add(header);

        // ── Form fields inside horizontal padding ──────────────────────────────
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(20, 36, 8, 36));

        idField       = new JTextField();
        nameField     = new JTextField();
        emailField    = new JTextField();
        passwordField = new JPasswordField();
        phoneField    = new JTextField();
        salaryField   = new JTextField();

        // FIX 4+5: consistent styled labels and inputs
        addField(form, "Employee ID",   idField);
        addField(form, "Full Name",     nameField);
        addField(form, "Email",         emailField);
        addField(form, "Password",      passwordField);
        addField(form, "Phone Number",  phoneField);
        addField(form, "Basic Salary",  salaryField);

        // Department dropdown
        deptCombo = new JComboBox<>(DEPARTMENTS);
        Theme.styleCombo(deptCombo);
        deptCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        deptCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        addLabeledComponent(form, "Department", deptCombo);

        // Position dropdown — reveals admin-key row when "Admin" is selected
        posCombo = new JComboBox<>(POSITIONS);
        Theme.styleCombo(posCombo);
        posCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        posCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        posCombo.addActionListener(e -> toggleAdminKeyRow());
        addLabeledComponent(form, "Position", posCombo);

        // Admin key row (hidden by default)
        adminKeyField = new JPasswordField();
        Theme.styleInput(adminKeyField);
        adminKeyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        adminKeyField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // FIX 8: row background matches the card so it blends seamlessly
        adminKeyRow = new JPanel();
        adminKeyRow.setLayout(new BoxLayout(adminKeyRow, BoxLayout.Y_AXIS));
        adminKeyRow.setBackground(Theme.BG_CARD);
        adminKeyRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel adminKeyLabel = Theme.fieldLabel("Admin Key");
        adminKeyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        adminKeyRow.add(adminKeyLabel);
        adminKeyRow.add(Box.createVerticalStrut(4));
        adminKeyRow.add(adminKeyField);
        adminKeyRow.add(Box.createVerticalStrut(12));
        adminKeyRow.setVisible(false);
        form.add(adminKeyRow);

        // Error label — left aligned, matches field alignment
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setFont(Theme.FONT_SMALL);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(8));

        // FIX 6: themed buttons matching LoginScreen
        JButton registerBtn = Theme.primaryButton("Register");
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        registerBtn.addActionListener(e -> handleRegister());
        form.add(registerBtn);
        form.add(Box.createVerticalStrut(8));

        JButton backBtn = Theme.outlineButton("Back to Login");
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        backBtn.addActionListener(e -> app.showLogin());
        form.add(backBtn);

        card.add(form);

        // FIX 3: form scrollable so it never clips on small screens
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setPreferredSize(new Dimension(460, 520));
        scroll.setBackground(Theme.BG);
        scroll.getViewport().setBackground(Theme.BG_CARD);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 0, 20, 0);
        add(scroll, gbc);
    }

    private void toggleAdminKeyRow() {
        boolean isAdmin = "Admin".equals(posCombo.getSelectedItem());
        adminKeyRow.setVisible(isAdmin);
        revalidate();
        repaint();
    }

    /**
     * Adds a label + styled input pair with consistent LEFT alignment.
     * FIX 4+5 applied here.
     */
    private void addField(JPanel panel, String labelText, JComponent field) {
        JLabel lbl = Theme.fieldLabel(labelText);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        Theme.styleInput(field);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        panel.add(Box.createVerticalStrut(12));
    }

    private void addLabeledComponent(JPanel panel, String labelText, JComponent comp) {
        JLabel lbl = Theme.fieldLabel(labelText);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(comp);
        panel.add(Box.createVerticalStrut(12));
    }

    // ── Business logic (unchanged) ────────────────────────────────────────────

    private void handleRegister() {
        String id       = idField.getText().trim();
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String phone    = phoneField.getText().trim();
        String salTxt   = salaryField.getText().trim();
        String dept     = (String) deptCombo.getSelectedItem();
        String position = (String) posCombo.getSelectedItem();

        String err = validate(id, name, email, password, phone, salTxt);
        if (err != null) { errorLabel.setText(err); return; }

        String role = "employee";
        if ("Admin".equals(position)) {
            String key = new String(adminKeyField.getPassword()).trim();
            if (!service.validateAdminKey(key)) {
                errorLabel.setText("Invalid admin key.");
                return;
            }
            role = "admin";
        }

        double salary = Double.parseDouble(salTxt);
        Employee emp  = new Employee(id, name, email, password, phone, dept, position, salary, role);

        if (service.register(emp)) {
            JOptionPane.showMessageDialog(this,
                "Registration successful! Please login.", "Success",
                JOptionPane.INFORMATION_MESSAGE);
            app.showLogin();
        } else {
            errorLabel.setText("Email is already registered.");
        }
    }

    private String validate(String id, String name, String email,
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

    public void reset() {
        idField.setText("");
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        phoneField.setText("");
        salaryField.setText("");
        adminKeyField.setText("");
        deptCombo.setSelectedIndex(0);
        posCombo.setSelectedIndex(0);
        adminKeyRow.setVisible(false);
        errorLabel.setText(" ");
    }
}
