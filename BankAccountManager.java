import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

public class BankAccountManager extends JFrame {

    
    static class BankAccount {
        private final String name;
        private final String accountNumber;
        private BigDecimal balance;
        private final DefaultTableModel transactionModel;

        public BankAccount(String name, String accountNumber) {
            this.name = name;
            this.accountNumber = accountNumber;
            this.balance = new BigDecimal("0.00");
            this.transactionModel = new DefaultTableModel(
                new String[]{"🕒 Time", "Transaction", "Amount (₹)", "Balance (₹)"}, 0
            );
            addTransaction("Account Created", BigDecimal.ZERO, balance);
        }

        public synchronized void deposit(BigDecimal amount) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Amount must be positive.");
            balance = balance.add(amount);
            addTransaction("Deposit", amount, balance);
        }

        public synchronized boolean withdraw(BigDecimal amount) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Amount must be positive.");
            if (amount.compareTo(balance) > 0)
                return false;
            balance = balance.subtract(amount);
            addTransaction("Withdraw", amount.negate(), balance);
            return true;
        }

        public synchronized BigDecimal getBalance() { return balance; }

        public DefaultTableModel getTransactionModel() { return transactionModel; }

        private void addTransaction(String type, BigDecimal amt, BigDecimal bal) {
            transactionModel.addRow(new Object[]{
                DATE_FORMAT.format(new Date()),
                type,
                type.equals("Account Created") ? "-" : formatINR(amt),
                formatINR(bal)
            });
        }

        public String getInfo() {
            return "👤 Account Holder: " + name +
                   "\n🏦 Account No: " + accountNumber +
                   "\n💰 Current Balance: " + formatINR(balance);
        }

        public String getDisplayName() { return name + " (" + accountNumber + ")"; }
    }

    
    private static final Locale INR_LOCALE = Locale.forLanguageTag("en-IN");
    private static final NumberFormat INR_FORMAT = NumberFormat.getCurrencyInstance(INR_LOCALE);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, hh:mm a");

    private final Map<String, BankAccount> accounts = new LinkedHashMap<>();
    private BankAccount currentAccount;

    private JTable historyTable;
    private JTextField amountField;
    private JTextArea infoArea;
    private JComboBox<String> userSelector;

    
    private JButton depositBtn, withdrawBtn, balanceBtn, infoBtn;

    
    public BankAccountManager() {
        setTitle("🏦 Bank Account Manager - Multi User");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(Color.decode("#F9FBFF"));

        setupHeader();
        setupControlPanel();
        setupTable();
        setupInfoArea();

        
        updateControlsEnabled(false);

        setVisible(true);

        
        SwingUtilities.invokeLater(this::showAddUserDialog);
    }

    
    private void setupHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 136, 229));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("🏦 Bank Account Manager Dashboard", JLabel.CENTER);
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
    }

    
    private void setupControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(new CompoundBorder(
            new EmptyBorder(15, 15, 15, 15),
            new LineBorder(new Color(220, 220, 220), 1, true)
        ));
        controlPanel.setPreferredSize(new Dimension(300, 0));

        JLabel sectionTitle = new JLabel("⚙️ Account Controls");
        sectionTitle.setFont(new Font("Segoe UI Semibold", Font.BOLD, 18));
        sectionTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sectionTitle.setForeground(new Color(25, 42, 85));
        controlPanel.add(sectionTitle);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        userPanel.setBackground(Color.WHITE);
        userPanel.add(new JLabel("Active Account:"));
        userSelector = new JComboBox<>();
        userSelector.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userSelector.setPreferredSize(new Dimension(180, 28));
        userSelector.addActionListener(e -> switchUser());
        JButton addUserBtn = createRoundedButton("➕ Add User", e -> showAddUserDialog());
        userPanel.add(userSelector);
        userPanel.add(addUserBtn);
        controlPanel.add(userPanel);

        
        JPanel amountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        amountPanel.setBackground(Color.WHITE);
        amountPanel.add(new JLabel("Amount (₹):"));
        amountField = new JTextField(12);
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        amountField.setBorder(new CompoundBorder(
            new LineBorder(Color.LIGHT_GRAY, 1, true),
            new EmptyBorder(5, 8, 5, 8)
        ));
        amountPanel.add(amountField);
        JButton clearBtn = createRoundedButton("🧹 Clear", e -> amountField.setText(""));
        amountPanel.add(clearBtn);
        controlPanel.add(amountPanel);

        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        
        JPanel btnGrid = new JPanel(new GridLayout(4, 1, 10, 10));
        btnGrid.setBackground(Color.WHITE);
        depositBtn = createWideButton("💰 Deposit", e -> depositAction());
        withdrawBtn = createWideButton("💸 Withdraw", e -> withdrawAction());
        balanceBtn = createWideButton("📊 Check Balance", e -> {
            if (currentAccount != null)
                showMessage("💰 Balance: " + formatINR(currentAccount.getBalance()));
        });
        infoBtn = createWideButton("ℹ️ Account Info", e -> {
            if (currentAccount != null)
                showMessage(currentAccount.getInfo());
        });
        btnGrid.add(depositBtn);
        btnGrid.add(withdrawBtn);
        btnGrid.add(balanceBtn);
        btnGrid.add(infoBtn);
        controlPanel.add(btnGrid);

        add(controlPanel, BorderLayout.WEST);
    }

    
    private void setupTable() {
        historyTable = new JTable();
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(25);
        historyTable.setGridColor(new Color(230, 230, 230));
        historyTable.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        historyTable.getTableHeader().setBackground(new Color(240, 240, 240));
        historyTable.setShowGrid(true);

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(scroll, BorderLayout.CENTER);
    }

    
    private void setupInfoArea() {
        infoArea = new JTextArea("💡 Tip: Click 'Add User' to create your first account.");
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        infoArea.setEditable(false);
        infoArea.setBackground(new Color(234, 244, 255));
        infoArea.setForeground(new Color(33, 53, 85));
        infoArea.setBorder(new EmptyBorder(12, 15, 12, 15));
        add(infoArea, BorderLayout.SOUTH);
    }

    
    private void showAddUserDialog() {
        JTextField nameField = new JTextField();
        JTextField accField = new JTextField();
        Object[] fields = {
            "Enter User Name:", nameField,
            "Enter Account Number:", accField
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "➕ Add New User", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String acc = accField.getText().trim();

            if (name.isEmpty() || acc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Both fields are required.");
                return;
            }
            if (accounts.containsKey(acc)) {
                JOptionPane.showMessageDialog(this, "⚠️ Account number already exists!");
                return;
            }

            createAccount(name, acc);
            showMessage("✅ User " + name + " added successfully!");
        } else {
            
            if (accounts.isEmpty()) {
                showError("No account selected. Please add a user to begin.");
            }
        }
    }

    
    protected void createAccount(String name, String acc) {
        BankAccount newAccount = new BankAccount(name, acc);
        accounts.put(acc, newAccount);
        userSelector.addItem(newAccount.getDisplayName());
        userSelector.setSelectedItem(newAccount.getDisplayName());
        currentAccount = newAccount;
        historyTable.setModel(currentAccount.getTransactionModel());
        updateControlsEnabled(true);
    }

    private void switchUser() {
        if (userSelector.getSelectedItem() == null) return;
        String selected = (String) userSelector.getSelectedItem();
        String accNo = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
        currentAccount = accounts.get(accNo);
        historyTable.setModel(currentAccount.getTransactionModel());
        showMessage("👤 Switched to: " + currentAccount.getDisplayName());
        updateControlsEnabled(true);
    }

    
    private void depositAction() {
        if (currentAccount == null) { showError("❌ Please add/select an account first!"); return; }
        BigDecimal amount = getAmount(); if (amount == null) return;
        currentAccount.deposit(amount);
        showMessage("✅ Deposited " + formatINR(amount) + "\nNew Balance: " + formatINR(currentAccount.getBalance()));
    }

    private void withdrawAction() {
        if (currentAccount == null) { showError("❌ Please add/select an account first!"); return; }
        BigDecimal amount = getAmount(); if (amount == null) return;
        if (currentAccount.withdraw(amount))
            showMessage("✅ Withdrawn " + formatINR(amount) + "\nNew Balance: " + formatINR(currentAccount.getBalance()));
        else showError("❌ Insufficient balance!");
    }

    private BigDecimal getAmount() {
        try {
            if (amountField.getText().trim().isEmpty()) {
                showError("❌ Please enter an amount."); return null;
            }
            BigDecimal amt = new BigDecimal(amountField.getText().trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                showError("❌ Enter a positive amount."); return null;
            }
            return amt.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            showError("❌ Invalid amount format."); return null;
        }
    }

    
    private void updateControlsEnabled(boolean enabled) {
        if (depositBtn != null) depositBtn.setEnabled(enabled);
        if (withdrawBtn != null) withdrawBtn.setEnabled(enabled);
        if (balanceBtn != null) balanceBtn.setEnabled(enabled);
        if (infoBtn != null) infoBtn.setEnabled(enabled);
    }

    private void showMessage(String msg) {
        infoArea.setForeground(new Color(0, 100, 0));
        infoArea.setBackground(new Color(234, 244, 255));
        infoArea.setText(msg);
    }

    private void showError(String msg) {
        infoArea.setForeground(new Color(153, 0, 0));
        infoArea.setBackground(new Color(255, 235, 238));
        infoArea.setText(msg);
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JButton createRoundedButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(new Color(240, 240, 240));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        btn.setBorder(new LineBorder(Color.GRAY, 1, true));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(225, 225, 225)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(240, 240, 240)); }
        });
        return btn;
    }

    private JButton createWideButton(String text, ActionListener action) {
        JButton btn = createRoundedButton(text, action);
        btn.setPreferredSize(new Dimension(240, 45));
        btn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        return btn;
    }

    private static String formatINR(BigDecimal value) { return INR_FORMAT.format(value); }

    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BankAccountManager::new);
    }
}
