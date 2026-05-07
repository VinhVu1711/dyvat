package com.vinh.dyvat.ui.screens.statistics

import com.vinh.dyvat.data.model.DailySummary
import com.vinh.dyvat.data.model.PurchaseExportRow
import com.vinh.dyvat.data.model.SaleExportRow
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal data class StatisticsReportPeriod(
    val mode: StatsPeriodMode,
    val selectedYear: Int,
    val selectedMonth: Int,
    val startDate: String,
    val endDate: String
)

internal class StatisticsExcelReportWriter {

    suspend fun writeReport(
        outputStream: OutputStream,
        period: StatisticsReportPeriod,
        dailyData: List<DailySummary>,
        loadPurchasePage: suspend (page: Int) -> List<PurchaseExportRow>,
        loadSalePage: suspend (page: Int) -> List<SaleExportRow>
    ) {
        ZipOutputStream(outputStream).use { zip ->
            writeStaticFiles(zip)
            writeWorkbook(zip)
            writeWorkbookRels(zip)
            writePurchaseSheet(zip, loadPurchasePage)
            writeSaleSheet(zip, loadSalePage)
            writeProfitSheet(zip, period, dailyData)
        }
    }

    private fun writeStaticFiles(zip: ZipOutputStream) {
        zip.writeEntry("[Content_Types].xml", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent())

        zip.writeEntry("_rels/.rels", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent())
    }

