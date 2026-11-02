package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Quote

object GoogleSheetsUtils {

    /**
     * Opens an existing Google Sheet or creates a new one via sheets.new
     */
    fun openGoogleSheet(context: Context, sheetUrl: String = "") {
        try {
            val url = if (sheetUrl.isNotBlank() && (sheetUrl.startsWith("http://") || sheetUrl.startsWith("https://"))) {
                sheetUrl
            } else {
                "https://sheets.new"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح Google Sheets: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Converts a Quote into a formatted CSV string ready for Google Sheets import
     */
    fun quoteToCsv(quote: Quote): String {
        val sb = StringBuilder()
        sb.append("رقم عرض السعر,تاريخ الإصدار,اسم العميل,رقم الهاتف,اسم البند,الوصف,الكمية,سعر الوحدة,المجموع الجزئي,الضريبة %,المجموع الكلي\n")
        val items = quote.getTypedItems()
        if (items.isEmpty()) {
            sb.append("\"${quote.quoteNumber}\",\"${quote.date}\",\"${quote.clientName}\",\"${quote.clientPhone}\",\"-\",\"-\",0,0,${quote.subtotal},${quote.taxRate}%,${quote.grandTotal}\n")
        } else {
            items.forEach { item ->
                sb.append("\"${quote.quoteNumber}\",\"${quote.date}\",\"${quote.clientName}\",\"${quote.clientPhone}\",\"${item.name}\",\"${item.description}\",${item.quantity},${item.unitPrice},${item.total},${quote.taxRate}%,${quote.grandTotal}\n")
            }
        }
        return sb.toString()
    }

    /**
     * Formats quote as a professional Arabic text quotation for WhatsApp or Email
     */
    fun formatQuoteText(quote: Quote): String {
        val sb = StringBuilder()
        sb.append("📄 *عرض سعر رسمي - مركز عملي*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🔢 رقم العرض: ${quote.quoteNumber}\n")
        sb.append("📅 التاريخ: ${quote.date}\n")
        if (quote.expiryDate.isNotBlank()) {
            sb.append("⏳ الصلاحية حتى: ${quote.expiryDate}\n")
        }
        sb.append("👤 العميل: ${quote.clientName}\n")
        if (quote.clientPhone.isNotBlank()) {
            sb.append("📞 الهاتف: ${quote.clientPhone}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📦 *تفاصيل البنود والأسعار:*\n\n")

        val items = quote.getTypedItems()
        items.forEachIndexed { index, item ->
            sb.append("${index + 1}) *${item.name}*\n")
            if (item.description.isNotBlank()) {
                sb.append("   • الوصف: ${item.description}\n")
            }
            sb.append("   • الكمية: ${item.quantity.toInt()} × ${item.unitPrice} ر.س = *${item.total} ر.س*\n\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💵 المجموع الفرعي: *${quote.subtotal} ر.س*\n")
        if (quote.taxRate > 0) {
            sb.append("📊 ضريبة القيمة المضافة (${quote.taxRate.toInt()}%): *${quote.taxAmount} ر.س*\n")
        }
        sb.append("💰 *الإجمالي النهائي: ${quote.grandTotal} ر.س*\n")

        if (quote.notes.isNotBlank()) {
            sb.append("━━━━━━━━━━━━━━━━━━━\n")
            sb.append("📝 *الشروط والملاحظات:*\n${quote.notes}\n")
        }

        if (quote.googleSheetUrl.isNotBlank()) {
            sb.append("\n📊 رابط جدول Google Sheets: ${quote.googleSheetUrl}\n")
        }

        sb.append("\nيسعدنا خدمتكم وتأكيد طلبكم في أي وقت!")
        return sb.toString()
    }

    /**
     * Shares quotation CSV or text directly with other apps or Google Drive/Sheets
     */
    fun shareQuoteCsv(context: Context, quote: Quote) {
        val csvData = quoteToCsv(quote)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvData)
            putExtra(Intent.EXTRA_SUBJECT, "بيانات عرض السعر ${quote.quoteNumber} - Google Sheets CSV")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "تصدير عرض السعر إلى Google Sheets أو التطبيقات")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }
}
