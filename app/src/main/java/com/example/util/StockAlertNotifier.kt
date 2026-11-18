package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Product
import kotlin.math.max

object StockAlertNotifier {

    const val CHANNEL_ID = "low_stock_alerts_channel"
    private const val NOTIFICATION_ID = 4102

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات انخفاض المخزون",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تلقائية عندما تنخفض كمية أي منتج عن الحد الأدنى المحدد"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyLowStockProducts(context: Context, lowProducts: List<Product>): Boolean {
        if (lowProducts.isEmpty()) return false
        if (!hasNotificationPermission(context)) return false

        createChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (lowProducts.size == 1) {
            val product = lowProducts.first()
            val statusText = if (product.isOutOfStock()) "نفد من المخزون!" else "اقترب من النفاد"
            val body = "الكمية المتبقية: ${product.formattedQuantity()} ${product.unit} (الحد الأدنى: ${product.formattedThreshold()}) • يرجى إعادة التوريد."
            builder
                .setContentTitle("تنبيه مخزون: ${product.name} ($statusText)")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        } else {
            val outCount = lowProducts.count { it.isOutOfStock() }
            val title = "تنبيه انخفاض المخزون (${lowProducts.size} منتجات)"
            val summary = if (outCount > 0) {
                "$outCount نفد تماماً • ${lowProducts.size - outCount} تحت الحد الأدنى"
            } else {
                "يرجى مراجعة المخزون وإعادة التوريد لضمان استمرارية المبيعات"
            }
            val inbox = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText(summary)

            lowProducts.take(5).forEach { p ->
                val tag = if (p.isOutOfStock()) "نفد" else "منخفض"
                inbox.addLine("${p.name}: ${p.formattedQuantity()} ${p.unit} (الحد ${p.formattedThreshold()}) [$tag]")
            }
            if (lowProducts.size > 5) {
                inbox.addLine("... و${lowProducts.size - 5} منتجات أخرى")
            }

            builder
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(inbox)
        }

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun buildLowStockWhatsAppOrder(product: Product): String {
        val suggestedQty = max(10.0, product.minThreshold * 2.0)
        val skuLine = if (product.sku.isNotBlank()) "رمز الصنف (SKU): ${product.sku}\n" else ""
        val supplierGreeting = if (product.supplierName.isNotBlank()) {
            "السلام عليكم ${product.supplierName}،"
        } else {
            "السلام عليكم،"
        }
        return buildString {
            appendLine(supplierGreeting)
            appendLine("نرغب بطلب توريد عاجل للمنتج التالي لضمان استمرارية المبيعات:")
            appendLine("• المنتج: ${product.name}")
            if (skuLine.isNotEmpty()) append(skuLine)
            appendLine("• الرصيد المتبقي حالياً: ${product.formattedQuantity()} ${product.unit}")
            appendLine("• الكمية المطلوب توريدها: ${Product.formatNumber(suggestedQty)} ${product.unit}")
            appendLine("يرجى تزويدنا بتأكيد التوفر وموعد التسليم. شكراً لكم.")
        }.trim()
    }
}
