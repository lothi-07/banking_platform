import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bank {
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Customer> customers = new HashMap<>();
    private int nextAccountNo = 1001;
    private int nextCustomerNo = 1;

    public Account createAccount(String name, String phone, String type, double openingBalance) {
        return createAccount(name, phone, type, openingBalance, "1234");
    }

    public Account createAccount(String name, String phone, String type, double openingBalance, String pin) {
        Customer c = new Customer("C" + (nextCustomerNo++), name, phone);
        customers.put(c.getCustomerId(), c);
        String accNo = "ACC" + (nextAccountNo++);
        Account acc = type.equalsIgnoreCase("savings")
                ? new SavingsAccount(accNo, name, openingBalance)
                : new CurrentAccount(accNo, name, openingBalance);
        if (pin != null && !pin.trim().isEmpty()) {
            acc.setPin(pin.trim());
        }
        c.addAccount(acc);
        accounts.put(accNo, acc);
        return acc;
    }

    public Account findAccountByPhoneOrAcc(String query) {
        if (query == null) return null;
        String q = query.trim();
        Account byAcc = accounts.get(q);
        if (byAcc != null) return byAcc;
        for (Customer c : customers.values()) {
            if (c.getPhone() != null && c.getPhone().trim().equalsIgnoreCase(q)) {
                if (!c.getAccounts().isEmpty()) {
                    return c.getAccounts().get(0);
                }
            }
        }
        return null;
    }

    public Customer getCustomerForAccount(String accNo) {
        for (Customer c : customers.values()) {
            for (Account a : c.getAccounts()) {
                if (a.getAccountNo().equals(accNo)) {
                    return c;
                }
            }
        }
        return null;
    }

    public Account getAccount(String accNo) throws InvalidAccountException {
        Account acc = accounts.get(accNo);
        if (acc == null) {
            throw new InvalidAccountException("Account " + accNo + " not found");
        }
        return acc;
    }

    public void deleteAccount(String accNo) throws InvalidAccountException {
        Account account = accounts.remove(accNo);
        if (account == null) {
            throw new InvalidAccountException("Account " + accNo + " not found");
        }

        customers.values().removeIf(customer -> {
            customer.removeAccount(account);
            return customer.getAccounts().isEmpty();
        });
    }

    // Transfer = withdraw from one + deposit to other, as one operation
    public void transfer(String fromNo, String toNo, double amount)
            throws InvalidAccountException, InsufficientFundsException {
        Account from = getAccount(fromNo);
        Account to = getAccount(toNo);      // validate BOTH accounts before moving money
        if (fromNo.equals(toNo)) {
            throw new InvalidAccountException("Cannot transfer to the same account");
        }
        from.withdraw(amount);              // if this throws, nothing is deposited
        to.deposit(amount);
    }

    public List<Account> getAllAccounts() {
        List<Account> list = new ArrayList<>(accounts.values());
        list.sort((a, b) -> a.getAccountNo().compareTo(b.getAccountNo()));
        return list;
    }

    public void showAllAccounts() {
        if (accounts.isEmpty()) {
            System.out.println("No accounts yet.");
            return;
        }
        accounts.values().stream()
                .sorted((a, b) -> a.getAccountNo().compareTo(b.getAccountNo()))
                .forEach(System.out::println);
    }
}
