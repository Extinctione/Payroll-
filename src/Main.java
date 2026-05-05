import javax.swing.*;
import java.awt.*;

/**
 * Main.java — Application entry point and screen router.
 *
 * Sets up the Look-and-Feel, global UIManager overrides,
 * builds the shared CardLayout frame, and routes between screens.
 *
 * Run from the src/ directory:
 *   cd src
 *   javac *.java
 *   java Main
 */
public class Main {

    public static final String LOGIN     = "LOGIN";
    public static final String REGISTER  = "REGISTER";
    public static final String DASHBOARD = "DASHBOARD";

    private final JFrame frame;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    private Employee currentUser;

    private final LoginScreen        loginScreen;
    private final RegistrationScreen registrationScreen;
    private final DashboardScreen    dashboardScreen;

    public Main() {
        // Cross-platform L&F so theme colours render identically on all OSes
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global UIManager defaults — applied before any component is built
        UIManager.put("Panel.background",              Theme.BG);
        UIManager.put("OptionPane.background",         Theme.BG);
        UIManager.put("OptionPane.messageForeground",  Theme.TEXT_DARK);
        UIManager.put("Button.background",             Theme.PRIMARY);
        UIManager.put("Button.foreground",             Color.WHITE);
        UIManager.put("Button.font",                   Theme.FONT_BTN);
        UIManager.put("Label.foreground",              Theme.TEXT_DARK);
        UIManager.put("Label.font",                    Theme.FONT_LABEL);
        UIManager.put("TextField.background",          Theme.BG_CARD);
        UIManager.put("TextField.foreground",          Theme.TEXT_DARK);
        UIManager.put("TextField.font",                Theme.FONT_INPUT);
        UIManager.put("PasswordField.background",      Theme.BG_CARD);
        UIManager.put("PasswordField.foreground",      Theme.TEXT_DARK);
        UIManager.put("PasswordField.font",            Theme.FONT_INPUT);
        UIManager.put("ComboBox.background",           Theme.BG_CARD);
        UIManager.put("ComboBox.foreground",           Theme.TEXT_DARK);
        UIManager.put("ComboBox.font",                 Theme.FONT_INPUT);
        UIManager.put("ScrollPane.background",         Theme.BG);
        UIManager.put("Viewport.background",           Theme.BG_CARD);
        UIManager.put("Table.background",              Theme.BG_CARD);
        UIManager.put("Table.foreground",              Theme.TEXT_DARK);
        UIManager.put("TableHeader.background",        Theme.PRIMARY);
        UIManager.put("TableHeader.foreground",        Color.WHITE);
        UIManager.put("TableHeader.font",              Theme.FONT_HEADER);

        // Build the shared service layer first
        EmployeeService service = new EmployeeService();

        // Print resolved data path so users can confirm location on startup
        System.out.println("[Main] Data directory: " + EmployeeService.DATA_DIR.getAbsolutePath());

        // Build screens (preferred sizes are known before frame creation)
        loginScreen        = new LoginScreen(this, service);
        registrationScreen = new RegistrationScreen(this, service);
        dashboardScreen    = new DashboardScreen(this, service);

        // Card container
        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setBackground(Theme.BG);
        mainPanel.add(loginScreen,        LOGIN);
        mainPanel.add(registrationScreen, REGISTER);
        mainPanel.add(dashboardScreen,    DASHBOARD);

        // Frame
        frame = new JFrame("Employee Payroll System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setSize(960, 680);
        frame.setMinimumSize(new Dimension(800, 580));
        frame.setLocationRelativeTo(null);  // centred after size is known
        frame.setVisible(true);
    }

    // ── Screen switching ───────────────────────────────────────────────────────

    public void showScreen(String name) {
        cardLayout.show(mainPanel, name);
    }

    public void showLogin() {
        currentUser = null;
        loginScreen.reset();
        showScreen(LOGIN);
    }

    public void showRegister() {
        registrationScreen.reset();
        showScreen(REGISTER);
    }

    public void showDashboard(Employee employee) {
        currentUser = employee;
        dashboardScreen.init(employee);
        showScreen(DASHBOARD);
    }

    public void logout() { showLogin(); }

    public Employee getCurrentUser()         { return currentUser; }
    public void setCurrentUser(Employee emp) { currentUser = emp; }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
