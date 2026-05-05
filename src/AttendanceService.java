import java.io.*;
import java.util.*;

/**
 * AttendanceService.java
 * Handles reading and writing attendance records to attendance.txt.
 *
 * FILE PATH: Uses the same DATA_DIR as EmployeeService so both files
 * always live in the same directory — no path split between src/ and bin/.
 *
 * Storage format per line (4 comma-separated fields):
 *   employeeId,daysWorked,overtimeHours,leaveDays
 */
public class AttendanceService {

    private static final String ATT_FILE = "attendance.txt";

    private static File dataFile() {
        // Reuse the same directory resolution as EmployeeService for consistency
        return new File(EmployeeService.DATA_DIR, ATT_FILE);
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

    public List<AttendanceRecord> loadAll() {
        List<AttendanceRecord> list = new ArrayList<>();
        File file = dataFile();
        if (!file.exists()) return list;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    AttendanceRecord r = AttendanceRecord.fromFileString(line);
                    if (r != null) list.add(r);
                }
            }
        } catch (IOException e) {
            System.err.println("[AttendanceService] Read error: " + e.getMessage());
        }
        return list;
    }

    public void saveAll(List<AttendanceRecord> records) {
        File file = dataFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            for (AttendanceRecord r : records) {
                writer.println(r.toFileString());
            }
        } catch (IOException e) {
            System.err.println("[AttendanceService] Write error: " + e.getMessage());
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    public AttendanceRecord findByEmployeeId(String employeeId) {
        if (employeeId == null) return null;
        for (AttendanceRecord r : loadAll()) {
            if (r.getEmployeeId().equalsIgnoreCase(employeeId)) return r;
        }
        return null;
    }

    public void saveOrUpdate(AttendanceRecord record) {
        List<AttendanceRecord> list = loadAll();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getEmployeeId().equalsIgnoreCase(record.getEmployeeId())) {
                list.set(i, record);
                saveAll(list);
                return;
            }
        }
        list.add(record);
        saveAll(list);
    }
}
