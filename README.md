# SpendSync 📱💸

**SpendSync** is an offline-first, intelligent Android expense tracking and split-management application built with modern **Jetpack Compose**, **Room Relational Database (v2)**, and **Kotlin Coroutines**. It automatically transforms bank SMS alerts into categorized expenses, tracks friend reimbursements, and offers deep financial insights without sacrificing user privacy.

---

## ✨ Features

### 🚀 1. Automated Bank SMS Tracking
- **Real-time SMS Interceptor**: Background `BroadcastReceiver` captures incoming transactional SMS from Indian banks (HDFC, SBI, ICICI, Axis, Kotak, PNB, Canara, Bank of Baroda, Paytm, etc.) and records transactions instantly without opening the app.
- **Historical Inbox Scanner**: One-tap asynchronous backfill of past bank transactions from the device SMS inbox upon granting permissions, with built-in deduplication.

### 🧠 2. Smart & Auto-Learning Categorization Engine
- **Priority Rule Engine**: When you edit a transaction's category, SpendSync prompts to remember the rule. Future matching SMS messages are automatically categorized according to your custom rules.
- **Expanded Dictionary**: Built-in comprehensive mappings for dining (Swiggy, Zomato, KFC, Belgian Waffle, Chicking), groceries (Instamart, Blinkit, Zepto, DMart), shopping (Amazon, Flipkart, Myntra, Ajio), utilities (KSEB, Water Authority, Airtel, Jio, Tata Play), travel (Uber, Ola, IRCTC, Fuel bunks), and entertainment (BookMyShow, PVR, Netflix, theatres).
- **Heuristic & Similarity Fallback**: Normalized Levenshtein similarity and token clustering algorithms prevent transactions from defaulting to "General".

### 🤝 3. Split Expense & Reimbursement Settlements
- **Net vs Gross Monthly Calculations**: Accurately computes your true spending:
  $$\text{Net Spending} = \sum (\text{Expense Amount} - \text{Reimbursement Amount})$$
  *(Automatically excludes Credit Card Bill Transfers and Income from double counting).*
- **"Owed to Me" Tracker**: Dedicated active settlements dashboard showing pending money owed by peers with a 1-tap **Mark as Settled** action.
- **UPI Split Listener**: `NotificationListenerService` that detects split requests and payment confirmations from apps like Google Pay, PhonePe, and Paytm.

### 📊 4. Deep Insights & Interactive Category Drill-Down
- **Category Spending Breakdown**: Visual progress bars and percentage share for each category in the selected month.
- **Interactive Drill-Down**: Tap any category row to open a filtered view showing individual transactions for that category.
- **Multi-Month Selector**: Horizontally scrollable filter bar to review previous months' records and spending totals.

### 🏦 5. Relational Multi-Account Management & Sidebar
- **Sidebar Drawer**: `ModalNavigationDrawer` displaying all user accounts (Bank Accounts, Credit Cards, Cash, Wallets).
- **Dynamic Account Filtering**: Filter transaction feeds, monthly cards, and category breakdowns per specific account or across all accounts.

### 🔐 6. Dual Authentication (Google OAuth & Phone OTP)
- **Google Sign-In**: Quick sign-in using Google credentials.
- **Phone OTP Verification**: 6-digit SMS verification flow via Firebase Auth.
- **Guest / Offline Mode**: Instant bypass option to use SpendSync 100% offline without mandatory cloud configuration.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design Tokens
- **Language**: [Kotlin](https://kotlinlang.org/) (Coroutines, StateFlow, Flow)
- **Database**: [Android Room Database v2](https://developer.android.com/training/data-storage/room) with Relational Entities (`Account`, `Category`, `CategoryRule`, `TransactionEntity`) and `@Relation` joins
- **Architecture**: Clean MVVM (Model-View-ViewModel) pattern
- **Authentication**: Firebase Authentication + Google Play Services Auth
- **Build System**: Gradle Version Catalogs (`libs.versions.toml`) & Kotlin DSL (`build.gradle.kts`)

---

## 📂 Project Structure

```
app/src/main/java/com/example/expensetracker/
├── MainActivity.kt               # Main entry point managing Auth & Dashboard routing
├── data/
│   ├── Account.kt                # Account entity (Bank, Credit Card, Cash)
│   ├── Category.kt               # Category entity
│   ├── CategoryRule.kt           # Custom auto-learning mapping rules
│   ├── TransactionEntity.kt      # Relational transaction model with split fields
│   ├── TransactionWithDetails.kt # Embedded relation model for Room
│   ├── AccountDao.kt             # Account data access object
│   ├── CategoryDao.kt            # Category data access object
│   ├── CategoryRuleDao.kt        # Rules data access object
│   ├── TransactionDao.kt         # Transaction queries & settlement updates
│   └── AppDatabase.kt            # Database v2 configuration with MIGRATION_1_2
├── domain/
│   ├── SmsParser.kt              # Regex parser & expanded merchant dictionary
│   ├── SmartCategorizer.kt       # Levenshtein similarity & ML classification interface
│   └── HistoricalSmsScanner.kt   # Async SMS inbox scanner & backfiller
├── receiver/
│   └── SmsReceiver.kt            # BroadcastReceiver for live bank SMS
├── service/
│   └── GPayNotificationListener.kt # Notification listener for UPI payment splits
└── ui/
    ├── AuthScreen.kt             # Dual Auth screen (Phone OTP & Google OAuth)
    ├── DashboardScreen.kt        # Main Compose UI, Sidebar, Cards & Drill-Downs
    └── MainViewModel.kt          # Reactive ViewModel with StateFlows
```

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio Ladybug | 2024.2+** or newer
- **JDK 17+** / **JDK 21**
- Android SDK 34+ (Target SDK 35/37, Min SDK 24)

### Clone & Build
```bash
# Clone the repository
git clone git@github.com:ashwinsaji2588/SpendSync.git

# Navigate to the project directory
cd SpendSync

# Build the debug APK
./gradlew assembleDebug
```

---

## 🛡️ Permissions & Background Troubleshooting

To enable real-time automatic tracking:
1. **SMS Permissions**: Grant `RECEIVE_SMS` and `READ_SMS` when prompted.
2. **Aggressive Battery Optimizations (MIUI / Samsung / OnePlus / Oppo)**:
   - Navigate to **Settings > Apps > SpendSync**.
   - Enable **Autostart** / **Allow Background Activity**.
   - Set Battery Optimization to **No Restrictions**.
3. **UPI Split Listener**: Enable SpendSync under **Settings > Notification Access** to allow automatic GPay/PhonePe split detection.

---

## 📬 Support & Contact

- **Developer**: Ashwin Saji
- **Email**: [ashwinsaji2588@gmail.com](mailto:ashwinsaji2588@gmail.com)
- **Repository**: [github.com/ashwinsaji2588/SpendSync](https://github.com/ashwinsaji2588/SpendSync)
