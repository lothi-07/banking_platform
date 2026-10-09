public class CurrentAccount extends Account {
    private static final double OVERDRAFT_LIMIT = 10000;

    public CurrentAccount(String accountNo, String holderName, double openingBalance) {
        super(accountNo, holderName, openingBalance);
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than 0");
        }
        if (getBalance() - amount < -OVERDRAFT_LIMIT) {
            logFailure("WITHDRAW", amount);
            throw new InsufficientFundsException(
                    "Overdraft limit of Rs." + OVERDRAFT_LIMIT + " exceeded");
        }
        debit(amount, "WITHDRAW");
    }

    @Override
    public double calculateInterest() {
        return 0;   // Current accounts earn no interest
    }

    @Override
    public String getAccountType() { return "CURRENT"; }
}
