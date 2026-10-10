import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web version of the Banking Transaction System.
 * Enhanced with enterprise authentication, customer session management,
 * security PIN verification, and luxury accessibility.
 */
public class WebApp {
    private static final Bank bank = new Bank();
    private static final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> accountNo

    static {
        // Seed initial accounts for immediate testing
        bank.createAccount("Rahul Verma", "9876543210", "savings", 15000.0, "1234");
        bank.createAccount("Priya Patel", "9812345678", "current", 28000.0, "5678");
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT env variable: " + envPort + ", falling back to 8080");
            }
        } else if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port argument: " + args[0] + ", falling back to 8080");
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Health check endpoint for Render
        server.createContext("/health", ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                send(ex, 204, "text/plain", "");
                return;
            }
            send(ex, 200, "application/json", "{\"status\":\"UP\",\"service\":\"Apex Banking Platform\"}");
        });

        // Root page
        server.createContext("/", ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                send(ex, 204, "text/plain", "");
                return;
            }
            String content = loadPageContent();
            send(ex, 200, "text/html; charset=utf-8", content);
        });

        // 1. Authentication: Login
        server.createContext("/api/login", ex -> handleApi(ex, p -> {
            String loginId = p.getOrDefault("loginId", "").trim();
            String pin = p.getOrDefault("pin", "").trim();
            if (loginId.isEmpty() || pin.isEmpty()) {
                throw new IllegalArgumentException("Account number / phone and PIN are required.");
            }
            Account acc = bank.findAccountByPhoneOrAcc(loginId);
            if (acc == null) {
                throw new InvalidAccountException("Account not found with ID or Phone: " + loginId);
            }
            if (!acc.verifyPin(pin)) {
                return "{\"ok\":false,\"message\":\"Incorrect Security PIN. Please verify your 4-digit PIN.\"}";
            }
            String token = UUID.randomUUID().toString();
            sessions.put(token, acc.getAccountNo());
            return String.format("{\"ok\":true,\"token\":\"%s\",\"account\":%s,\"message\":\"Welcome back, %s!\"}",
                    token, accountDetailsJson(acc), esc(acc.getHolderName()));
        }));

        // 2. Authentication: Register / Open Account
        server.createContext("/api/register", ex -> handleApi(ex, p -> {
            String name = p.getOrDefault("name", "").trim();
            String phone = p.getOrDefault("phone", "").trim();
            String type = p.getOrDefault("type", "savings").trim();
            String amountStr = p.getOrDefault("amount", "0").trim();
            String pin = p.getOrDefault("pin", "1234").trim();

            if (name.isEmpty() || phone.isEmpty()) {
                throw new IllegalArgumentException("Full Name and Phone Number are required.");
            }
            if (pin.length() < 4) {
                throw new IllegalArgumentException("Security PIN must be at least 4 digits.");
            }
            double amount = Double.parseDouble(amountStr);
            if (type.equalsIgnoreCase("savings") && amount < 500) {
                throw new IllegalArgumentException("Savings account requires a minimum opening deposit of Rs.500.");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("Opening balance cannot be negative.");
            }

            Account acc = bank.createAccount(name, phone, type, amount, pin);
            String token = UUID.randomUUID().toString();
            sessions.put(token, acc.getAccountNo());

            return String.format("{\"ok\":true,\"token\":\"%s\",\"account\":%s,\"message\":\"Account %s opened successfully! Welcome to Apex Bank.\"}",
                    token, accountDetailsJson(acc), acc.getAccountNo());
        }));

        // 3. User Session: Profile & Balances
        server.createContext("/api/me", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) {
                return "{\"ok\":false,\"message\":\"Session expired or invalid token. Please log in.\"}";
            }
            return String.format("{\"ok\":true,\"account\":%s}", accountDetailsJson(acc));
        }));

        // 4. Logout
        server.createContext("/api/logout", ex -> handleApi(ex, p -> {
            String token = p.get("token");
            if (token != null) {
                sessions.remove(token);
            }
            return "{\"ok\":true,\"message\":\"Successfully logged out.\"}";
        }));

        // 5. Verify Recipient (for instant transfer lookup)
        server.createContext("/api/recipient", ex -> handleApi(ex, p -> {
            String accNo = p.getOrDefault("acc", "").trim();
            if (accNo.isEmpty()) {
                throw new IllegalArgumentException("Please enter an account number.");
            }
            Account target = bank.getAccount(accNo);
            return String.format("{\"ok\":true,\"no\":\"%s\",\"name\":\"%s\",\"type\":\"%s\"}",
                    esc(target.getAccountNo()), esc(target.getHolderName()), esc(target.getAccountType()));
        }));

        // 6. Deposit
        server.createContext("/api/deposit", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) throw new InvalidAccountException("Please authenticate first.");
            double amount = Double.parseDouble(p.getOrDefault("amount", "0"));
            acc.deposit(amount);
            return String.format("{\"ok\":true,\"message\":\"Deposited Rs.%.2f successfully. New balance: Rs.%.2f\",\"account\":%s}",
                    amount, acc.getBalance(), accountDetailsJson(acc));
        }));

        // 7. Withdraw
        server.createContext("/api/withdraw", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) throw new InvalidAccountException("Please authenticate first.");
            String pin = p.getOrDefault("pin", "").trim();
            if (!pin.isEmpty() && !acc.verifyPin(pin)) {
                return "{\"ok\":false,\"message\":\"Incorrect Security PIN for withdrawal authorization.\"}";
            }
            double amount = Double.parseDouble(p.getOrDefault("amount", "0"));
            acc.withdraw(amount);
            return String.format("{\"ok\":true,\"message\":\"Withdrew Rs.%.2f successfully. New balance: Rs.%.2f\",\"account\":%s}",
                    amount, acc.getBalance(), accountDetailsJson(acc));
        }));

        // 8. Transfer
        server.createContext("/api/transfer", ex -> handleApi(ex, p -> {
            Account from = getAuthAccount(p);
            if (from == null) {
                String fromNo = p.getOrDefault("from", "").trim();
                from = bank.getAccount(fromNo);
            }
            String pin = p.getOrDefault("pin", "").trim();
            if (!pin.isEmpty() && !from.verifyPin(pin)) {
                return "{\"ok\":false,\"message\":\"Incorrect Security PIN. Transfer rejected for your protection.\"}";
            }
            String toNo = p.getOrDefault("to", "").trim();
            double amount = Double.parseDouble(p.getOrDefault("amount", "0"));
            Account to = bank.getAccount(toNo);
            bank.transfer(from.getAccountNo(), toNo, amount);
            return String.format("{\"ok\":true,\"message\":\"Transferred Rs.%.2f to %s (%s) successfully.\",\"account\":%s}",
                    amount, to.getAccountNo(), esc(to.getHolderName()), accountDetailsJson(from));
        }));

        // 9. Freeze / Lock Account Card
        server.createContext("/api/freeze", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) throw new InvalidAccountException("Please authenticate first.");
            String pin = p.getOrDefault("pin", "").trim();
            if (!pin.isEmpty() && !acc.verifyPin(pin)) {
                return "{\"ok\":false,\"message\":\"Incorrect Security PIN.\"}";
            }
            boolean newStatus = !acc.isFrozen();
            acc.setFrozen(newStatus);
            String msg = newStatus
                    ? "Your debit card and account debits are now FROZEN. Withdrawals and transfers are blocked."
                    : "Your debit card is UNLOCKED and fully operational.";
            return String.format("{\"ok\":true,\"message\":\"%s\",\"account\":%s}", msg, accountDetailsJson(acc));
        }));

        // 10. Change PIN
        server.createContext("/api/change-pin", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) throw new InvalidAccountException("Please authenticate first.");
            String oldPin = p.getOrDefault("oldPin", "").trim();
            String newPin = p.getOrDefault("newPin", "").trim();
            if (!acc.verifyPin(oldPin)) {
                return "{\"ok\":false,\"message\":\"Current PIN does not match.\"}";
            }
            if (newPin.length() < 4) {
                return "{\"ok\":false,\"message\":\"New PIN must be at least 4 digits.\"}";
            }
            acc.setPin(newPin);
            return "{\"ok\":true,\"message\":\"Security PIN updated successfully.\"}";
        }));

        // 11. Interest
        server.createContext("/api/interest", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) {
                String accNo = p.getOrDefault("acc", "").trim();
                acc = bank.getAccount(accNo);
            }
            double interest = acc.calculateInterest();
            return String.format("{\"ok\":true,\"interest\":%.2f,\"type\":\"%s\",\"message\":\"%s yearly interest: Rs.%.2f\"}",
                    interest, acc.getAccountType(), acc.getAccountType(), interest);
        }));

        // 12. Transaction History
        server.createContext("/api/history", ex -> handleApi(ex, p -> {
            Account acc = getAuthAccount(p);
            if (acc == null) {
                String accNo = p.getOrDefault("acc", "").trim();
                acc = bank.getAccount(accNo);
            }
            return String.format("{\"ok\":true,\"account\":%s}", accountDetailsJson(acc));
        }));

        // 13. Accounts List (for Admin / System view)
        server.createContext("/api/accounts", ex -> send(ex, 200, "application/json", accountsJson()));

        // 14. Delete / Close Account
        server.createContext("/api/delete", ex -> handleApi(ex, p -> {
            String accountNo = p.getOrDefault("acc", "").trim();
            if (accountNo.isEmpty()) {
                Account auth = getAuthAccount(p);
                if (auth != null) accountNo = auth.getAccountNo();
            }
            bank.deleteAccount(accountNo);
            sessions.values().removeIf(accountNo::equals);
            return String.format("{\"ok\":true,\"message\":\"Account %s closed and removed from system.\",\"accounts\":%s}",
                    accountNo, accountsJson());
        }));

        // Legacy /api/create backward compatibility
        server.createContext("/api/create", ex -> handleApi(ex, p -> {
            Account a = bank.createAccount(p.get("name"), p.get("phone"), p.get("type"),
                    Double.parseDouble(p.get("amount")), p.getOrDefault("pin", "1234"));
            return String.format("{\"ok\":true,\"message\":\"Account created: %s\",\"account\":%s}",
                    a.getAccountNo(), accountDetailsJson(a));
        }));

        server.start();
        System.out.println("Apex Banking Web App running at http://localhost:" + port);
        System.out.println("Default Demo Accounts:");
        System.out.println("  1. ACC1001 (Rahul Verma) - PIN: 1234 - Savings (Rs.15,000)");
        System.out.println("  2. ACC1002 (Priya Patel) - PIN: 5678 - Current (Rs.28,000)");
        System.out.println("Press Ctrl+C to terminate.");
    }

    // ---------- Helpers ----------
    interface ApiAction { String run(Map<String, String> params) throws Exception; }

    private static void handleApi(HttpExchange ex, ApiAction action) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 204, "text/plain", "");
            return;
        }

        Map<String, String> params = getParams(ex);
        String json;
        synchronized (bank) {
            try {
                json = action.run(params);
            } catch (InsufficientFundsException e) {
                json = result(false, "[TRANSACTION FAILED] " + e.getMessage());
            } catch (InvalidAccountException e) {
                json = result(false, "[INVALID ACCOUNT] " + e.getMessage());
            } catch (IllegalStateException e) {
                json = result(false, "[SECURITY RESTRICTION] " + e.getMessage());
            } catch (NumberFormatException | NullPointerException e) {
                json = result(false, "[INVALID INPUT] Please fill all numerical and required fields properly.");
            } catch (IllegalArgumentException e) {
                json = result(false, "[VALIDATION ERROR] " + e.getMessage());
            } catch (Exception e) {
                json = result(false, "[ERROR] " + e.getMessage());
            }
        }
        send(ex, 200, "application/json", json);
    }

    private static Map<String, String> getParams(HttpExchange ex) throws IOException {
        Map<String, String> params = new HashMap<>();

        // URL Query parameters
        String query = ex.getRequestURI().getRawQuery();
        if (query != null && !query.isEmpty()) {
            parseQuery(query, params);
        }

        // Body parameters
        String method = ex.getRequestMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            byte[] bodyBytes = ex.getRequestBody().readAllBytes();
            if (bodyBytes.length > 0) {
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                parseQuery(body, params);
            }
        }

        // Authorization Bearer token header
        List<String> authHeaders = ex.getRequestHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String auth = authHeaders.get(0).trim();
            if (auth.startsWith("Bearer ")) {
                params.put("token", auth.substring(7).trim());
            }
        }

        return params;
    }

    private static void parseQuery(String query, Map<String, String> map) {
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, val);
        }
    }

    private static Account getAuthAccount(Map<String, String> params) {
        String token = params.get("token");
        if (token != null && sessions.containsKey(token)) {
            String accNo = sessions.get(token);
            try {
                return bank.getAccount(accNo);
            } catch (InvalidAccountException e) {
                sessions.remove(token);
            }
        }
        String directAcc = params.get("acc");
        if (directAcc != null && !directAcc.trim().isEmpty()) {
            try {
                return bank.getAccount(directAcc.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String accountsJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Account> list;
        synchronized (bank) { list = bank.getAllAccounts(); }
        for (int i = 0; i < list.size(); i++) {
            Account a = list.get(i);
            Customer c = bank.getCustomerForAccount(a.getAccountNo());
            String phone = c != null ? c.getPhone() : "";
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"no\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\",\"type\":\"%s\",\"balance\":%.2f,\"frozen\":%b}",
                    esc(a.getAccountNo()), esc(a.getHolderName()), esc(phone), a.getAccountType(), a.getBalance(), a.isFrozen()));
        }
        return sb.append("]").toString();
    }

    private static String accountDetailsJson(Account a) {
        Customer c = bank.getCustomerForAccount(a.getAccountNo());
        String phone = c != null ? c.getPhone() : "";
        double minBal = a instanceof SavingsAccount ? 500.0 : 0.0;
        double overdraft = a instanceof CurrentAccount ? 10000.0 : 0.0;
        double avail = a instanceof SavingsAccount
                ? Math.max(0, a.getBalance() - 500.0)
                : a.getBalance() + 10000.0;
        double interest = a.calculateInterest();

        StringBuilder txs = new StringBuilder("[");
        List<Transaction> list = a.getTransactions();
        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            if (i > 0) txs.append(",");
            txs.append(String.format("{\"type\":\"%s\",\"amount\":%.2f,\"status\":\"%s\",\"date\":\"%s\"}",
                    esc(t.getType()), t.getAmount(), esc(t.getStatus()), esc(t.getFormattedDate())));
        }
        txs.append("]");

        return String.format(
                "{\"no\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\",\"type\":\"%s\",\"balance\":%.2f," +
                "\"frozen\":%b,\"minBalance\":%.2f,\"overdraft\":%.2f,\"available\":%.2f," +
                "\"interest\":%.2f,\"transactions\":%s}",
                esc(a.getAccountNo()), esc(a.getHolderName()), esc(phone),
                esc(a.getAccountType()), a.getBalance(), a.isFrozen(),
                minBal, overdraft, avail, interest, txs.toString()
        );
    }

    private static String result(boolean ok, String message) {
        return "{\"ok\":" + ok + ",\"message\":\"" + esc(message) + "\"}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String loadPageContent() {
        try {
            Path file = Path.of("index.html");
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
            Path userDirFile = Path.of(System.getProperty("user.dir", "."), "index.html");
            if (Files.exists(userDirFile)) {
                return Files.readString(userDirFile, StandardCharsets.UTF_8);
            }
            try (java.io.InputStream is = WebApp.class.getResourceAsStream("/index.html")) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            try (java.io.InputStream is = WebApp.class.getClassLoader().getResourceAsStream("index.html")) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<!DOCTYPE html><html><body><h1>Apex Banking System</h1><p>Please ensure index.html exists in the working directory.</p></body></html>";
    }
}
