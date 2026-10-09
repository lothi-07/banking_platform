public class SavingsAccount extends Account {
    private static final double INTEREST_RATE = 0.04;   // 4% per year
    private static final double MIN_BALANCE = 500;

    public SavingsAccount(String accountNo, String holderName, double openingBalance) {
        super(accountNo, holderName, openingBalance);
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than 0");
        }
        if (getBalance() - amount < MIN_BALANCE) {
            logFailure("WITHDRAW", amount);
            throw new InsufficientFundsException(
                    "Savings account must keep minimum balance of Rs." + MIN_BALANCE
                            + ". Available to withdraw: Rs." + Math.max(0, getBalance() - MIN_BALANCE));
        }
        debit(amount, "WITHDRAW");
    }

    @Override
    public double calculateInterest() {
        return getBalance() * INTEREST_RATE;
    }

    @Override
    public String getAccountType() { return "SAVINGS"; }
}
