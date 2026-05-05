import java.io.*;
import java.util.*;

public class AttendanceService {

    private static final String FILE_PATH = "attendance.txt";

    public List<AttendanceRecord> loadAll() {
        List<AttendanceRecord> list = new ArrayList<>();
        File file = new File(FILE_PATH);
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
            e.printStackTrace();
        }
        return list;
    }

    public void saveAll(List<AttendanceRecord> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH, false))) {
            for (AttendanceRecord r : records) {
                writer.println(r.toFileString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AttendanceRecord findByEmployeeId(String employeeId) {
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
