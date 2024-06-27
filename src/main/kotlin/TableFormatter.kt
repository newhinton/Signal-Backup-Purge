package de.felixnuesse

import com.github.freva.asciitable.AsciiTable
import com.github.freva.asciitable.Column
import com.github.freva.asciitable.ColumnData

class TableFormatter {

    companion object {
        fun format(months: ArrayList<Month>, extensive: Boolean = false): String {

            val freed = months.sumOf { it.getDeleted().sumOf { inner -> inner.getSize() }}.toString()
            val leftover = months.sumOf { it.getKeptFiles().sumOf { inner -> inner.getSize() }}.toString()

            val formatter = arrayListOf<ColumnData<Month>>(
                Column().header("Year.Month").footer("").with{"${it.year}.${it.month}" },
                Column().header("Kept").footer(months.sumOf { it.getKept() }.toString()).with{ it.getKept().toString() },
                Column().header("Deleted").footer(months.sumOf { it.getDeletions() }.toString()).with{ it.getDeletions().toString() },
                Column().header("Freed Storage").footer(freed).with { it.getDeleted().sumOf { it.getSize()}.toString() },
                Column().header("Leftover Storage").footer(leftover).with { it.getKeptFiles().sumOf { it.getSize()}.toString() }
            )

            if(extensive) {
                formatter.add(
                    Column().header("Files Kept").with {
                        it.getKeptFiles().joinToString(separator = "\n") { file -> file.getName() }
                    }
                )

                formatter.add(
                    Column().header("Files Deleted").with {
                        it.getDeleted().joinToString(separator = "\n") { file -> file.getName() }
                    }
                )
            }

            val SLIM_FANCY_ASCII = arrayOf('┌', '─', '┬', '┐', '│', '│', '│', '╞', '═', '╪', '╡', '│', '│', '│', '├', '─', '┼', '┤', '╞', '═', '╪', '╡', '│', '│', '│', '└', '─', '┴', '┘')

            val table = AsciiTable.builder()
                .lineSeparator("\r\n")
                .border(SLIM_FANCY_ASCII)
                .data(months, formatter)

            if(extensive) {
                return table.asString()
            }

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