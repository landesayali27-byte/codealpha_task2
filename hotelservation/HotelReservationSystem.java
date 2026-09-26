import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

// ========================================================
// 1. DOMAIN MODELS & OOP CLASSES
// ========================================================

enum RoomCategory {
    STANDARD("Standard", 75.0),
    DELUXE("Deluxe", 130.0),
    SUITE("Executive Suite", 220.0);

    private final String displayName;
    private final double pricePerNight;

    RoomCategory(String displayName, double pricePerNight) {
        this.displayName = displayName;
        this.pricePerNight = pricePerNight;
    }

    public String getDisplayName() { return displayName; }
    public double getPricePerNight() { return pricePerNight; }
}

class Room {
    private final int roomNumber;
    private final RoomCategory category;
    private boolean isAvailable;

    public Room(int roomNumber, RoomCategory category, boolean isAvailable) {
        this.roomNumber = roomNumber;
        this.category = category;
        this.isAvailable = isAvailable;
    }

    public int getRoomNumber() { return roomNumber; }
    public RoomCategory getCategory() { return category; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
}

class Reservation {
    private final String bookingId;
    private final String guestName;
    private final int roomNumber;
    private final RoomCategory category;
    private final int nights;
    private final double totalAmount;
    private final String bookingDate;
    private String paymentStatus;

    public Reservation(String bookingId, String guestName, int roomNumber, RoomCategory category, int nights, double totalAmount, String bookingDate, String paymentStatus) {
        this.bookingId = bookingId;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.category = category;
        this.nights = nights;
        this.totalAmount = totalAmount;
        this.bookingDate = bookingDate;
        this.paymentStatus = paymentStatus;
    }

    public String getBookingId() { return bookingId; }
    public String getGuestName() { return guestName; }
    public int getRoomNumber() { return roomNumber; }
    public RoomCategory getCategory() { return category; }
    public int getNights() { return nights; }
    public double getTotalAmount() { return totalAmount; }
    public String getBookingDate() { return bookingDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String toCsvRow() {
        return String.join(",",
                bookingId,
                guestName,
                String.valueOf(roomNumber),
                category.name(),
                String.valueOf(nights),
                String.valueOf(totalAmount),
                bookingDate,
                paymentStatus
        );
    }

    public static Reservation fromCsvRow(String line) {
        String[] parts = line.split(",");
        if (parts.length < 8) return null;
        return new Reservation(
                parts[0],
                parts[1],
                Integer.parseInt(parts[2]),
                RoomCategory.valueOf(parts[3]),
                Integer.parseInt(parts[4]),
                Double.parseDouble(parts[5]),
                parts[6],
                parts[7]
        );
    }
}

// ========================================================
// 2. MAIN APPLICATION GUI
// ========================================================

public class HotelReservationSystem extends JFrame {

    // Palette Colors
    private static final Color BG_DARK = new Color(20, 22, 31);
    private static final Color CARD_BG = new Color(29, 33, 47);
    private static final Color TABLE_BG = new Color(24, 27, 39);
    private static final Color ACCENT_BLUE = new Color(79, 110, 247);
    private static final Color COLOR_GREEN = new Color(52, 211, 153);
    private static final Color COLOR_RED = new Color(248, 113, 113);
    private static final Color TEXT_WHITE = new Color(240, 243, 250);
    private static final Color TEXT_MUTED = new Color(155, 162, 180);
    private static final Color BORDER_COLOR = new Color(45, 52, 74);

    private static final String RESERVATIONS_FILE = "hotel_reservations.csv";
    private final DecimalFormat moneyFmt = new DecimalFormat("$#,##0.00");

    // In-Memory Storage
    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private final List<Reservation> reservations = new ArrayList<>();

    // Dashboard Cards
    private JLabel totalRoomsBadge;
    private JLabel availableRoomsBadge;
    private JLabel bookedRoomsBadge;
    private JLabel totalRevenueBadge;

    // Room Browser Components
    private JComboBox<String> categoryFilterBox;
    private DefaultTableModel roomTableModel;
    private JTable roomTable;

    // Active Reservations Components
    private DefaultTableModel bookingTableModel;
    private JTable bookingTable;

    // Booking Inputs
    private JTextField guestNameField;
    private JSpinner nightsSpinner;

