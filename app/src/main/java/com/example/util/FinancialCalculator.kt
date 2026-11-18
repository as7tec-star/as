package com.example.util

import com.example.data.model.BusinessTransaction
import com.example.data.model.PayrollRecord
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class LineCalcResult(
    val net: Double,
    val tax: Double,
    val total: Double
)

data class InvoiceCalcResult(
    val subtotal: Double,
    val discountTotal: Double,
    val taxAmount: Double,
    val grandTotal: Double
)

data class VatSummary(
    val salesCount: Int,
    val salesNet: Double,
    val outputVat: Double,
    val salesTotal: Double,
    val purchasesCount: Int,
    val purchasesNet: Double,
    val inputVat: Double,
    val purchasesTotal: Double,
    val netVatPayable: Double,
    val grossOperatingProfit: Double
)

data class PayrollCalcResult(
    val totalEarnings: Double,
    val totalDeductions: Double,
    val netSalary: Double
)

object FinancialCalculator {

    private val HUNDRED = BigDecimal("100")
    private val ONE = BigDecimal.ONE
    private val ZERO = BigDecimal.ZERO

    fun bd(value: Double): BigDecimal =
        BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)

    fun round2(value: BigDecimal): Double =
        value.setScale(2, RoundingMode.HALF_UP).toDouble()

    fun round2(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    /**
     * Calculates exact Net, Tax, and Total for a single line item using BigDecimal (HALF_UP).
     */
    fun calculateLineItem(
        quantity: Double,
        unitPrice: Double,
        discount: Double,
        taxRate: Double,
        taxMode: String
    ): LineCalcResult {
        val qtyBd = BigDecimal.valueOf(quantity.coerceAtLeast(0.0))
        val priceBd = BigDecimal.valueOf(unitPrice.coerceAtLeast(0.0))
        val discBd = BigDecimal.valueOf(discount.coerceAtLeast(0.0))
        val rateBd = BigDecimal.valueOf(taxRate.coerceIn(0.0, 100.0))

        val rawBase = qtyBd.multiply(priceBd).subtract(discBd).max(ZERO)

        return when (taxMode) {
            "معفى" -> {
                val net = round2(rawBase)
                LineCalcResult(net = net, tax = 0.0, total = net)
            }
            "شامل الضريبة" -> {
                // rawBase IS the tax-inclusive total
                val divisor = ONE.add(rateBd.divide(HUNDRED, 8, RoundingMode.HALF_UP))
                val netBd = if (divisor > ZERO) {
                    rawBase.divide(divisor, 4, RoundingMode.HALF_UP)
                } else {
                    rawBase
                }
                val netRounded = netBd.setScale(2, RoundingMode.HALF_UP)
                val totalRounded = rawBase.setScale(2, RoundingMode.HALF_UP)
                val taxRounded = totalRounded.subtract(netRounded).max(ZERO)
                LineCalcResult(
                    net = netRounded.toDouble(),
                    tax = taxRounded.toDouble(),
                    total = totalRounded.toDouble()
                )
            }
            else -> { // "غير شامل الضريبة"
                val netRounded = rawBase.setScale(2, RoundingMode.HALF_UP)
                val taxBd = netRounded.multiply(rateBd).divide(HUNDRED, 4, RoundingMode.HALF_UP)
                val taxRounded = taxBd.setScale(2, RoundingMode.HALF_UP)
                val totalRounded = netRounded.add(taxRounded)
                LineCalcResult(
                    net = netRounded.toDouble(),
                    tax = taxRounded.toDouble(),
                    total = totalRounded.toDouble()
                )
            }
        }
    }

    /**
     * Aggregates invoice totals from line items using exact BigDecimal summation.
     */
    fun calculateInvoiceTotals(
        items: List<com.example.data.model.TransactionItem>,
        taxRate: Double,
        taxMode: String
    ): InvoiceCalcResult {
        var subtotalBd = ZERO
        var discountBd = ZERO
        var taxBd = ZERO
        var grandTotalBd = ZERO

        for (item in items) {
            val calc = calculateLineItem(
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discount = item.discount,
                taxRate = taxRate,
                taxMode = taxMode
            )
            subtotalBd = subtotalBd.add(BigDecimal.valueOf(calc.net))
            discountBd = discountBd.add(BigDecimal.valueOf(item.discount.coerceAtLeast(0.0)))
            taxBd = taxBd.add(BigDecimal.valueOf(calc.tax))
            grandTotalBd = grandTotalBd.add(BigDecimal.valueOf(calc.total))
        }

        return InvoiceCalcResult(
            subtotal = round2(subtotalBd),
            discountTotal = round2(discountBd),
            taxAmount = round2(taxBd),
            grandTotal = round2(grandTotalBd)
        )
    }

    /**
     * Computes VAT return summary (Output VAT - Input VAT) and operating profit.
     */
    fun calculateVatSummary(transactions: List<BusinessTransaction>): VatSummary {
        var salesCount = 0
        var salesNet = ZERO
        var outputVat = ZERO
        var salesTotal = ZERO

        var purchasesCount = 0
        var purchasesNet = ZERO
        var inputVat = ZERO
        var purchasesTotal = ZERO

        for (tx in transactions) {
            if (tx.type == "مبيعات") {
                salesCount++
                salesNet = salesNet.add(BigDecimal.valueOf(tx.subtotal))
                outputVat = outputVat.add(BigDecimal.valueOf(tx.taxAmount))
                salesTotal = salesTotal.add(BigDecimal.valueOf(tx.grandTotal))
            } else if (tx.type == "مشتريات") {
                purchasesCount++
                purchasesNet = purchasesNet.add(BigDecimal.valueOf(tx.subtotal))
                inputVat = inputVat.add(BigDecimal.valueOf(tx.taxAmount))
                purchasesTotal = purchasesTotal.add(BigDecimal.valueOf(tx.grandTotal))
            }
        }

        val netVat = outputVat.subtract(inputVat)
        val grossProfit = salesNet.subtract(purchasesNet)

        return VatSummary(
            salesCount = salesCount,
            salesNet = round2(salesNet),
            outputVat = round2(outputVat),
            salesTotal = round2(salesTotal),
            purchasesCount = purchasesCount,
            purchasesNet = round2(purchasesNet),
            inputVat = round2(inputVat),
            purchasesTotal = round2(purchasesTotal),
            netVatPayable = round2(netVat),
            grossOperatingProfit = round2(grossProfit)
        )
    }

    /**
     * Computes exact Net Salary from base salary, allowances, bonuses, and deductions.
     */
    fun calculatePayroll(
        baseSalary: Double,
        allowances: Double,
        bonuses: Double,
        deductions: Double
    ): PayrollCalcResult {
        val baseBd = BigDecimal.valueOf(baseSalary.coerceAtLeast(0.0))
        val allowBd = BigDecimal.valueOf(allowances.coerceAtLeast(0.0))
        val bonusBd = BigDecimal.valueOf(bonuses.coerceAtLeast(0.0))
        val deductBd = BigDecimal.valueOf(deductions.coerceAtLeast(0.0))

        val earnings = baseBd.add(allowBd).add(bonusBd)
        val net = earnings.subtract(deductBd).max(ZERO)

        return PayrollCalcResult(
            totalEarnings = round2(earnings),
            totalDeductions = round2(deductBd),
            netSalary = round2(net)
        )
    }

    fun totalPayrollCost(records: List<PayrollRecord>): Double {
        var total = ZERO
        for (r in records) {
            total = total.add(BigDecimal.valueOf(r.netSalary))
        }
        return round2(total)
    }

    fun formatMoney(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val df = DecimalFormat("#,##0.00", symbols)
        return df.format(round2(amount))
    }

    /**
     * Aggregates Sales & Purchases volume by month (yyyy-MM) with optional yyyy-MM-dd date filtering.
     */
    fun aggregateMonthlyVolume(
        transactions: List<BusinessTransaction>,
        startDate: String? = null,
        endDate: String? = null
    ): List<MonthlyVolumePoint> {
        val start = startDate?.trim()?.takeIf { it.length >= 10 }
        val end = endDate?.trim()?.takeIf { it.length >= 10 }

        val filtered = transactions.filter { tx ->
            val d = tx.date.trim()
            if (d.length < 7) return@filter false
            val afterStart = start == null || d >= start
            val beforeEnd = end == null || d <= end
            afterStart && beforeEnd
        }

        val grouped = filtered.groupBy { it.date.trim().substring(0, 7) }
            .toSortedMap()

        return grouped.map { (monthKey, txs) ->
            var salesTotal = ZERO
            var salesNet = ZERO
            var salesVat = ZERO
            var purchasesTotal = ZERO
            var purchasesNet = ZERO
            var purchasesVat = ZERO

            for (tx in txs) {
                if (tx.type == "مبيعات") {
                    salesTotal = salesTotal.add(BigDecimal.valueOf(tx.grandTotal))
                    salesNet = salesNet.add(BigDecimal.valueOf(tx.subtotal))
                    salesVat = salesVat.add(BigDecimal.valueOf(tx.taxAmount))
                } else if (tx.type == "مشتريات") {
                    purchasesTotal = purchasesTotal.add(BigDecimal.valueOf(tx.grandTotal))
                    purchasesNet = purchasesNet.add(BigDecimal.valueOf(tx.subtotal))
                    purchasesVat = purchasesVat.add(BigDecimal.valueOf(tx.taxAmount))
                }
            }

            val netOperating = salesNet.subtract(purchasesNet)
            MonthlyVolumePoint(
                monthKey = monthKey,
                shortLabel = formatShortArabicMonth(monthKey),
                fullLabel = formatFullArabicMonth(monthKey),
                salesTotal = round2(salesTotal),
                salesNet = round2(salesNet),
                purchasesTotal = round2(purchasesTotal),
                purchasesNet = round2(purchasesNet),
                netVat = round2(salesVat.subtract(purchasesVat)),
                operatingProfit = round2(netOperating),
                transactionCount = txs.size
            )
        }
    }

    private fun formatShortArabicMonth(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val yearShort = parts[0].takeLast(2)
        val monthNum = parts[1].toIntOrNull() ?: return monthKey
        val names = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        val mName = names.getOrNull(monthNum - 1) ?: parts[1]
        return "$mName $yearShort"
    }

    private fun formatFullArabicMonth(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull() ?: return monthKey
        val names = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        val mName = names.getOrNull(monthNum - 1) ?: parts[1]
        return "$mName $year"
    }
}

data class MonthlyVolumePoint(
    val monthKey: String,
    val shortLabel: String,
    val fullLabel: String,
    val salesTotal: Double,
    val salesNet: Double,
    val purchasesTotal: Double,
    val purchasesNet: Double,
    val netVat: Double,
    val operatingProfit: Double,
    val transactionCount: Int
)
