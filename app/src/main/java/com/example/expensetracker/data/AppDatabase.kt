package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Account::class,
        Category::class,
        CategoryRule::class,
        TransactionEntity::class,
        Budget::class,
        PayableEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun payableDao(): PayableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payables` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `creditorName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL,
                        `isSettled` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create accounts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `accountNumberLast4` TEXT,
                        `nickname` TEXT
                    )
                    """.trimIndent()
                )

                // 2. Create categories table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconName` TEXT,
                        `colorHex` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")

                // 3. Create category_rules table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `merchantKeyword` TEXT NOT NULL,
                        `targetCategoryId` INTEGER NOT NULL,
                        FOREIGN KEY(`targetCategoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_rules_merchantKeyword` ON `category_rules` (`merchantKeyword`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_rules_targetCategoryId` ON `category_rules` (`targetCategoryId`)")

                // Seed default categories
                seedCategoriesSql(db)

                // Seed default accounts
                seedAccountsSql(db)

                // 4. Create new transactions table with foreign keys & split columns
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `merchantName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `isSplit` INTEGER NOT NULL DEFAULT 0,
                        `reimbursementAmount` REAL NOT NULL DEFAULT 0.0,
                        `settled` INTEGER NOT NULL DEFAULT 0,
                        `peerName` TEXT,
                        `notes` TEXT,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Migrate data from old transactions to transactions_new
                db.execSQL(
                    """
                    INSERT INTO `transactions_new` (`id`, `amount`, `merchantName`, `timestamp`, `transactionType`, `accountId`, `categoryId`, `isSplit`, `reimbursementAmount`, `settled`)
                    SELECT 
                        old.id,
                        old.amount,
                        old.merchantName,
                        old.timestamp,
                        old.transactionType,
                        1 AS accountId,
                        COALESCE(cat.id, 7) AS categoryId,
                        0 AS isSplit,
                        0.0 AS reimbursementAmount,
                        0 AS settled
                    FROM `transactions` old
                    LEFT JOIN `categories` cat ON LOWER(cat.name) = LOWER(old.category)
                    """.trimIndent()
                )

                // Drop old table and rename new table
                db.execSQL("DROP TABLE IF EXISTS `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")

                // Recreate indices on transactions table
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_timestamp_amount_merchantName_accountId` ON `transactions` (`timestamp`, `amount`, `merchantName`, `accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `nickname` TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `monthlyLimit` REAL NOT NULL,
                        `monthTimestamp` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId_monthTimestamp` ON `budgets` (`categoryId`, `monthTimestamp`)")
            }
        }

        private fun seedCategoriesSql(db: SupportSQLiteDatabase) {
            val defaultCategories = listOf(
                Pair("Food", "#FF7043"),
                Pair("Grocery", "#66BB6A"),
                Pair("Shopping", "#29B6F6"),
                Pair("Entertainment", "#AB47BC"),
                Pair("Travel", "#FFA726"),
                Pair("Bills & Utilities", "#EF5350"),
                Pair("General", "#7E57C2"),
                Pair("Salary", "#26A69A"),
                Pair("Freelance", "#42A5F5"),
                Pair("Investment", "#8D6E63"),
                Pair("Health", "#E91E63")
            )
            for ((name, color) in defaultCategories) {
                db.execSQL("INSERT OR IGNORE INTO `categories` (`name`, `colorHex`) VALUES ('$name', '$color')")
            }
        }

        private fun seedAccountsSql(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO `accounts` (`id`, `name`, `type`, `nickname`) VALUES (1, 'Primary Bank Account', 'BANK_ACCOUNT', NULL)")
            db.execSQL("INSERT OR IGNORE INTO `accounts` (`id`, `name`, `type`, `nickname`) VALUES (2, 'Cash', 'CASH', NULL)")
            db.execSQL("INSERT OR IGNORE INTO `accounts` (`id`, `name`, `type`, `nickname`) VALUES (3, 'Credit Card', 'CREDIT_CARD', NULL)")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedCategoriesSql(db)
                            seedAccountsSql(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedCategoriesSql(db)
                            seedAccountsSql(db)
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(db: AppDatabase) {
            runCatching {
                val categoryDao = db.categoryDao()
                val accountDao = db.accountDao()

                if (categoryDao.getCount() == 0) {
                    val categories = listOf(
                        Category(name = "Food", iconName = "food", colorHex = "#FF7043"),
                        Category(name = "Grocery", iconName = "grocery", colorHex = "#66BB6A"),
                        Category(name = "Shopping", iconName = "shopping", colorHex = "#29B6F6"),
                        Category(name = "Entertainment", iconName = "entertainment", colorHex = "#AB47BC"),
                        Category(name = "Travel", iconName = "travel", colorHex = "#FFA726"),
                        Category(name = "Bills & Utilities", iconName = "bills", colorHex = "#EF5350"),
                        Category(name = "General", iconName = "general", colorHex = "#7E57C2"),
                        Category(name = "Salary", iconName = "salary", colorHex = "#26A69A"),
                        Category(name = "Freelance", iconName = "freelance", colorHex = "#42A5F5"),
                        Category(name = "Investment", iconName = "investment", colorHex = "#8D6E63"),
                        Category(name = "Health", iconName = "health", colorHex = "#E91E63")
                    )
                    categoryDao.insertCategories(categories)
                }

                if (accountDao.getCount() == 0) {
                    val accounts = listOf(
                        Account(id = 1, name = "Primary Bank Account", type = AccountType.BANK_ACCOUNT),
                        Account(id = 2, name = "Cash", type = AccountType.CASH),
                        Account(id = 3, name = "Credit Card", type = AccountType.CREDIT_CARD)
                    )
                    accountDao.insertAccounts(accounts)
                } else {
                    // Reclassify existing misclassified card accounts
                    val allAccounts = accountDao.getAllAccountsDirect()
                    for (acc in allAccounts) {
                        val lowerName = acc.name.lowercase(java.util.Locale.ROOT)
                        if (acc.type == AccountType.BANK_ACCOUNT) {
                            if (lowerName.contains("credit card") || lowerName.contains("card") || lowerName.contains(" cc")) {
                                val newType = if (lowerName.contains("debit")) AccountType.DEBIT_CARD else AccountType.CREDIT_CARD
                                accountDao.updateAccountTypeAndName(acc.id, newType, acc.name)
                            }
                        }
                    }
                }
            }
        }
    }
}
