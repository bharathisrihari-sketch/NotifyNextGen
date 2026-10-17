package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class NotificationCategory(
    val displayName: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    ALL("All", Icons.Default.Notifications, Color(0xFF6366F1)),
    MESSAGES("Messages", Icons.Default.Chat, Color(0xFF10B981)),
    SOCIAL("Social", Icons.Default.Share, Color(0xFFEC4899)),
    FINANCE("Finance", Icons.Default.Payment, Color(0xFFF59E0B)),
    SHOPPING("Shopping", Icons.Default.ShoppingBag, Color(0xFF8B5CF6)),
    WORK("Work", Icons.Default.Work, Color(0xFF3B82F6)),
    SYSTEM("System", Icons.Default.Settings, Color(0xFF6B7280)),
    OTHER("Other", Icons.Default.Notifications, Color(0xFF64748B));

    companion object {
        fun classify(packageName: String, categoryTag: String?, title: String, content: String): NotificationCategory {
            val pkg = packageName.lowercase()
            val text = "$title $content".lowercase()

            return when {
                // Messaging & Calls
                pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
                pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") ||
                pkg.contains("viber") || pkg.contains("wechat") || pkg.contains("line") ||
                categoryTag?.contains("msg", ignoreCase = true) == true ||
                categoryTag?.contains("call", ignoreCase = true) == true -> MESSAGES

                // Work / Productivity
                pkg.contains("slack") || pkg.contains("teams") || pkg.contains("zoom") ||
                pkg.contains("gmail") || pkg.contains("outlook") || pkg.contains("mail") ||
                pkg.contains("trello") || pkg.contains("jira") || pkg.contains("notion") ||
                pkg.contains("calendar") || text.contains("meeting") || text.contains("deadline") -> WORK

                // Social
                pkg.contains("instagram") || pkg.contains("twitter") || pkg.contains("facebook") ||
                pkg.contains("tiktok") || pkg.contains("reddit") || pkg.contains("linkedin") ||
                pkg.contains("snapchat") || pkg.contains("pinterest") || pkg.contains("youtube") ||
                categoryTag?.contains("social", ignoreCase = true) == true -> SOCIAL

                // Finance & Banking
                pkg.contains("bank") || pkg.contains("paypal") || pkg.contains("venmo") ||
                pkg.contains("cashapp") || pkg.contains("stripe") || pkg.contains("wallet") ||
                pkg.contains("pay") || text.contains("transaction") || text.contains("credited") ||
                text.contains("debited") || text.contains("spent") || text.contains("otp") ||
                text.contains("payment") || text.contains("invoice") -> FINANCE

                // Shopping & Deliveries
                pkg.contains("amazon") || pkg.contains("ebay") || pkg.contains("target") ||
                pkg.contains("walmart") || pkg.contains("aliexpress") || pkg.contains("doordash") ||
                pkg.contains("ubereats") || pkg.contains("instacart") || pkg.contains("shopify") ||
                text.contains("delivered") || text.contains("shipped") || text.contains("order #") -> SHOPPING

                // System & Device
                pkg.contains("android") || pkg.contains("systemui") || pkg.contains("settings") ||
                pkg.contains("bluetooth") || pkg.contains("battery") || pkg.contains("download") ||
                pkg.contains("security") || categoryTag?.contains("sys", ignoreCase = true) == true -> SYSTEM

                else -> OTHER
            }
        }
    }
}
