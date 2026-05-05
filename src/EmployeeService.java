import java.io.*;
import java.util.*;

/**
 * EmployeeService.java
 * Handles all file I/O, authentication, and salary calculations.
 *
 * FILE PATH STRATEGY:
 *   Data files (employees.txt) are resolved in this priority order:
 *   1. Same directory as the running .class file (works for: javac *.java && java Main from src/)
 *   2. "data" sub-folder next to the .class file (VS Code bin/ launch)
 *   3. Current working directory fallback
 *
 *   This ensures employees.txt is always in ONE consistent location
 *   regardless of whether you run from src/, bin/, or via VS Code's debugger.
 *
 *   Storage format per line (9 comma-separated fields):
 *   id,name,email,password,phone,department,position,salary,role
 */
public class EmployeeService {

    static final String DATA_FILE = "employees.txt";
    // Resolved once; shared so AttendanceService can use the same directory.
    static final File DATA_DIR = resolveDataDir();

    public static final String ADMIN_KEY = "ADMIN123";

    /**
     * Resolves the directory where data files are stored.
     * Priority: location of the compiled class → current working directory.
     */
    static File resolveDataDir() {
        try {
            File classLoc = new File(
                EmployeeService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            );
            // If classLoc is a directory (exploded classes), use it directly.
            File dir = classLoc.isDirectory() ? classLoc : classLoc.getParentFile();
            if (dir != null && dir.exists()) return dir;
        } catch (Exception ignored) {}
        // Fallback: current working directory
        return new File(System.getProperty("user.dir"));
    }

    private static File dataFile() {
        return new File(DATA_DIR, DATA_FILE);
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

    public List<Employee> loadEmployees() {
        List<Employee> list = new ArrayList<>();
        File file = dataFile();
        if (!file.exists()) return list;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    Employee e = Employee.fromFileString(line);
                    if (e != null) list.add(e);
                }
            }
        } catch (IOException e) {
            System.err.println("[EmployeeService] Read error: " + e.getMessage());
        }
        return list;
    }

    public void saveEmployees(List<Employee> employees) {
        File file = dataFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            for (Employee e : employees) {
                writer.println(e.toFileString());
            }
        } catch (IOException e) {
            System.err.println("[EmployeeService] Write error: " + e.getMessage());
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    public Employee findByEmail(String email) {
        if (email == null) return null;
        for (Employee e : loadEmployees()) {
            if (e.getEmail().equalsIgnoreCase(email)) return e;
        }
        return null;
    }

    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public boolean register(Employee employee) {
        if (emailExists(employee.getEmail())) return false;
        List<Employee> list = loadEmployees();
        list.add(employee);
        saveEmployees(list);
        return true;
    }

    public Employee login(String email, String password) {
        if (email == null || password == null) return null;
        for (Employee e : loadEmployees()) {
            if (e.getEmail().equalsIgnoreCase(email)
                    && e.getPassword().equals(password)) {
                return e;
            }
        }
        return null;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean updateEmployee(String originalEmail, Employee updated) {
        List<Employee> list = loadEmployees();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getEmail().equalsIgnoreCase(originalEmail)) {
                list.set(i, updated);
                saveEmployees(list);
                return true;
            }
        }
        return false;
    }

    public boolean deleteEmployee(String email) {
        List<Employee> list = loadEmployees();
        boolean removed = list.removeIf(e -> e.getEmail().equalsIgnoreCase(email));
        if (removed) saveEmployees(list);
        return removed;
    }

    // ── Salary Calculations ───────────────────────────────────────────────────

    public double calculateTax(double basic)       { return basic * 0.10; }
    public double calculatePF(double basic)        { return basic * 0.05; }
    public double calculateNetSalary(double basic) { return basic - calculateTax(basic) - calculatePF(basic); }

    public double calculateAttendanceSalary(double basic, int daysWorked,
                                            double overtimeHours, int leaveDays) {
        double dailyRate    = basic / 30.0;
        double overtimeRate = dailyRate / 8.0;
        double gross = (daysWorked * dailyRate)
                     + (overtimeHours * overtimeRate)
                     - (leaveDays * dailyRate);
        return gross - calculateTax(basic) - calculatePF(basic);
    }

    // ── Admin Key ─────────────────────────────────────────────────────────────

    public boolean validateAdminKey(String key) {
        return ADMIN_KEY.equals(key);
    }
}
