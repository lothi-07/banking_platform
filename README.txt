BANKING TRANSACTION SYSTEM - Java OOP Mini Banking Platform
============================================================

Files: Account (abstract), SavingsAccount, CurrentAccount, Customer, Transaction,
       Bank, InsufficientFundsException, InvalidAccountException, Main, WebApp, index.html

HOW TO RUN CONSOLE VERSION (Command Prompt / Terminal):
-------------------------------------------------------
  javac *.java
  java Main

AUTO DEMO (Console):
  Windows :  java Main < demo_input.txt
  Linux   :  java Main < demo_input.txt

WEB VERSION (Enterprise Luxury Digital Banking UI)
==================================================
  javac *.java
  java WebApp

Then open http://localhost:8080 in Chrome / Edge / Firefox.

KEY FEATURES & SECURITY:
------------------------
1. Mandatory Security Authentication:
   - Login with Account Number or Registered Phone + 4-Digit PIN.
   - Demo Accounts pre-seeded for 1-click testing:
     * ACC1001 (Rahul Verma) - PIN: 1234 - Savings (Rs.15,000)
     * ACC1002 (Priya Patel) - PIN: 5678 - Current (Rs.28,000)
   - Open New Account (instant registration with custom PIN and opening balance).

2. Customer Banking Hub:
   - 3D Interactive Virtual Debit Card (with EMV chip, contactless wave, copy account number).
   - Real-time Net Balance, Available Spending, and Interest Accrual stats.
   - Instant Account Transfer with live recipient verification (shows recipient name dynamically).
   - Deposit & Withdrawal Cash Management with quick amount chips (+Rs.500, +Rs.1,000, etc.) and live balance forecaster.
   - Interactive Interest Calculator & Compound Growth Simulator.
   - Passbook Audit Ledger with filtering (All, Inflows, Outflows).
   - Official Statement Generator with print / export slip modal.
   - Security Controls: Card Freeze / Lock toggle & PIN Change.

3. Accessibility & Aesthetics:
   - Dark Luxury Glassmorphism & Light Pearl themes with instant toggle.
   - Web Audio tactile sound effects.
   - Mask/reveal privacy eye for account balances.