    public HotelReservationSystem() {
        setTitle("Grand Horizon - Hotel Management & Reservation Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(15, 15));

        seedRooms();
        loadReservationsFromFile();

        // 1. Header KPIs
        add(createMetricCardsPanel(), BorderLayout.NORTH);

        // 2. Split Workspace: Left (Browse Rooms & Book), Right (Active Reservations)
        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 18, 0));
        contentGrid.setBackground(BG_DARK);
        contentGrid.setBorder(new EmptyBorder(0, 20, 15, 20));

        contentGrid.add(createRoomBrowsingPanel());
        contentGrid.add(createReservationsPanel());
        add(contentGrid, BorderLayout.CENTER);

        refreshAllViews();
    }

    private void seedRooms() {
        // Standard rooms
        rooms.put(101, new Room(101, RoomCategory.STANDARD, true));
        rooms.put(102, new Room(102, RoomCategory.STANDARD, true));
        rooms.put(103, new Room(103, RoomCategory.STANDARD, true));

        // Deluxe rooms
        rooms.put(201, new Room(201, RoomCategory.DELUXE, true));
        rooms.put(202, new Room(202, RoomCategory.DELUXE, true));
        rooms.put(203, new Room(203, RoomCategory.DELUXE, true));

        // Suite rooms
        rooms.put(301, new Room(301, RoomCategory.SUITE, true));
        rooms.put(302, new Room(302, RoomCategory.SUITE, true));
    }

    // ========================================================
    // METRICS PANEL
    // ========================================================
    private JPanel createMetricCardsPanel() {
        JPanel header = new JPanel(new GridLayout(1, 4, 12, 0));
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(20, 20, 5, 20));

        totalRoomsBadge = createCard("TOTAL ROOMS", "0");
        availableRoomsBadge = createCard("AVAILABLE VACANCIES", "0");
        bookedRoomsBadge = createCard("OCCUPIED ROOMS", "0");
        totalRevenueBadge = createCard("TOTAL EARNED REVENUE", "$0.00");

        header.add(totalRoomsBadge);
        header.add(availableRoomsBadge);
        header.add(bookedRoomsBadge);
        header.add(totalRevenueBadge);
        return header;
    }

