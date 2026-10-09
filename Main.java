import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("===== BANKING TRANSACTION SYSTEM =====");
        while (running) {
            System.out.println("\n1. Create Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Transaction History");
            System.out.println("6. Calculate Interest");
            System.out.println("7. Show All Accounts");
            System.out.println("0. Exit");
            System.out.print("Choose option: ");

            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Name: ");            String name = sc.nextLine();
                        System.out.print("Phone: ");           String phone = sc.nextLine();
                        System.out.print("Type (savings/current): "); String type = sc.nextLine();
                        System.out.print("Opening balance: "); double bal = Double.parseDouble(sc.nextLine());
                        Account a = bank.createAccount(name, phone, type, bal);
                        System.out.println("Account created -> " + a);
                    }
                    case "2" -> {
                        System.out.print("Account no: "); Account a = bank.getAccount(sc.nextLine().trim());
                        System.out.print("Amount: ");     a.deposit(Double.parseDouble(sc.nextLine()));
                        System.out.println("Deposit successful. New balance: Rs." + a.getBalance());
                    }
                    case "3" -> {
                        System.out.print("Account no: "); Account a = bank.getAccount(sc.nextLine().trim());
                        System.out.print("Amount: ");     a.withdraw(Double.parseDouble(sc.nextLine()));
                        System.out.println("Withdrawal successful. New balance: Rs." + a.getBalance());
                    }
                    case "4" -> {
                        System.out.print("From account: "); String from = sc.nextLine().trim();
                        System.out.print("To account: ");   String to = sc.nextLine().trim();
                        System.out.print("Amount: ");       double amt = Double.parseDouble(sc.nextLine());
                        bank.transfer(from, to, amt);
                        System.out.println("Transfer successful.");
                    }
                    case "5" -> {
                        System.out.print("Account no: ");
                        bank.getAccount(sc.nextLine().trim()).printHistory();
                    }
                    case "6" -> {
                        System.out.print("Account no: ");
                        Account a = bank.getAccount(sc.nextLine().trim());
                        System.out.printf("%s yearly interest: Rs.%.2f%n", a.getAccountType(), a.calculateInterest());
                    }
                    case "7" -> bank.showAllAccounts();
                    case "0" -> { running = false; System.out.println("Thank you for banking with us!"); }
                    default -> System.out.println("Invalid option, try again.");
                }
            } catch (InsufficientFundsException e) {
                System.out.println("[TRANSACTION FAILED] " + e.getMessage());
            } catch (InvalidAccountException e) {
                System.out.println("[INVALID ACCOUNT] " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("[INVALID INPUT] Please enter a valid number.");
            } catch (IllegalArgumentException e) {
                System.out.println("[INVALID AMOUNT] " + e.getMessage());
            }
        }
        sc.close();
    }
}
