import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Theme.java — Central UI theme (earthy green palette).
 *
 * All screens pull colours, fonts, and factory helpers from here.
 *
 * CHANGES vs original:
 * - Added primaryButton(String, ActionListener) convenience overload so
 *   DashboardScreen can create buttons in one line.
 * - Added outlineButton(String, ActionListener) convenience overload.
 * - styleCombo() now also sets preferred/max height for layout consistency.
 * - styleScrollPane() sets viewport background to BG (was BG_CARD) so the
 *   page-level scroll pane matches the outer panel colour.
 */
public class Theme {

    // ── Palette ───────────────────────────────────────────────────────────────
    public static final Color PRIMARY    = new Color(0x2E7D32); // dark green
    public static final Color PRIMARY_HO = new Color(0x1B5E20); // hover / pressed
    public static final Color SECONDARY  = new Color(0xA5D6A7); // light green
    public static final Color BG         = new Color(0xF1F8F4); // outer background
    public static final Color BG_CARD    = Color.WHITE;
    public static final Color TEXT_DARK  = new Color(0x1B2A1C);
    public static final Color TEXT_MID   = new Color(0x4A4A4A);
    public static final Color ERROR      = new Color(0xB71C1C);
    public static final Color DIVIDER    = new Color(0xC8E6C9);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD,  20);
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD,  14);
    public static final Font FONT_LABEL  = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_INPUT  = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BTN    = new Font("SansSerif", Font.BOLD,  13);
    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 12);

    // ── Borders ───────────────────────────────────────────────────────────────
    public static Border panelPadding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        );
    }

    public static Border sectionBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SECONDARY, 1), " " + title + " ",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            FONT_LABEL, PRIMARY
        );
    }

    // ── Button factories ──────────────────────────────────────────────────────

    /** Solid green primary button. */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? PRIMARY_HO : PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        applyButtonBase(btn, Color.WHITE);
        return btn;
    }

    /** Convenience overload: creates a primary button and attaches a listener. */
    public static JButton primaryButton(String text, ActionListener listener) {
        JButton btn = primaryButton(text);
        btn.addActionListener(listener);
        return btn;
    }

    /** Green outline button (transparent fill, green border). */
    public static JButton outlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? SECONDARY : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        applyButtonBase(btn, PRIMARY);
        return btn;
    }

    /** Convenience overload: creates an outline button and attaches a listener. */
    public static JButton outlineButton(String text, ActionListener listener) {
        JButton btn = outlineButton(text);
        btn.addActionListener(listener);
        return btn;
    }

    /** Shared base styling applied to all button variants. */
    private static void applyButtonBase(JButton btn, Color fgColor) {
        btn.setFont(FONT_BTN);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Fixed height; width is determined by the parent layout
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 36));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.repaint(); }
            public void mouseExited(MouseEvent e)  { btn.repaint(); }
        });
    }

    // ── Input field styling ───────────────────────────────────────────────────

    /** Applies consistent border, font, and background to text/password fields. */
    public static void styleInput(JComponent field) {
        field.setFont(FONT_INPUT);
        field.setBackground(BG_CARD);
        field.setForeground(TEXT_DARK);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    /** Applies consistent font, background, and foreground to combo boxes. */
    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(FONT_INPUT);
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_DARK);
        // Ensure height is consistent with styled text fields
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    // ── Table styling ─────────────────────────────────────────────────────────

    /**
     * Applies the full earthy-green theme to a JTable:
     * alternating row colours, styled header, left-aligned cells,
     * and selection using SECONDARY green.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_LABEL);
        table.setRowHeight(30);
        table.setGridColor(DIVIDER);
        table.setBackground(BG_CARD);
        table.setSelectionBackground(SECONDARY);
        table.setSelectionForeground(TEXT_DARK);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADER);
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createEmptyBorder());

        // Alternating row renderer with left-aligned cell content
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (sel) {
                    setBackground(SECONDARY);
                    setForeground(TEXT_DARK);
                } else {
                    setBackground(row % 2 == 0 ? BG_CARD : new Color(0xF6FBF7));
                    setForeground(TEXT_DARK);
                }
                // Left-align with consistent internal padding
                setHorizontalAlignment(LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
    }

    /**
     * Applies the earthy-green border and scroll increments to a JScrollPane.
     * Viewport background is set to BG so page-level scroll panes blend in.
     */
    public static void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        sp.getVerticalScrollBar().setUnitIncrement(14);
        // BG for outer scrolls; callers can override for table scrolls
        sp.getViewport().setBackground(BG);
    }

    // ── Panel helpers ─────────────────────────────────────────────────────────

    public static JPanel bgPanel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(BG);
        return p;
    }

    public static JLabel titleLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(PRIMARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    public static JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MID);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
}
