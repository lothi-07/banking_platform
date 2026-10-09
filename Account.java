import java.util.ArrayList;
import java.util.List;

public abstract class Account {
    private final String accountNo;
    private final String holderName;
    private double balance;                      // Encapsulation: private
    private final List<Transaction> history = new ArrayList<>();
    private String pin = "1234";
    private boolean frozen = false;

    public Account(String accountNo, String holderName, double openingBalance) {
        this.accountNo = accountNo;
        this.holderName = holderName;
        this.balance = openingBalance;
        history.add(new Transaction("ACCOUNT_OPEN", openingBalance, "SUCCESS"));
    }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public boolean verifyPin(String candidate) { return this.pin != null && this.pin.equals(candidate); }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public List<Transaction> getTransactions() { return history; }

    public String getAccountNo()  { return accountNo; }
    public String getHolderName() { return holderName; }
    public double getBalance()    { return balance; }

    public void deposit(double amount) {
        if (amount <= 0) {
            history.add(new Transaction("DEPOSIT", amount, "FAILED"));
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        balance += amount;
        history.add(new Transaction("DEPOSIT", amount, "SUCCESS"));
    }

    // Polymorphism: each account type decides its own withdrawal rule
    public abstract void withdraw(double amount) throws InsufficientFundsException;

    public abstract double calculateInterest();

    public abstract String getAccountType();

    // Subclasses use these protected helpers, outside classes cannot touch balance directly
    protected void debit(double amount, String type) {
        if (frozen) {
            logFailure(type, amount);
            throw new IllegalStateException("Account/Card is currently frozen for security. Unfreeze it in the dashboard.");
        }
        balance -= amount;
        history.add(new Transaction(type, amount, "SUCCESS"));
    }

    protected void logFailure(String type, double amount) {
        history.add(new Transaction(type, amount, "FAILED"));
    }

    protected void credit(double amount, String type) {
        balance += amount;
        history.add(new Transaction(type, amount, "SUCCESS"));
    }

    public List<String> getHistoryLines() {
        List<String> lines = new ArrayList<>();
        for (Transaction t : history) {
            lines.add(t.toString());
        }
        return lines;
    }

    public void printHistory() {
        System.out.println("--- Transaction History: " + accountNo + " (" + holderName + ") ---");
        for (Transaction t : history) {
            System.out.println(t);
        }
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %-8s | Balance: Rs.%.2f",
                accountNo, holderName, getAccountType(), balance);
    }
}
