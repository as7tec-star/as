package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("مركز عملي", appName)
  }

  @Test
  fun `xlsx write and parse roundtrip preserves Arabic transactions and clients`() {
    val txTable = com.example.util.ExcelCsvUtils.transactionsTemplateTable()
    val out = java.io.ByteArrayOutputStream()
    com.example.util.ExcelCsvUtils.writeXlsx(out, "المبيعات والمشتريات", txTable.first, txTable.second)
    val rows = com.example.util.ExcelCsvUtils.parseXlsxBytes(out.toByteArray())
    val imported = com.example.util.ExcelCsvUtils.parseTransactionsFromRows(rows, "Excel (.xlsx)")
    assertEquals(2, imported.items.size)
    assertEquals("INV-1001", imported.items[0].invoiceNumber)
    assertEquals(1150.0, imported.items[0].grandTotal, 0.01)

    val clientTable = com.example.util.ExcelCsvUtils.clientsTemplateTable()
    val cOut = java.io.ByteArrayOutputStream()
    com.example.util.ExcelCsvUtils.writeXlsx(cOut, "العملاء", clientTable.first, clientTable.second)
    val cRows = com.example.util.ExcelCsvUtils.parseXlsxBytes(cOut.toByteArray())
    val cImported = com.example.util.ExcelCsvUtils.parseClientsFromRows(cRows, "Excel (.xlsx)")
    assertEquals(1, cImported.items.size)
    assertEquals("أحمد السعيد", cImported.items[0].name)
  }
}
