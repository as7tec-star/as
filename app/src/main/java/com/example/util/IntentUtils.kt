package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object IntentUtils {

    fun openWhatsApp(context: Context, phone: String, message: String) {
        try {
            // Clean phone number: remove +, spaces, dashes
            var cleanPhone = phone.replace(Regex("[^0-9]"), "")
            // Normalize Saudi numbers if starting with 05...
            if (cleanPhone.startsWith("05") && cleanPhone.length == 10) {
                cleanPhone = "966" + cleanPhone.substring(1)
            } else if (cleanPhone.startsWith("5") && cleanPhone.length == 9) {
                cleanPhone = "966$cleanPhone"
            }

            val encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val url = if (cleanPhone.isNotBlank()) {
                "https://wa.me/$cleanPhone?text=$encodedMsg"
            } else {
                "https://wa.me/?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGmailDraft(context: Context, recipientEmail: String, subject: String, body: String) {
        try {
            val emailUri = if (recipientEmail.isNotBlank()) "mailto:$recipientEmail" else "mailto:"
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(emailUri)).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق البريد: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createCalendarEvent(context: Context, clientName: String, notes: String) {
        try {
            val startTime = System.currentTimeMillis() + (60 * 60 * 1000) // 1 hour from now
            val endTime = startTime + (30 * 60 * 1000) // 30 minutes duration

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "متابعة العميل: $clientName")
                putExtra(CalendarContract.Events.DESCRIPTION, "ملاحظات وتفاصيل المتابعة: $notes")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق التقويم: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createReminderOrTask(context: Context, clientName: String, phone: String, notes: String) {
        try {
            // Android Tasks / Note intent
            val taskText = "تذكير بمتابعة العميل: $clientName\nرقم الهاتف: $phone\nالملاحظات: $notes"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, taskText)
                putExtra(Intent.EXTRA_TITLE, "مهمة: متابعة $clientName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "حفظ التذكير في تطبيق المهام أو الملاحظات").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق التذكيرات: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPlatformUrl(context: Context, platform: String, postUrl: String = "") {
        try {
            val url = if (postUrl.isNotBlank() && (postUrl.startsWith("http://") || postUrl.startsWith("https://"))) {
                postUrl
            } else {
                when (platform) {
                    "حراج" -> "https://haraj.com.sa"
                    "مرجان" -> "https://mourjan.com"
                    "تيك توك" -> "https://www.tiktok.com"
                    "فيسبوك" -> "https://www.facebook.com"
                    "واتساب" -> "https://web.whatsapp.com"
                    else -> "https://google.com"
                }
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح رابط المنصة: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "نص الإعلان") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ النص إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
    }
}
