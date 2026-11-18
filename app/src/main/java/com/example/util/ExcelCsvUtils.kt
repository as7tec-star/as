package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.data.model.BusinessTransaction
import com.example.data.model.Client
import com.example.data.model.Employee
import com.example.data.model.PayrollRecord
import com.example.data.model.Product
import com.example.data.model.TransactionItem
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed class ExcelCell {
    data class Text(val value: String) : ExcelCell()
    data class Number(val value: Double) : ExcelCell()
}

data class ImportResult<T>(
    val items: List<T>,
    val skippedRows: Int,
    val detectedFormat: String
)

object ExcelCsvUtils {

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    // =========================================================================
    // 1. PURE KOTLIN .XLSX (OOXML) WRITER WITH ARABIC RTL & NUMERIC SUPPORT
    // =========================================================================

    fun writeXlsx(
        outputStream: OutputStream,
        sheetName: String,
        headers: List<String>,
        rows: List<List<ExcelCell>>
    ) {
        val safeSheetName = sheetName.replace(Regex("[\\\\/?*\\[\\]:]"), "").take(30).ifBlank { "Sheet1" }
        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", relsXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml(safeSheetName))
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(headers, rows))
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colIndexToLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    private fun colLetterToIndex(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }.uppercase(Locale.ENGLISH)
        var result = 0
        for (ch in letters) {
            result = result * 26 + (ch - 'A' + 1)
        }
        return (result - 1).coerceAtLeast(0)
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun relsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun workbookXml(sheetName: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1A365D"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""

    private fun worksheetXml(headers: List<String>, rows: List<List<ExcelCell>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetViews><sheetView workbookViewId="0" rightToLeft="1"/></sheetViews>""")
        sb.append("<sheetData>")

        // Header Row (Row 1, style 1)
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { colIdx, h ->
            val ref = "${colIndexToLetter(colIdx)}1"
            sb.append("<c r=\"$ref\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>")
        }
        sb.append("</row>")

        // Data Rows
        rows.forEachIndexed { rowIdx, row ->
            val rowNum = rowIdx + 2
            sb.append("<row r=\"$rowNum\">")
            row.forEachIndexed { colIdx, cell ->
                val ref = "${colIndexToLetter(colIdx)}$rowNum"
                when (cell) {
                    is ExcelCell.Text -> {
                        sb.append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${escapeXml(cell.value)}</t></is></c>")
                    }
                    is ExcelCell.Number -> {
                        val numStr = if (cell.value.isNaN() || cell.value.isInfinite()) "0" else cell.value.toString()
                        sb.append("<c r=\"$ref\" t=\"n\"><v>$numStr</v></c>")
                    }
                }
            }
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // =========================================================================
    // 2. PURE KOTLIN .CSV (UTF-8 WITH BOM FOR EXCEL & GOOGLE SHEETS)
    // =========================================================================

    fun writeCsv(
        outputStream: OutputStream,
        headers: List<String>,
        rows: List<List<ExcelCell>>
    ) {
        outputStream.write(UTF8_BOM)
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")
        for (row in rows) {
            sb.append(
                row.joinToString(",") { cell ->
                    when (cell) {
                        is ExcelCell.Text -> escapeCsv(cell.value)
                        is ExcelCell.Number -> escapeCsv(
                            if (cell.value % 1.0 == 0.0) cell.value.toLong().toString()
                            else FinancialCalculator.round2(cell.value).toString()
                        )
                    }
                }
            ).append("\r\n")
        }
        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    // =========================================================================
    // 3. FILE READERS (.XLSX AND .CSV)
    // =========================================================================

    fun readRowsFromUri(context: Context, uri: Uri): Pair<String, List<List<String>>> {
        val mime = context.contentResolver.getType(uri) ?: ""
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("تعذر فتح الملف المحدد.")

        // Detect ZIP/XLSX magic bytes ("PK")
        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        return if (isZip || mime.contains("spreadsheetml")) {
            "Excel (.xlsx)" to parseXlsxBytes(bytes)
        } else {
            "CSV" to parseCsvText(String(bytes, Charsets.UTF_8))
        }
    }

    fun parseXlsxBytes(bytes: ByteArray): List<List<String>> {
        var sharedStringsBytes: ByteArray? = null
        var sheet1Bytes: ByteArray? = null

        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase(Locale.ENGLISH)
                if (name == "xl/sharedstrings.xml") {
                    sharedStringsBytes = readEntryBytes(zis)
                } else if (name == "xl/worksheets/sheet1.xml") {
                    sheet1Bytes = readEntryBytes(zis)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val targetSheet = sheet1Bytes
            ?: throw IllegalStateException("لم يتم العثور على ورقة بيانات صالحة داخل ملف Excel.")

        val sharedStrings = if (sharedStringsBytes != null) {
            parseSharedStrings(sharedStringsBytes!!.inputStream())
        } else {
            emptyList()
        }

        return parseSheetXml(targetSheet.inputStream(), sharedStrings)
    }

    private fun readEntryBytes(zis: ZipInputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(4096)
        var n: Int
        while (zis.read(data).also { n = it } != -1) {
            buffer.write(data, 0, n)
        }
        return buffer.toByteArray()
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var event = parser.eventType
        var insideSi = false
        val currentText = StringBuilder()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        insideSi = true
                        currentText.clear()
                    } else if (insideSi && parser.name == "t") {
                        currentText.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si" && insideSi) {
                        strings.add(currentText.toString())
                        insideSi = false
                    }
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var event = parser.eventType
        var currentRow: MutableMap<Int, String>? = null
        var currentColIdx = 0
        var currentType = ""
        var currentVal = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                        }
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            currentColIdx = if (ref.isNotBlank()) colLetterToIndex(ref) else (currentRow?.size ?: 0)
                            currentType = parser.getAttributeValue(null, "t") ?: ""
                            currentVal = ""
                        }
                        "v" -> {
                            currentVal = parser.nextText()
                        }
                        "t" -> {
                            if (currentType == "inlineStr") {
                                currentVal += parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val resolved = when (currentType) {
                                "s" -> {
                                    val idx = currentVal.trim().toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else currentVal
                                }
                                else -> currentVal
                            }
                            currentRow?.put(currentColIdx, resolved.trim())
                        }
                        "row" -> {
                            val map = currentRow
                            if (map != null && map.isNotEmpty()) {
                                val maxCol = map.keys.maxOrNull() ?: 0
                                val list = (0..maxCol).map { map[it] ?: "" }
                                if (list.any { it.isNotBlank() }) {
                                    rows.add(list)
                                }
                            }
                            currentRow = null
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    fun parseCsvText(rawText: String): List<List<String>> {
        val text = rawText.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        // Detect delimiter from first line if comma vs semicolon
        val firstLine = text.lineSequence().firstOrNull() ?: ""
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' } && !firstLine.contains(",")) ';' else ','

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') i++
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
        }

        return rows
    }

    // =========================================================================
    // 4. DOMAIN EXPORT & TEMPLATE DEFINITIONS
    // =========================================================================

    fun transactionsTable(transactions: List<BusinessTransaction>): Pair<List<String>, List<List<ExcelCell>>> {
        val headers = listOf(
            "النوع", "رقم الفاتورة", "العميل / المورد", "الهاتف", "الرقم الضريبي",
            "التصنيف", "التاريخ", "طريقة الدفع", "حالة الدفع", "نظام الضريبة",
            "نسبة الضريبة %", "الصافي قبل الضريبة", "قيمة الضريبة", "الإجمالي النهائي", "ملاحظات"
        )
        val rows = transactions.map { tx ->
            listOf(
                ExcelCell.Text(tx.type),
                ExcelCell.Text(tx.invoiceNumber),
                ExcelCell.Text(tx.partyName),
                ExcelCell.Text(tx.partyPhone),
                ExcelCell.Text(tx.partyTaxNumber),
                ExcelCell.Text(tx.category),
                ExcelCell.Text(tx.date),
                ExcelCell.Text(tx.paymentMethod),
                ExcelCell.Text(tx.paymentStatus),
                ExcelCell.Text(tx.taxMode),
                ExcelCell.Number(tx.taxRate),
                ExcelCell.Number(tx.subtotal),
                ExcelCell.Number(tx.taxAmount),
                ExcelCell.Number(tx.grandTotal),
                ExcelCell.Text(tx.notes)
            )
        }
        return headers to rows
    }

    fun transactionsTemplateTable(): Pair<List<String>, List<List<ExcelCell>>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val sample = listOf(
            BusinessTransaction(
                type = "مبيعات",
                invoiceNumber = "INV-1001",
                partyName = "شركة المثال التجارية",
                partyPhone = "0501234567",
                partyTaxNumber = "300012345600003",
                category = "بضاعة",
                date = today,
                paymentMethod = "تحويل بنكي",
                paymentStatus = "مدفوع",
                taxMode = "غير شامل الضريبة",
                taxRate = 15.0,
                subtotal = 1000.0,
                taxAmount = 150.0,
                grandTotal = 1150.0,
                notes = "مثال فاتورة مبيعات - يمكنك تعديل أو إضافة صفوف"
            ),
            BusinessTransaction(
                type = "مشتريات",
                invoiceNumber = "PUR-2001",
                partyName = "مورد الجملة",
                partyPhone = "0559876543",
                partyTaxNumber = "",
                category = "بضاعة",
                date = today,
                paymentMethod = "نقدي",
                paymentStatus = "مدفوع",
                taxMode = "شامل الضريبة",
                taxRate = 15.0,
                subtotal = 500.0,
                taxAmount = 75.0,
                grandTotal = 575.0,
                notes = "مثال فاتورة مشتريات"
            )
        )
        return transactionsTable(sample)
    }

    fun clientsTable(clients: List<Client>): Pair<List<String>, List<List<ExcelCell>>> {
        val headers = listOf("الاسم", "الهاتف", "البريد الإلكتروني", "المصدر", "الحالة", "قيمة الصفقة", "آخر تواصل", "ملاحظات")
        val rows = clients.map { c ->
            listOf(
                ExcelCell.Text(c.name),
                ExcelCell.Text(c.phone),
                ExcelCell.Text(c.email),
                ExcelCell.Text(c.source),
                ExcelCell.Text(c.status),
                ExcelCell.Number(c.dealValue),
                ExcelCell.Text(c.lastContactDate),
                ExcelCell.Text(c.notes)
            )
        }
        return headers to rows
    }

    fun clientsTemplateTable(): Pair<List<String>, List<List<ExcelCell>>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        return clientsTable(
            listOf(
                Client(
                    name = "أحمد السعيد",
                    phone = "0501112233",
                    email = "ahmed@example.com",
                    source = "حراج",
                    status = "مهتم",
                    dealValue = 1500.0,
                    lastContactDate = today,
                    notes = "عميل مهتم بالباقة الذهبية"
                )
            )
        )
    }

    fun employeesTable(employees: List<Employee>): Pair<List<String>, List<List<ExcelCell>>> {
        val headers = listOf(
            "الاسم", "المسمى الوظيفي", "القسم", "الهاتف", "البريد الإلكتروني",
            "رقم الهوية", "تاريخ التعيين", "الراتب الأساسي", "بدل السكن",
            "بدل النقل", "بدلات أخرى", "الاستقطاعات", "صافي الراتب التقديري", "الحالة"
        )
        val rows = employees.map { e ->
            listOf(
                ExcelCell.Text(e.name),
                ExcelCell.Text(e.jobTitle),
                ExcelCell.Text(e.department),
                ExcelCell.Text(e.phone),
                ExcelCell.Text(e.email),
                ExcelCell.Text(e.nationalId),
                ExcelCell.Text(e.hireDate),
                ExcelCell.Number(e.baseSalary),
                ExcelCell.Number(e.housingAllowance),
                ExcelCell.Number(e.transportAllowance),
                ExcelCell.Number(e.otherAllowances),
                ExcelCell.Number(e.defaultDeductions),
                ExcelCell.Number(e.estimatedNetSalary()),
                ExcelCell.Text(e.status)
            )
        }
        return headers to rows
    }

    fun employeesTemplateTable(): Pair<List<String>, List<List<ExcelCell>>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        return employeesTable(
            listOf(
                Employee(
                    name = "خالد الحربي",
                    jobTitle = "مندوب مبيعات",
                    department = "المبيعات",
                    phone = "0552223344",
                    email = "khalid@example.com",
                    nationalId = "1023456789",
                    hireDate = today,
                    baseSalary = 4500.0,
                    housingAllowance = 1000.0,
                    transportAllowance = 500.0,
                    otherAllowances = 0.0,
                    defaultDeductions = 400.0,
                    status = "نشط"
                )
            )
        )
    }

    fun payrollTable(records: List<PayrollRecord>): Pair<List<String>, List<List<ExcelCell>>> {
        val headers = listOf(
            "الشهر", "اسم الموظف", "المسمى الوظيفي", "الهاتف",
            "الراتب الأساسي", "البدلات", "المكافآت", "الخصومات",
            "صافي الراتب", "حالة الصرف", "تاريخ الصرف", "ملاحظات"
        )
        val rows = records.map { r ->
            listOf(
                ExcelCell.Text(r.monthYear),
                ExcelCell.Text(r.employeeName),
                ExcelCell.Text(r.jobTitle),
                ExcelCell.Text(r.employeePhone),
                ExcelCell.Number(r.baseSalary),
                ExcelCell.Number(r.allowances),
                ExcelCell.Number(r.bonuses),
                ExcelCell.Number(r.deductions),
                ExcelCell.Number(r.netSalary),
                ExcelCell.Text(r.paymentStatus),
                ExcelCell.Text(r.paymentDate),
                ExcelCell.Text(r.notes)
            )
        }
        return headers to rows
    }

    fun vatSummaryTable(summary: VatSummary): Pair<List<String>, List<List<ExcelCell>>> {
        val headers = listOf("البند الضريبي / المالي", "العدد", "صافي المبلغ قبل الضريبة", "ضريبة القيمة المضافة (VAT)", "الإجمالي شامل الضريبة")
        val rows = listOf(
            listOf(
                ExcelCell.Text("إجمالي المبيعات (ضريبة المخرجات)"),
                ExcelCell.Number(summary.salesCount.toDouble()),
                ExcelCell.Number(summary.salesNet),
                ExcelCell.Number(summary.outputVat),
                ExcelCell.Number(summary.salesTotal)
            ),
            listOf(
                ExcelCell.Text("إجمالي المشتريات (ضريبة المدخلات)"),
                ExcelCell.Number(summary.purchasesCount.toDouble()),
                ExcelCell.Number(summary.purchasesNet),
                ExcelCell.Number(summary.inputVat),
                ExcelCell.Number(summary.purchasesTotal)
            ),
            listOf(
                ExcelCell.Text("صافي الضريبة المستحقة / (المستردة)"),
                ExcelCell.Text("-"),
                ExcelCell.Text("-"),
                ExcelCell.Number(summary.netVatPayable),
                ExcelCell.Text("-")
            ),
            listOf(
                ExcelCell.Text("مجمل الربح التشغيلي (الصافي)"),
                ExcelCell.Text("-"),
                ExcelCell.Number(summary.grossOperatingProfit),
                ExcelCell.Text("-"),
                ExcelCell.Text("-")
            )
        )
        return headers to rows
    }

    fun productsTable(products: List<Product>): Pair<List<String>, List<List<ExcelCell>>> {
        val headers = listOf(
            "اسم المنتج", "رمز SKU", "التصنيف", "الوحدة", "الكمية المتوفرة",
            "حد التنبيه الأدنى", "تكلفة الشراء", "سعر البيع", "اسم المورد",
            "هاتف المورد", "تفعيل التنبيهات", "حالة المخزون", "ملاحظات"
        )
        val rows = products.map { p ->
            listOf(
                ExcelCell.Text(p.name),
                ExcelCell.Text(p.sku),
                ExcelCell.Text(p.category),
                ExcelCell.Text(p.unit),
                ExcelCell.Number(p.quantity),
                ExcelCell.Number(p.minThreshold),
                ExcelCell.Number(p.unitCost),
                ExcelCell.Number(p.sellingPrice),
                ExcelCell.Text(p.supplierName),
                ExcelCell.Text(p.supplierPhone),
                ExcelCell.Text(if (p.alertsEnabled) "نعم" else "لا"),
                ExcelCell.Text(p.stockStatusLabel()),
                ExcelCell.Text(p.notes)
            )
        }
        return headers to rows
    }

    fun productsTemplateTable(): Pair<List<String>, List<List<ExcelCell>>> {
        return productsTable(
            listOf(
                Product(
                    name = "شاحن سريع Type-C",
                    sku = "CHG-001",
                    category = "إلكترونيات",
                    unit = "حبة",
                    quantity = 4.0,
                    minThreshold = 10.0,
                    unitCost = 25.0,
                    sellingPrice = 55.0,
                    supplierName = "مؤسسة التقنية الحديثة",
                    supplierPhone = "0501234567",
                    alertsEnabled = true,
                    notes = "منتج سريع الدوران"
                )
            )
        )
    }

    // =========================================================================
    // 5. SMART IMPORT PARSERS (MAP ROWS BACK TO ENTITIES WITH PRECISION MATH)
    // =========================================================================

    fun parseTransactionsFromRows(rawRows: List<List<String>>, format: String): ImportResult<BusinessTransaction> {
        if (rawRows.size < 2) return ImportResult(emptyList(), 0, format)
        val header = rawRows.first().map { it.trim().lowercase(Locale.ENGLISH) }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        fun col(vararg aliases: String): Int {
            return header.indexOfFirst { h -> aliases.any { h.contains(it.lowercase(Locale.ENGLISH)) } }
        }

        val typeIdx = col("النوع", "type")
        val numIdx = col("رقم الفاتورة", "رقم", "invoice")
        val partyIdx = col("العميل", "المورد", "party", "name", "الاسم")
        val phoneIdx = col("الهاتف", "جوال", "phone")
        val taxNumIdx = col("الرقم الضريبي", "tax id", "vat")
        val catIdx = col("التصنيف", "category")
        val dateIdx = col("التاريخ", "date")
        val payMethodIdx = col("طريقة الدفع", "method")
        val payStatusIdx = col("حالة الدفع", "status", "الحالة")
        val taxModeIdx = col("نظام الضريبة", "tax mode")
        val taxRateIdx = col("نسبة الضريبة", "tax rate", "rate")
        val subtotalIdx = col("الصافي", "subtotal", "المبلغ", "amount")
        val totalIdx = col("الإجمالي", "total")
        val notesIdx = col("ملاحظات", "notes")

        val imported = mutableListOf<BusinessTransaction>()
        var skipped = 0

        rawRows.drop(1).forEachIndexed { idx, row ->
            fun cell(i: Int): String = if (i in row.indices) row[i].trim() else ""
            fun num(i: Int): Double = cell(i).replace(",", "").toDoubleOrNull() ?: 0.0

            val party = cell(partyIdx).ifBlank { cell(0) }
            if (party.isBlank()) {
                skipped++
                return@forEachIndexed
            }

            val rawType = cell(typeIdx)
            val txType = if (rawType.contains("مشتريات") || rawType.equals("purchase", true)) "مشتريات" else "مبيعات"
            val invNum = cell(numIdx).ifBlank {
                (if (txType == "مبيعات") "INV-" else "PUR-") + "${System.currentTimeMillis() % 100000}-${idx + 1}"
            }
            val rawTaxMode = cell(taxModeIdx)
            val taxMode = when {
                rawTaxMode.contains("شامل") && !rawTaxMode.contains("غير") -> "شامل الضريبة"
                rawTaxMode.contains("معفى") || rawTaxMode.contains("exempt", true) -> "معفى"
                else -> "غير شامل الضريبة"
            }
            val taxRate = if (taxMode == "معفى") 0.0 else (cell(taxRateIdx).replace("%", "").toDoubleOrNull() ?: 15.0)

            val rawSubtotal = num(subtotalIdx)
            val rawTotal = num(totalIdx)
            val amountBase = when {
                taxMode == "شامل الضريبة" && rawTotal > 0 -> rawTotal
                rawSubtotal > 0 -> rawSubtotal
                rawTotal > 0 -> rawTotal
                else -> 0.0
            }

            val calc = FinancialCalculator.calculateLineItem(
                quantity = 1.0,
                unitPrice = amountBase,
                discount = 0.0,
                taxRate = taxRate,
                taxMode = if (taxMode != "شامل الضريبة" && rawSubtotal <= 0.0 && rawTotal > 0.0) "شامل الضريبة" else taxMode
            )

            val lineItem = TransactionItem(
                name = cell(catIdx).ifBlank { "بند مستورد" },
                quantity = 1.0,
                unitPrice = amountBase,
                discount = 0.0,
                netAmount = calc.net,
                taxAmount = calc.tax,
                total = calc.total
            )

            val payMethod = cell(payMethodIdx).let {
                if (it in BusinessTransaction.PAYMENT_METHODS) it else "تحويل بنكي"
            }
            val payStatus = cell(payStatusIdx).let {
                if (it in BusinessTransaction.PAYMENT_STATUSES) it else "مدفوع"
            }

            imported.add(
                BusinessTransaction(
                    type = txType,
                    invoiceNumber = invNum,
                    partyName = party,
                    partyPhone = cell(phoneIdx),
                    partyTaxNumber = cell(taxNumIdx),
                    category = cell(catIdx).ifBlank { "بضاعة" },
                    date = cell(dateIdx).ifBlank { today },
                    paymentMethod = payMethod,
                    paymentStatus = payStatus,
                    taxMode = taxMode,
                    taxRate = taxRate,
                    items = listOf(lineItem.toMap()),
                    subtotal = calc.net,
                    discountTotal = 0.0,
                    taxAmount = calc.tax,
                    grandTotal = calc.total,
                    notes = cell(notesIdx)
                )
            )
        }

        return ImportResult(imported, skipped, format)
    }

    fun parseClientsFromRows(rawRows: List<List<String>>, format: String): ImportResult<Client> {
        if (rawRows.size < 2) return ImportResult(emptyList(), 0, format)
        val header = rawRows.first().map { it.trim().lowercase(Locale.ENGLISH) }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        fun col(vararg aliases: String): Int =
            header.indexOfFirst { h -> aliases.any { h.contains(it.lowercase(Locale.ENGLISH)) } }

        val nameIdx = col("الاسم", "name")
        val phoneIdx = col("الهاتف", "جوال", "phone")
        val emailIdx = col("البريد", "email")
        val sourceIdx = col("المصدر", "source")
        val statusIdx = col("الحالة", "status")
        val valueIdx = col("قيمة", "value", "deal")
        val dateIdx = col("تواصل", "date")
        val notesIdx = col("ملاحظات", "notes")

        val list = mutableListOf<Client>()
        var skipped = 0

        rawRows.drop(1).forEach { row ->
            fun cell(i: Int): String = if (i in row.indices) row[i].trim() else ""
            val name = cell(if (nameIdx >= 0) nameIdx else 0)
            if (name.isBlank()) {
                skipped++
                return@forEach
            }
            val source = cell(sourceIdx).let { if (it in Client.SOURCES) it else "أخرى" }
            val status = cell(statusIdx).let { if (it in Client.STATUSES) it else "جديد" }
            val value = cell(valueIdx).replace(",", "").toDoubleOrNull() ?: 0.0

            list.add(
                Client(
                    name = name,
                    phone = cell(if (phoneIdx >= 0) phoneIdx else 1),
                    email = cell(emailIdx),
                    source = source,
                    status = status,
                    dealValue = value,
                    lastContactDate = cell(dateIdx).ifBlank { today },
                    notes = cell(notesIdx)
                )
            )
        }
        return ImportResult(list, skipped, format)
    }

    fun parseEmployeesFromRows(rawRows: List<List<String>>, format: String): ImportResult<Employee> {
        if (rawRows.size < 2) return ImportResult(emptyList(), 0, format)
        val header = rawRows.first().map { it.trim().lowercase(Locale.ENGLISH) }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        fun col(vararg aliases: String): Int =
            header.indexOfFirst { h -> aliases.any { h.contains(it.lowercase(Locale.ENGLISH)) } }

        val nameIdx = col("الاسم", "name")
        val jobIdx = col("المسمى", "الوظيفة", "job", "title")
        val deptIdx = col("القسم", "department")
        val phoneIdx = col("الهاتف", "جوال", "phone")
        val emailIdx = col("البريد", "email")
        val idIdx = col("الهوية", "national")
        val hireIdx = col("التعيين", "hire")
        val baseIdx = col("الأساسي", "الراتب الأساسي", "base")
        val housingIdx = col("السكن", "housing")
        val transportIdx = col("النقل", "transport")
        val otherIdx = col("بدلات أخرى", "other")
        val deductIdx = col("الاستقطاعات", "الخصم", "deduction")
        val statusIdx = col("الحالة", "status")

        val list = mutableListOf<Employee>()
        var skipped = 0

        rawRows.drop(1).forEach { row ->
            fun cell(i: Int): String = if (i in row.indices) row[i].trim() else ""
            fun num(i: Int): Double = cell(i).replace(",", "").toDoubleOrNull() ?: 0.0

            val name = cell(if (nameIdx >= 0) nameIdx else 0)
            if (name.isBlank()) {
                skipped++
                return@forEach
            }
            val job = cell(if (jobIdx >= 0) jobIdx else 1).ifBlank { "موظف" }
            val status = cell(statusIdx).let { if (it in Employee.STATUSES) it else "نشط" }

            list.add(
                Employee(
                    name = name,
                    jobTitle = job,
                    department = cell(deptIdx).ifBlank { "المبيعات" },
                    phone = cell(phoneIdx),
                    email = cell(emailIdx),
                    nationalId = cell(idIdx),
                    hireDate = cell(hireIdx).ifBlank { today },
                    baseSalary = num(baseIdx).coerceAtLeast(0.0),
                    housingAllowance = num(housingIdx).coerceAtLeast(0.0),
                    transportAllowance = num(transportIdx).coerceAtLeast(0.0),
                    otherAllowances = num(otherIdx).coerceAtLeast(0.0),
                    defaultDeductions = num(deductIdx).coerceAtLeast(0.0),
                    status = status
                )
            )
        }
        return ImportResult(list, skipped, format)
    }

    fun parseProductsFromRows(rawRows: List<List<String>>, format: String): ImportResult<Product> {
        if (rawRows.size < 2) return ImportResult(emptyList(), 0, format)
        val header = rawRows.first().map { it.trim().lowercase(Locale.ENGLISH) }

        fun col(vararg aliases: String): Int =
            header.indexOfFirst { h -> aliases.any { h.contains(it.lowercase(Locale.ENGLISH)) } }

        val nameIdx = col("اسم المنتج", "المنتج", "الاسم", "name", "product")
        val skuIdx = col("sku", "رمز", "باركود", "barcode")
        val catIdx = col("التصنيف", "category")
        val unitIdx = col("الوحدة", "unit")
        val qtyIdx = col("الكمية", "quantity", "stock", "qty")
        val minIdx = col("حد التنبيه", "الحد الأدنى", "threshold", "min")
        val costIdx = col("تكلفة", "cost")
        val priceIdx = col("سعر البيع", "السعر", "price")
        val supNameIdx = col("اسم المورد", "المورد", "supplier")
        val supPhoneIdx = col("هاتف المورد", "الهاتف", "phone")
        val alertsIdx = col("التنبيهات", "alert")
        val notesIdx = col("ملاحظات", "notes")

        val list = mutableListOf<Product>()
        var skipped = 0

        rawRows.drop(1).forEach { row ->
            fun cell(i: Int): String = if (i in row.indices) row[i].trim() else ""
            fun num(i: Int): Double = cell(i).replace(",", "").toDoubleOrNull() ?: 0.0

            val name = cell(if (nameIdx >= 0) nameIdx else 0)
            if (name.isBlank()) {
                skipped++
                return@forEach
            }

            val rawAlerts = cell(alertsIdx).lowercase(Locale.ENGLISH)
            val alertsOn = !(rawAlerts == "لا" || rawAlerts == "no" || rawAlerts == "false" || rawAlerts == "0")

            list.add(
                Product(
                    name = name,
                    sku = cell(skuIdx),
                    category = cell(catIdx).ifBlank { "بضاعة" },
                    unit = cell(unitIdx).ifBlank { "حبة" },
                    quantity = num(qtyIdx).coerceAtLeast(0.0),
                    minThreshold = if (minIdx >= 0 && cell(minIdx).isNotBlank()) num(minIdx).coerceAtLeast(0.0) else 5.0,
                    unitCost = num(costIdx).coerceAtLeast(0.0),
                    sellingPrice = num(priceIdx).coerceAtLeast(0.0),
                    supplierName = cell(supNameIdx),
                    supplierPhone = cell(supPhoneIdx),
                    alertsEnabled = alertsOn,
                    notes = cell(notesIdx)
                )
            )
        }
        return ImportResult(list, skipped, format)
    }
}
