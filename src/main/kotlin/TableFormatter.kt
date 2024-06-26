package de.felixnuesse

import com.github.freva.asciitable.AsciiTable
import com.github.freva.asciitable.AsciiTableBuilder
import com.github.freva.asciitable.Column
import com.github.freva.asciitable.ColumnData

class TableFormatter {

    companion object {
        fun format(months: ArrayList<Month>): AsciiTableBuilder {

            val freed = months.sumOf { it.getDeleted().sumOf { it.getSize() }}.toString()
            val leftover = months.sumOf { it.getKeptFiles().sumOf { it.getSize() }}.toString()

            val formatter = listOf<ColumnData<Month>>(
                Column().header("Year.Month").footer("").with{"${it.year}.${it.month}" },
                Column().header("Kept").footer(months.sumOf { it.getKept() }.toString()).with{ it.getKept().toString() },
                Column().header("Deleted").footer(months.sumOf { it.getDeletions() }.toString()).with{ it.getDeletions().toString() },
                Column().header("Freed Storage").footer(freed).with { it.getDeleted().sumOf { it.getSize()}.toString() },
                Column().header("Leftover Storage").footer(leftover).with { it.getKeptFiles().sumOf { it.getSize()}.toString() }
            )

            val SLIM_FANCY_ASCII = arrayOf('┌', '─', '┬', '┐', '│', '│', '│', '╞', '═', '╪', '╡', '│', '│', '│', '│', '─', '┼', '┤', '╞', '═', '╪', '╡', '│', '│', '│', '└', '─', '┴', '┘')

            val table = AsciiTable.builder()
                .lineSeparator("\r\n")
                .border(SLIM_FANCY_ASCII)
                .data(months, formatter)

            return table
        }
    }
}