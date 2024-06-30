package de.felixnuesse

import com.github.freva.asciitable.AsciiTable
import com.github.freva.asciitable.Column
import com.github.freva.asciitable.ColumnData
import com.github.freva.asciitable.Styler
import de.felixnuesse.Utils.Companion.human
import org.fusesource.jansi.Ansi
import java.util.stream.Collectors


class TableFormatter {

    companion object {

        const val HEADER_YEARMONTH = "Year.Month"

        fun format(months: ArrayList<Month>, extensive: Boolean = false): String {

            val freed =  human(months.sumOf { it.getDeleted().sumOf { inner -> inner.getSize() }}).toString()
            val leftover =  human(months.sumOf { it.getKeptFiles().sumOf { inner -> inner.getSize() }}).toString()

            val formatter = arrayListOf<ColumnData<Month>>(
                Column().header(HEADER_YEARMONTH).footer("").with{ "${it.year}.${it.month}" },
                Column().header("Kept").footer(months.sumOf { it.getKept() }.toString()).with{ it.getKept().toString() },
                Column().header("Deleted").footer(months.sumOf { it.getDeletions() }.toString()).with{ it.getDeletions().toString() },
                Column().header("Freed Storage").footer(freed).with { human(it.getDeleted().sumOf { file -> file.getSize()}).toString() },
                Column().header("Leftover Storage").footer(leftover).with { human(it.getKeptFiles().sumOf { file -> file.getSize()}).toString() }
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


            val styler: Styler = object : Styler {
                override fun styleCell(column: Column, row: Int, col: Int, data: List<String>): List<String> {
                    if (column.header.equals(HEADER_YEARMONTH)) {
                        if(months[row].isInPrimaryStoragePhase()) {
                            return data.stream()
                                .map { line -> Ansi.ansi().fgBright(Ansi.Color.GREEN).a(line).reset().toString() }
                                .collect(Collectors.toList())
                        }
                        if(months[row].isInSecondaryStoragePhase()) {
                            return data.stream()
                                .map { line -> Ansi.ansi().fgBright(Ansi.Color.YELLOW).a(line).reset().toString() }
                                .collect(Collectors.toList())
                        }
                    }
                    return data
                }
            }


            val SLIM_FANCY_ASCII = arrayOf('┌', '─', '┬', '┐', '│', '│', '│', '╞', '═', '╪', '╡', '│', '│', '│', '├', '─', '┼', '┤', '╞', '═', '╪', '╡', '│', '│', '│', '└', '─', '┴', '┘')

            val table = AsciiTable.builder()
                .lineSeparator("\r\n")
                .border(SLIM_FANCY_ASCII)
                .data(months, formatter)
                .styler(styler)

            if(extensive) {
                return table.asString()
            }


            /*
             In theory, there is another way. I could pre-process the data,
             and create a grouping by concatenating the cells for the "group"
             before processing with the table tool. The drawback is that
             i have to keep track over the newlines i would have to add
             to each non-list-column (keep/delete), so that we dont missaling them.
             This seems easier and less error prone.
            */

            // Figure out which seperators to keep:
            val linesToKeep = arrayListOf<Int>()
            var i = 4 // Magic number: the first data row is in line 4, after the header
            var lastyear = if(months.size > 0 ) {
                months.first().year
            } else {
                ""
            }

            months.forEach {
                if(lastyear != it.year) {
                    linesToKeep.add(i-2)
                    lastyear = it.year
                }
                i += 2
            }

            val slimTable = StringBuilder()
            for ((j, line) in table.asString().lines().withIndex()) {
                var keep = true
                if(line.startsWith("├─")) {
                    if(!linesToKeep.contains(j)) {
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