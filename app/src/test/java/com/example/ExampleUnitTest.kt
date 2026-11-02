package com.example

import com.example.data.model.AdPost
import com.example.data.model.Client
import com.example.data.model.MessageTemplate
import com.example.data.model.Quote
import com.example.data.model.QuoteItem
import com.example.util.GoogleSheetsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ExampleUnitTest {

    @Test
    fun testClientModelDefaults() {
        val client = Client(
            name = "محمد القحطاني",
            phone = "0501234567",
            source = "حراج",
            status = "جديد"
        )
        assertEquals("محمد القحطاني", client.name)
        assertTrue(Client.SOURCES.contains(client.source))
        assertTrue(Client.STATUSES.contains(client.status))
    }

    @Test
    fun testAdPostModelDefaults() {
        val ad = AdPost(
            title = "ساعة ذكية فاخرة",
            price = "350",
            description = "ساعة جديدة بالكرتون",
            city = "الرياض"
        )
        assertEquals(7L, ad.renewalDays)
        assertEquals("لم ينشر", ad.status)
        assertTrue(AdPost.PLATFORMS.contains("حراج"))
        assertTrue(AdPost.PLATFORMS.contains("تيك توك"))
    }

    @Test
    fun testDefaultTemplatesGeneration() {
        val templates = MessageTemplate.defaultTemplates("user_123")
        assertTrue(templates.isNotEmpty())
        assertEquals(5, templates.size)
        assertTrue(templates.any { it.type == "ترحيب" })
        assertTrue(templates.any { it.type == "متابعة" })
        assertTrue(templates.any { it.type == "عرض سعر" })
    }

    @Test
    fun testOverdueDaysCalculation() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val fourDaysAgoMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4)
        val fourDaysAgoDateStr = dateFormat.format(Date(fourDaysAgoMillis))

        val client = Client(
            name = "عبدالله",
            phone = "0559998877",
            status = "مهتم",
            lastContactDate = fourDaysAgoDateStr
        )

        val parsedDate = dateFormat.parse(client.lastContactDate)
        assertNotNull(parsedDate)
        val diffDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - parsedDate!!.time)
        assertTrue(diffDays >= 3)
    }

    @Test
    fun testQuotePrebuiltTemplatesAndCsv() {
        val templates = Quote.getPrebuiltTemplates()
        assertTrue(templates.isNotEmpty())
        assertEquals(3, templates.size)

        val items = templates[0].second
        val quote = Quote(
            quoteNumber = "Q-TEST-001",
            clientName = "شركة النخبة",
            clientPhone = "0500000000",
            date = "2026-11-01",
            items = items.map { it.toMap() },
            subtotal = items.sumOf { it.total },
            taxRate = 15.0,
            taxAmount = items.sumOf { it.total } * 0.15,
            grandTotal = items.sumOf { it.total } * 1.15
        )

        val csv = GoogleSheetsUtils.quoteToCsv(quote)
        assertTrue(csv.contains("Q-TEST-001"))
        assertTrue(csv.contains("شركة النخبة"))

        val text = GoogleSheetsUtils.formatQuoteText(quote)
        assertTrue(text.contains("عرض سعر رسمي"))
        assertTrue(text.contains("Q-TEST-001"))
    }
}
