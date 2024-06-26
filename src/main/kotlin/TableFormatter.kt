package de.felixnuesse

import com.github.freva.asciitable.AsciiTable
import com.github.freva.asciitable.Column
import com.github.freva.asciitable.ColumnData

class TableFormatter {

    companion object {
        fun format(months: ArrayList<Month>): String {

            val freed = months.sumOf { it.getDeleted().sumOf { inner -> inner.getSize() }}.toString()
            val leftover = months.sumOf { it.getKeptFiles().sumOf { inner -> inner.getSize() }}.toString()

            val formatter = listOf<ColumnData<Month>>(
                Column().header("Year.Month").footer("").with{"${it.year}.${it.month}" },
                Column().header("Kept").footer(months.sumOf { it.getKept() }.toString()).with{ it.getKept().toString() },
                Column().header("Deleted").footer(months.sumOf { it.getDeletions() }.toString()).with{ it.getDeletions().toString() },
                Column().header("Freed Storage").footer(freed).with { it.getDeleted().sumOf { it.getSize()}.toString() },
                Column().header("Leftover Storage").footer(leftover).with { it.getKeptFiles().sumOf { it.getSize()}.toString() }
            )

            val SLIM_FANCY_ASCII = arrayOf('┌', '─', '┬', '┐', '│', '│', '│', '╞', '═', '╪', '╡', '│', '│', '│', '├', '─', '┼', '┤', '╞', '═', '╪', '╡', '│', '│', '│', '└', '─', '┴', '┘')

            val table = AsciiTable.builder()
                .lineSeparator("\r\n")
                .border(SLIM_FANCY_ASCII)
                .data(months, formatter)


            // Figure out which seperators to keep:
            val linesToKeep = arrayListOf<Int>()
            var i = 4 // Magic number: the first data row is in line 4, after the header
            var lastyear = months.first().year
            months.forEach {
                if(lastyear != it.year) {
                    linesToKeep.add(i-2)
                    lastyear = it.year
                }
                i += 2
            }

            val slimTable = StringBuilder()
            for ((i, line) in table.asString().lines().withIndex()) {
                var keep = true
                if(line.startsWith("├─")) {
                    if(!linesToKeep.contains(i)) {
                        keep = false
                    }
                }

                if(keep) {
                    slimTable.append(line)
                    slimTable.append("\n")
                }

            }
            return slimTable.toString()
        }
    }
}