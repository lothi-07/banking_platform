import java.util.ArrayList;
import java.util.List;

public class Customer {
    private final String customerId;
    private final String name;
    private final String phone;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(String customerId, String name, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
    }

    public String getCustomerId() { return customerId; }
    public String getName()       { return name; }
    public String getPhone()      { return phone; }
    public List<Account> getAccounts() { return accounts; }
    public void addAccount(Account a)  { accounts.add(a); }
    public void removeAccount(Account a) { accounts.remove(a); }
}
