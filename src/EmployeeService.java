import java.io.*;
import java.util.*;

public class EmployeeService {

    private static final String FILE_PATH = "employees.txt";
    public  static final String ADMIN_KEY = "ADMIN123";

    // ── File I/O ───────────────────────────────────────────────────────────────

    public List<Employee> loadEmployees() {
        List<Employee> list = new ArrayList<>();
        File file = new File(FILE_PATH);
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
            e.printStackTrace();
        }
        return list;
    }

    public void saveEmployees(List<Employee> employees) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH, false))) {
            for (Employee e : employees) {
                writer.println(e.toFileString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Lookup ─────────────────────────────────────────────────────────────────

    public Employee findByEmail(String email) {
        for (Employee e : loadEmployees()) {
            if (e.getEmail().equalsIgnoreCase(email)) return e;
        }
        return null;
    }

    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    public boolean register(Employee employee) {
        if (emailExists(employee.getEmail())) return false;
        List<Employee> list = loadEmployees();
        list.add(employee);
        saveEmployees(list);
        return true;
    }

    public Employee login(String email, String password) {
        for (Employee e : loadEmployees()) {
            if (e.getEmail().equalsIgnoreCase(email) && e.getPassword().equals(password)) {
                return e;
            }
        }
        return null;
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

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

    // ── Salary Deductions ──────────────────────────────────────────────────────

    public double calculateTax(double basic)   { return basic * 0.10; }
    public double calculatePF(double basic)    { return basic * 0.05; }

    /** Net salary from basic alone: basic − tax − pf */
    public double calculateNetSalary(double basic) {
        return basic - calculateTax(basic) - calculatePF(basic);
    }

    // ── Attendance-based Salary ────────────────────────────────────────────────

    /**
     * Gross attendance salary:
     *   dailyRate    = basic / 30
     *   overtimeRate = dailyRate / 8
     *   gross        = (daysWorked * dailyRate)
     *                + (overtimeHours * overtimeRate)
     *                - (leaveDays * dailyRate)
     *
     * Final net: gross − tax(basic) − pf(basic)
     */
    public double calculateAttendanceSalary(double basic, int daysWorked,
                                             double overtimeHours, int leaveDays) {
        double dailyRate    = basic / 30.0;
        double overtimeRate = dailyRate / 8.0;
        double gross = (daysWorked * dailyRate)
                     + (overtimeHours * overtimeRate)
                     - (leaveDays * dailyRate);
        return gross - calculateTax(basic) - calculatePF(basic);
    }

    // ── Role Helper ────────────────────────────────────────────────────────────

    public boolean validateAdminKey(String key) {
        return ADMIN_KEY.equals(key);
    }
}
