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

    @Test
    fun testFinancialCalculatorVatModes() {
        // 1. Tax Exclusive: 1000 + 15% = 1150
        val excl = com.example.util.FinancialCalculator.calculateLineItem(
            quantity = 2.0,
            unitPrice = 500.0,
            discount = 0.0,
            taxRate = 15.0,
            taxMode = "غير شامل الضريبة"
        )
        assertEquals(1000.0, excl.net, 0.001)
        assertEquals(150.0, excl.tax, 0.001)
        assertEquals(1150.0, excl.total, 0.001)

        // 2. Tax Inclusive: 1150 inclusive of 15% => Net 1000.00, Tax 150.00
        val incl = com.example.util.FinancialCalculator.calculateLineItem(
            quantity = 1.0,
            unitPrice = 1150.0,
            discount = 0.0,
            taxRate = 15.0,
            taxMode = "شامل الضريبة"
        )
        assertEquals(1000.0, incl.net, 0.001)
        assertEquals(150.0, incl.tax, 0.001)
        assertEquals(1150.0, incl.total, 0.001)

        // 3. Exempt
        val exempt = com.example.util.FinancialCalculator.calculateLineItem(
            quantity = 3.0,
            unitPrice = 100.0,
            discount = 50.0,
            taxRate = 15.0,
            taxMode = "معفى"
        )
        assertEquals(250.0, exempt.net, 0.001)
        assertEquals(0.0, exempt.tax, 0.001)
        assertEquals(250.0, exempt.total, 0.001)
    }

    @Test
    fun testVatSummaryAndPayrollMath() {
        val txs = listOf(
            com.example.data.model.BusinessTransaction(
                type = "مبيعات",
                subtotal = 2000.0,
                taxAmount = 300.0,
                grandTotal = 2300.0
            ),
            com.example.data.model.BusinessTransaction(
                type = "مشتريات",
                subtotal = 800.0,
                taxAmount = 120.0,
                grandTotal = 920.0
            )
        )
        val summary = com.example.util.FinancialCalculator.calculateVatSummary(txs)
        assertEquals(300.0, summary.outputVat, 0.001)
        assertEquals(120.0, summary.inputVat, 0.001)
        assertEquals(180.0, summary.netVatPayable, 0.001)
        assertEquals(1200.0, summary.grossOperatingProfit, 0.001)

        val pay = com.example.util.FinancialCalculator.calculatePayroll(
            baseSalary = 5000.0,
            allowances = 1500.0,
            bonuses = 500.0,
            deductions = 450.0
        )
        assertEquals(7000.0, pay.totalEarnings, 0.001)
        assertEquals(6550.0, pay.netSalary, 0.001)
    }

    @Test
    fun testCsvRoundtripForTransactionsAndEmployees() {
        val txTable = com.example.util.ExcelCsvUtils.transactionsTemplateTable()
        val txOut = java.io.ByteArrayOutputStream()
        com.example.util.ExcelCsvUtils.writeCsv(txOut, txTable.first, txTable.second)
        val parsedRows = com.example.util.ExcelCsvUtils.parseCsvText(txOut.toString("UTF-8"))
        val importedTx = com.example.util.ExcelCsvUtils.parseTransactionsFromRows(parsedRows, "CSV")
        assertEquals(2, importedTx.items.size)
        assertEquals("مبيعات", importedTx.items[0].type)
        assertEquals("مشتريات", importedTx.items[1].type)

        val empTable = com.example.util.ExcelCsvUtils.employeesTemplateTable()
        val empOut = java.io.ByteArrayOutputStream()
        com.example.util.ExcelCsvUtils.writeCsv(empOut, empTable.first, empTable.second)
        val empRows = com.example.util.ExcelCsvUtils.parseCsvText(empOut.toString("UTF-8"))
        val importedEmp = com.example.util.ExcelCsvUtils.parseEmployeesFromRows(empRows, "CSV")
        assertEquals(1, importedEmp.items.size)
        assertEquals("خالد الحربي", importedEmp.items[0].name)
        assertEquals(4500.0, importedEmp.items[0].baseSalary, 0.001)
    }

    @Test
    fun testMonthlyVolumeAggregationAndDateFiltering() {
        val txs = listOf(
            com.example.data.model.BusinessTransaction(
                type = "مبيعات",
                date = "2026-09-10",
                subtotal = 1000.0,
                taxAmount = 150.0,
                grandTotal = 1150.0
            ),
            com.example.data.model.BusinessTransaction(
                type = "مبيعات",
                date = "2026-10-05",
                subtotal = 2000.0,
                taxAmount = 300.0,
                grandTotal = 2300.0
            ),
            com.example.data.model.BusinessTransaction(
                type = "مشتريات",
                date = "2026-10-12",
                subtotal = 500.0,
                taxAmount = 75.0,
                grandTotal = 575.0
            ),
            com.example.data.model.BusinessTransaction(
                type = "مبيعات",
                date = "2026-11-15",
                subtotal = 3000.0,
                taxAmount = 450.0,
                grandTotal = 3450.0
            )
        )

        val allMonths = com.example.util.FinancialCalculator.aggregateMonthlyVolume(txs)
        assertEquals(3, allMonths.size)
        assertEquals("2026-09", allMonths[0].monthKey)
        assertEquals("2026-10", allMonths[1].monthKey)
        assertEquals(2300.0, allMonths[1].salesTotal, 0.001)
        assertEquals(575.0, allMonths[1].purchasesTotal, 0.001)
        assertEquals(1500.0, allMonths[1].operatingProfit, 0.001)

        val filtered = com.example.util.FinancialCalculator.aggregateMonthlyVolume(
            transactions = txs,
            startDate = "2026-10-01",
            endDate = "2026-10-31"
        )
        assertEquals(1, filtered.size)
        assertEquals("2026-10", filtered[0].monthKey)
        assertEquals(2, filtered[0].transactionCount)
    }

    @Test
    fun testProductLowStockAlertsAndWhatsAppOrder() {
        val healthy = com.example.data.model.Product(
            name = "سماعة بلوتوث",
            sku = "AUD-10",
            unit = "حبة",
            quantity = 15.0,
            minThreshold = 5.0,
            alertsEnabled = true
        )
        assertEquals(false, healthy.isLowStock())
        assertEquals(false, healthy.isOutOfStock())
        assertEquals("متوفر", healthy.stockStatusLabel())

        val low = healthy.copy(quantity = 4.0, supplierName = "مورد الرياض")
        assertEquals(true, low.isLowStock())
        assertEquals(false, low.isOutOfStock())
        assertEquals("مخزون منخفض", low.stockStatusLabel())

        val muted = low.copy(alertsEnabled = false)
        assertEquals(false, muted.isLowStock())

        val empty = low.copy(quantity = 0.0)
        assertEquals(true, empty.isOutOfStock())
        assertEquals("نفد المخزون", empty.stockStatusLabel())

        val orderMsg = com.example.util.StockAlertNotifier.buildLowStockWhatsAppOrder(low)
        assertTrue(orderMsg.contains("سماعة بلوتوث"))
        assertTrue(orderMsg.contains("AUD-10"))
        assertTrue(orderMsg.contains("مورد الرياض"))

        val prodTable = com.example.util.ExcelCsvUtils.productsTemplateTable()
        val prodOut = java.io.ByteArrayOutputStream()
        com.example.util.ExcelCsvUtils.writeCsv(prodOut, prodTable.first, prodTable.second)
        val parsedProdRows = com.example.util.ExcelCsvUtils.parseCsvText(prodOut.toString("UTF-8"))
        val importedProducts = com.example.util.ExcelCsvUtils.parseProductsFromRows(parsedProdRows, "CSV")
        assertEquals(1, importedProducts.items.size)
        assertEquals("شاحن سريع Type-C", importedProducts.items[0].name)
        assertEquals(true, importedProducts.items[0].isLowStock())
    }
}