    private JLabel createCard(String title, String val) {
        JLabel card = new JLabel(formatHtml(title, val, "#F0F3FA"), SwingConstants.CENTER);
        card.setOpaque(true);
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(12, 6, 12, 6)
        ));
        return card;
    }

    private String formatHtml(String title, String val, String valColor) {
        return "<html><center><span style='font-size:9px; color:#9BA2B4; letter-spacing:1px;'>" + title + "</span><br>"
                + "<span style='font-size:17px; font-weight:bold; color:" + valColor + ";'>" + val + "</span></center></html>";
    }

    // ========================================================
    // LEFT PANEL: ROOM BROWSING & BOOKING
    // ========================================================
    private JPanel createRoomBrowsingPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Top Filter Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel title = new JLabel("Search & Filter Rooms");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT_WHITE);

        categoryFilterBox = new JComboBox<>(new String[]{"All Categories", "Standard", "Deluxe", "Executive Suite"});
        categoryFilterBox.setBackground(TABLE_BG);
        categoryFilterBox.setForeground(TEXT_WHITE);
        categoryFilterBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        categoryFilterBox.addActionListener(e -> refreshRoomTable());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(categoryFilterBox, BorderLayout.EAST);

        // Room Table
        String[] cols = {"Room No.", "Category", "Rate / Night", "Status"};
        roomTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        roomTable = styleTable(new JTable(roomTableModel));

        JScrollPane scroll = new JScrollPane(roomTable);
        scroll.getViewport().setBackground(TABLE_BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));

        // Bottom Booking Form
        JPanel bookingForm = new JPanel(new GridLayout(3, 2, 10, 8));
        bookingForm.setOpaque(false);
        bookingForm.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel guestLbl = new JLabel("Guest Full Name:");
        guestLbl.setForeground(TEXT_MUTED);
        guestNameField = new JTextField();
        guestNameField.setBackground(new Color(18, 20, 28));
        guestNameField.setForeground(TEXT_WHITE);
        guestNameField.setCaretColor(TEXT_WHITE);
        guestNameField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));

        JLabel nightsLbl = new JLabel("Number of Nights:");
        nightsLbl.setForeground(TEXT_MUTED);
        nightsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
        nightsSpinner.setBackground(new Color(18, 20, 28));

        JButton bookBtn = new JButton("Proceed to Payment & Reserve");
        styleButton(bookBtn, ACCENT_BLUE, TEXT_WHITE);
        bookBtn.addActionListener(e -> handleBooking());

        bookingForm.add(guestLbl);
        bookingForm.add(guestNameField);
        bookingForm.add(nightsLbl);
        bookingForm.add(nightsSpinner);
        bookingForm.add(new JLabel("")); // spacer
        bookingForm.add(bookBtn);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bookingForm, BorderLayout.SOUTH);

        return panel;
    }

    // ========================================================
    // RIGHT PANEL: ACTIVE RESERVATIONS & CANCELLATION
    // ========================================================
    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel("Active Reservations & Folios");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT_WHITE);

        String[] cols = {"Booking ID", "Guest", "Room", "Paid Total", "Status"};
        bookingTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookingTable = styleTable(new JTable(bookingTableModel));

        JScrollPane scroll = new JScrollPane(bookingTable);
        scroll.getViewport().setBackground(TABLE_BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));

        // Bottom Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        JButton viewBtn = new JButton("View Receipt / Folio");
        styleButton(viewBtn, new Color(48, 56, 82), TEXT_WHITE);
        viewBtn.addActionListener(e -> handleViewDetails());

        JButton cancelBtn = new JButton("Cancel Reservation");
        styleButton(cancelBtn, COLOR_RED, TEXT_WHITE);
        cancelBtn.addActionListener(e -> handleCancellation());

        actionRow.add(viewBtn);
        actionRow.add(cancelBtn);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(actionRow, BorderLayout.SOUTH);

        return panel;
    }

    private JTable styleTable(JTable table) {
        table.setBackground(TABLE_BG);
        table.setForeground(TEXT_WHITE);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(45, 54, 80));
        table.setSelectionForeground(TEXT_WHITE);

        table.getTableHeader().setBackground(new Color(34, 38, 54));
        table.getTableHeader().setForeground(TEXT_WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, renderer);
        return table;
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(190, 36));
    }

    // ========================================================
    // BOOKING, PAYMENT, & CANCELLATION LOGIC
    // ========================================================
    private void handleBooking() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an available room from the list.", "Room Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomNum = (int) roomTableModel.getValueAt(selectedRow, 0);
        Room room = rooms.get(roomNum);

        if (!room.isAvailable()) {
            JOptionPane.showMessageDialog(this, "Room " + roomNum + " is already occupied! Select an available room.", "Room Occupied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String guestName = guestNameField.getText().trim();
        if (guestName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the guest's name.", "Name Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int nights = (int) nightsSpinner.getValue();
        double cost = room.getCategory().getPricePerNight() * nights;

        // Payment Simulation Dialog
        String[] options = {"Credit/Debit Card", "UPI / Net Banking", "Cancel"};
        int paymentChoice = JOptionPane.showOptionDialog(
                this,
                "Simulated Payment Gateway\n" +
                "Guest: " + guestName + "\n" +
                "Room: " + room.getRoomNumber() + " (" + room.getCategory().getDisplayName() + ")\n" +
                "Duration: " + nights + " nights @ " + moneyFmt.format(room.getCategory().getPricePerNight()) + "/night\n" +
                "Total Amount Due: " + moneyFmt.format(cost),
                "Secure Payment Simulator",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (paymentChoice == 0 || paymentChoice == 1) {
            // Payment approved
            String bookingId = "BK-" + (1000 + reservations.size() + 1);
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String method = paymentChoice == 0 ? "PAID (Card)" : "PAID (UPI)";

            Reservation res = new Reservation(bookingId, guestName, room.getRoomNumber(), room.getCategory(), nights, cost, date, method);
            reservations.add(res);
            room.setAvailable(false);

            guestNameField.setText("");
            nightsSpinner.setValue(1);

            saveReservationsToFile();
            refreshAllViews();

            JOptionPane.showMessageDialog(this, "Payment Successful! Reservation confirmed.\nBooking Reference: " + bookingId, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleCancellation() {
        int selectedRow = bookingTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a reservation from the table to cancel.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookingId = (String) bookingTableModel.getValueAt(selectedRow, 0);
        Reservation target = null;
        for (Reservation r : reservations) {
            if (r.getBookingId().equals(bookingId)) {
                target = r;
                break;
            }
        }

        if (target == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cancel reservation " + bookingId + " for " + target.getGuestName() + "?\n" +
                "A refund of " + moneyFmt.format(target.getTotalAmount()) + " will be issued to the original payment method.",
                "Confirm Cancellation & Refund",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            Room room = rooms.get(target.getRoomNumber());
            if (room != null) {
                room.setAvailable(true);
            }
            reservations.remove(target);

            saveReservationsToFile();
            refreshAllViews();

            JOptionPane.showMessageDialog(this, "Reservation cancelled and Room " + target.getRoomNumber() + " is now available again.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleViewDetails() {
        int selectedRow = bookingTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a reservation to view receipt details.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookingId = (String) bookingTableModel.getValueAt(selectedRow, 0);
        for (Reservation r : reservations) {
            if (r.getBookingId().equals(bookingId)) {
                String receipt = String.format(
                        "=========================================\n" +
                        "        HOTEL RESERVATION FOLIO          \n" +
                        "=========================================\n" +
                        "Booking Reference : %s\n" +
                        "Date Booked       : %s\n" +
                        "Guest Name        : %s\n" +
                        "-----------------------------------------\n" +
                        "Assigned Room     : %d\n" +
                        "Room Category     : %s\n" +
                        "Length of Stay    : %d nights\n" +
                        "Nightly Rate      : %s\n" +
                        "-----------------------------------------\n" +
                        "TOTAL CHARGE      : %s\n" +
                        "PAYMENT STATUS    : %s\n" +
                        "=========================================",
                        r.getBookingId(),
                        r.getBookingDate(),
                        r.getGuestName(),
                        r.getRoomNumber(),
                        r.getCategory().getDisplayName(),
                        r.getNights(),
                        moneyFmt.format(r.getCategory().getPricePerNight()),
                        moneyFmt.format(r.getTotalAmount()),
                        r.getPaymentStatus()
                );

                JTextArea area = new JTextArea(receipt);
                area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                area.setEditable(false);
                area.setBackground(TABLE_BG);
                area.setForeground(TEXT_WHITE);
                area.setMargin(new Insets(10, 10, 10, 10));

                JOptionPane.showMessageDialog(this, new JScrollPane(area), "Folio Details - " + bookingId, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
    }

    // ========================================================
    // UI REFRESH & PERSISTENCE
    // ========================================================
    private void refreshRoomTable() {
        roomTableModel.setRowCount(0);
        String filter = (String) categoryFilterBox.getSelectedItem();

        for (Room r : rooms.values()) {
            boolean matches = filter == null || filter.equals("All Categories") || r.getCategory().getDisplayName().equalsIgnoreCase(filter);
            if (matches) {
                roomTableModel.addRow(new Object[]{
                        r.getRoomNumber(),
                        r.getCategory().getDisplayName(),
                        moneyFmt.format(r.getCategory().getPricePerNight()),
                        r.isAvailable() ? "Available" : "Occupied"
                });
            }
        }
    }

    private void refreshAllViews() {
        refreshRoomTable();

        // Refresh Bookings Table
        bookingTableModel.setRowCount(0);
        double totalRev = 0.0;
        int occupiedCount = 0;

        for (Reservation r : reservations) {
            totalRev += r.getTotalAmount();
            bookingTableModel.addRow(new Object[]{
                    r.getBookingId(),
                    r.getGuestName(),
                    r.getRoomNumber() + " (" + r.getCategory().name() + ")",
                    moneyFmt.format(r.getTotalAmount()),
                    r.getPaymentStatus()
            });
        }

        for (Room r : rooms.values()) {
            if (!r.isAvailable()) occupiedCount++;
        }

        totalRoomsBadge.setText(formatHtml("TOTAL ROOMS", String.valueOf(rooms.size()), "#F0F3FA"));
        availableRoomsBadge.setText(formatHtml("AVAILABLE VACANCIES", String.valueOf(rooms.size() - occupiedCount), "#34D399"));
        bookedRoomsBadge.setText(formatHtml("OCCUPIED ROOMS", String.valueOf(occupiedCount), "#F87171"));
        totalRevenueBadge.setText(formatHtml("TOTAL EARNED REVENUE", moneyFmt.format(totalRev), "#F0F3FA"));
    }

    private void saveReservationsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(RESERVATIONS_FILE))) {
            for (Reservation r : reservations) {
                writer.println(r.toCsvRow());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving bookings: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadReservationsFromFile() {
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Reservation res = Reservation.fromCsvRow(line.trim());
                if (res != null) {
                    reservations.add(res);
                    Room r = rooms.get(res.getRoomNumber());
                    if (r != null) {
                        r.setAvailable(false);
                    }
                }
            }
        } catch (Exception ignored) {
            // Keep seeded default if file is empty or corrupted
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HotelReservationSystem system = new HotelReservationSystem();
            system.setVisible(true);
        });
    }
}