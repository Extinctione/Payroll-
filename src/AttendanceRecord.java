public class AttendanceRecord {

    private String employeeId;
    private int    daysWorked;
    private double overtimeHours;
    private int    leaveDays;

    public AttendanceRecord(String employeeId, int daysWorked, double overtimeHours, int leaveDays) {
        this.employeeId   = employeeId;
        this.daysWorked   = daysWorked;
        this.overtimeHours = overtimeHours;
        this.leaveDays    = leaveDays;
    }

    public String getEmployeeId()    { return employeeId; }
    public int    getDaysWorked()    { return daysWorked; }
    public double getOvertimeHours() { return overtimeHours; }
    public int    getLeaveDays()     { return leaveDays; }

    /** Format: employeeId,daysWorked,overtimeHours,leaveDays */
    public String toFileString() {
        return employeeId + "," + daysWorked + "," + overtimeHours + "," + leaveDays;
    }

    public static AttendanceRecord fromFileString(String line) {
        String[] p = line.split(",", 4);
        if (p.length != 4) return null;
        try {
            return new AttendanceRecord(
                p[0].trim(),
                Integer.parseInt(p[1].trim()),
                Double.parseDouble(p[2].trim()),
                Integer.parseInt(p[3].trim())
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
