import javax.swing.*;
import java.awt.*;

/**
 * LoginScreen.java
 *
 * FIXES:
 * - FIX 1: Header title is now CENTER-aligned inside the header stripe
 *   (was LEFT-aligned, looked off-balance).
 * - FIX 2: "Sign in" sub-title is LEFT-aligned to match the form fields
 *   below it (was CENTER_ALIGNMENT which fought with BoxLayout).
 * - FIX 3: All field labels use LEFT_ALIGNMENT explicitly so BoxLayout
 *   does not shift them based on preferred-width differences.
 * - FIX 4: Input fields get Theme.styleInput() for consistent border/font.
 * - FIX 5: Card width is capped at 420px max via setMaximumSize so it
 *   does not stretch on wide windows.
 * - FIX 6: Error label is LEFT-aligned so it sits flush with the fields.
 * - FIX 7: Buttons fill full card width (LEFT_ALIGNMENT + MAX_VALUE width)
 *   for visual consistency.
 */
public class LoginScreen extends JPanel {

    private JTextField     emailField;
    private JPasswordField passwordField;
    private JLabel         errorLabel;

    private final Main            app;
    private final EmployeeService service;

    public LoginScreen(Main app, EmployeeService service) {
        this.app     = app;
        this.service = service;
        setBackground(Theme.BG);
        // GridBagLayout centres the card both horizontally and vertically
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        // ── Card: white panel with shadow-like border ──────────────────────────
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.DIVIDER, 1),
            BorderFactory.createEmptyBorder(32, 40, 36, 40)
        ));
        // FIX 5: cap maximum width so it never stretches across the whole frame
        card.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));

        // ── Header stripe ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // FIX 1: title centred inside the stripe
        JLabel titleLbl = new JLabel("Employee Payroll System", SwingConstants.CENTER);
        titleLbl.setFont(Theme.FONT_TITLE.deriveFont(16f));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.CENTER);

        // BoxLayout needs this pair so the header stretches to card width
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        card.add(header);
        card.add(Box.createVerticalStrut(24));

        // ── Sub-title ──────────────────────────────────────────────────────────
        // FIX 2: LEFT_ALIGNMENT so it aligns with the form fields below
        JLabel sub = new JLabel("Sign in to your account");
        sub.setFont(Theme.FONT_LABEL);
        sub.setForeground(Theme.TEXT_MID);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(20));

        // ── Fields ────────────────────────────────────────────────────────────
        emailField    = new JTextField();
        passwordField = new JPasswordField();
        Theme.styleInput(emailField);
        Theme.styleInput(passwordField);

        addField(card, "Email address", emailField);
        addField(card, "Password",      passwordField);

        // ── Error label ───────────────────────────────────────────────────────
        // FIX 6: LEFT alignment — matches field labels
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.FONT_SMALL);
        errorLabel.setForeground(Theme.ERROR);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));

        // ── Buttons ───────────────────────────────────────────────────────────
        // FIX 7: LEFT_ALIGNMENT + MAX_VALUE width so buttons fill the card
        JButton loginBtn = Theme.primaryButton("Login");
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginBtn.addActionListener(e -> handleLogin());
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));

        JButton registerBtn = Theme.outlineButton("Create Account");
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        registerBtn.addActionListener(e -> app.showRegister());
        card.add(registerBtn);

        // ── GridBagConstraints: card is centred in the outer GridBagLayout ────
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill  = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(card, gbc);
    }

    /**
     * Helper: adds a label + field pair with consistent LEFT alignment and spacing.
     * FIX 3: label AlignmentX explicitly set LEFT so BoxLayout never drifts it.
     */
    private void addField(JPanel panel, String labelText, JComponent field) {
        JLabel label = Theme.fieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT); // FIX 3

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        panel.add(Box.createVerticalStrut(14));
    }

    // ── Business logic (unchanged) ────────────────────────────────────────────

    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("All fields are required.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            errorLabel.setText("Enter a valid email address.");
            return;
        }
        if (password.length() < 4) {
            errorLabel.setText("Password must be at least 4 characters.");
            return;
        }

        Employee employee = service.login(email, password);
        if (employee != null) {
            errorLabel.setText(" ");
            app.showDashboard(employee);
        } else {
            errorLabel.setText("Invalid email or password.");
        }
    }

    public void reset() {
        emailField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
    }
}