    private fun writeWorkbook(zip: ZipOutputStream) {
        zip.writeEntry("xl/workbook.xml", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                    <sheet name="Chi tiết nhập hàng" sheetId="1" r:id="rId1"/>
                    <sheet name="Chi tiết bán hàng" sheetId="2" r:id="rId2"/>
                    <sheet name="Doanh thu lợi nhuận" sheetId="3" r:id="rId3"/>
                </sheets>
            </workbook>
        """.trimIndent())
    }

    private fun writeWorkbookRels(zip: ZipOutputStream) {
        zip.writeEntry("xl/_rels/workbook.xml.rels", """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
            </Relationships>
        """.trimIndent())
    }

    private suspend fun writePurchaseSheet(
        zip: ZipOutputStream,
        loadPurchasePage: suspend (page: Int) -> List<PurchaseExportRow>
    ) {
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zip.writeText(sheetStart())
        var rowIndex = 1
        zip.writeText(row(rowIndex++, listOf(
            textCell("Ngày nhập"),
            textCell("Mã phiếu"),
            textCell("Tên sản phẩm"),
            textCell("Nhà cung cấp"),
            textCell("Đơn vị tính"),
            textCell("Số lượng"),
            textCell("Đơn giá nhập"),
            textCell("Tổng tiền dòng")
        )))

        var page = 0
        var total = 0L
        while (true) {
            val rows = loadPurchasePage(page)
            if (rows.isEmpty()) break
            rows.forEach { item ->
                total += item.lineTotalVnd
                zip.writeText(row(rowIndex++, listOf(
                    textCell(item.purchaseDate),
                    textCell(item.ticketCode),
                    textCell(item.productName),
                    textCell(item.supplierName),
                    textCell(item.unitName),
                    numberCell(item.quantityPurchased),
                    numberCell(item.purchasePriceVnd),
                    numberCell(item.lineTotalVnd)
                )))
            }
            page++
        }

        zip.writeText(row(rowIndex, listOf(
            textCell("Tổng tiền nhập kỳ"),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            numberCell(total)
        )))
        zip.writeText(sheetEnd())
        zip.closeEntry()
    }

    private suspend fun writeSaleSheet(
        zip: ZipOutputStream,
        loadSalePage: suspend (page: Int) -> List<SaleExportRow>
    ) {
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
        zip.writeText(sheetStart())
        var rowIndex = 1
        zip.writeText(row(rowIndex++, listOf(
            textCell("Ngày bán"),
            textCell("Mã phiếu"),
            textCell("Tên sản phẩm"),
            textCell("Đơn vị tính"),
            textCell("Số lượng bán"),
            textCell("Đơn giá bán"),
            textCell("Tổng tiền dòng")
        )))

        var page = 0
        var total = 0L
        while (true) {
            val rows = loadSalePage(page)
            if (rows.isEmpty()) break
            rows.forEach { item ->
                total += item.lineRevenueVnd
                zip.writeText(row(rowIndex++, listOf(
                    textCell(item.saleDate),
                    textCell(item.ticketCode),
                    textCell(item.productName),
                    textCell(item.unitName),
                    numberCell(item.quantitySold),
                    numberCell(item.salePriceVnd),
                    numberCell(item.lineRevenueVnd)
                )))
            }
            page++
        }

        zip.writeText(row(rowIndex, listOf(
            textCell("Tổng tiền bán kỳ"),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            emptyCell(),
            numberCell(total)
        )))
        zip.writeText(sheetEnd())
        zip.closeEntry()
    }

    private fun writeProfitSheet(
        zip: ZipOutputStream,
        period: StatisticsReportPeriod,
        dailyData: List<DailySummary>
    ) {
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet3.xml"))
        zip.writeText(sheetStart())
        var rowIndex = 1
        zip.writeText(row(rowIndex++, listOf(
            textCell("Kỳ"),
            textCell("Tiền nhập"),
            textCell("Doanh thu"),
            textCell("Giá vốn"),
            textCell("Lợi nhuận"),
            textCell("Số phiếu nhập"),
            textCell("Số phiếu bán")
        )))

        val rows = if (period.mode == StatsPeriodMode.MONTH) {
            fillMonthDays(dailyData, period.selectedYear, period.selectedMonth).map { summary ->
                ProfitRow(
                    label = summary.businessDate,
                    totalPurchaseVnd = summary.totalPurchaseVnd,
                    totalSaleVnd = summary.totalSaleVnd,
                    totalCostVnd = summary.totalCostVnd,
                    profitVnd = summary.profitVnd,
                    purchaseTicketCount = summary.purchaseTicketCount,
                    saleTicketCount = summary.saleTicketCount
                )
            }
        } else {
            (1..12).map { month ->
                val monthStr = month.toString().padStart(2, '0')
                val days = dailyData.filter { it.businessDate.length >= 7 && it.businessDate.substring(5, 7) == monthStr }
                ProfitRow(
                    label = "Tháng $month/${period.selectedYear}",
                    totalPurchaseVnd = days.sumOf { it.totalPurchaseVnd },
                    totalSaleVnd = days.sumOf { it.totalSaleVnd },
                    totalCostVnd = days.sumOf { it.totalCostVnd },
                    profitVnd = days.sumOf { it.profitVnd },
                    purchaseTicketCount = days.sumOf { it.purchaseTicketCount },
                    saleTicketCount = days.sumOf { it.saleTicketCount }
                )
            }
        }

        rows.forEach { item ->
            zip.writeText(row(rowIndex++, listOf(
                textCell(item.label),
                numberCell(item.totalPurchaseVnd),
                numberCell(item.totalSaleVnd),
                numberCell(item.totalCostVnd),
                numberCell(item.profitVnd),
                numberCell(item.purchaseTicketCount),
                numberCell(item.saleTicketCount)
            )))
        }

        zip.writeText(row(rowIndex, listOf(
            textCell("Tổng toàn kỳ"),
            numberCell(rows.sumOf { it.totalPurchaseVnd }),
            numberCell(rows.sumOf { it.totalSaleVnd }),
            numberCell(rows.sumOf { it.totalCostVnd }),
            numberCell(rows.sumOf { it.profitVnd }),
            numberCell(rows.sumOf { it.purchaseTicketCount }),
            numberCell(rows.sumOf { it.saleTicketCount })
        )))
        zip.writeText(sheetEnd())
        zip.closeEntry()
    }

    private fun sheetStart(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>"""

    private fun sheetEnd(): String = "</sheetData></worksheet>"

    private fun row(index: Int, cells: List<String>): String {
        val content = cells.mapIndexed { idx, cell -> cell.replace("{ref}", "${columnName(idx + 1)}$index") }.joinToString("")
        return """<row r="$index">$content</row>"""
    }

    private fun textCell(value: String): String = """<c r="{ref}" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>"""

    private fun numberCell(value: Number): String = """<c r="{ref}"><v>$value</v></c>"""

    private fun emptyCell(): String = """<c r="{ref}"/>"""

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        writeText(content)
        closeEntry()
    }

    private fun ZipOutputStream.writeText(content: String) {
        write(content.toByteArray(Charsets.UTF_8))
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun columnName(index: Int): String {
        var value = index
        val result = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            result.insert(0, ('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return result.toString()
    }
}

private data class ProfitRow(
    val label: String,
    val totalPurchaseVnd: Long,
    val totalSaleVnd: Long,
    val totalCostVnd: Long,
    val profitVnd: Long,
    val purchaseTicketCount: Int,
    val saleTicketCount: Int
)
