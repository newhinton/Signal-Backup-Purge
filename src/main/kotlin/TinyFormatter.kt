package de.felixnuesse

import com.github.freva.asciitable.AsciiTable
import com.github.freva.asciitable.Column
import com.github.freva.asciitable.ColumnData
import com.github.freva.asciitable.Styler
import de.felixnuesse.TableFormatter.Companion
import de.felixnuesse.Utils.Companion.human
import org.apache.commons.io.FileUtils
import org.fusesource.jansi.Ansi
import java.util.stream.Collectors


class TinyFormatter {

    companion object {

        const val HEADER_YEARMONTH = "Year.Month"

        fun format(months: ArrayList<Month>, extensive: Boolean = false): String {

            var text = StringBuilder()

            months.forEach {
                text.appendLine("${colorFormat(it)}: ")

                if(extensive) {
                    text.appendLine(green("   Keeping: ${it.getKept()}"))
                    it.getKeptFiles().forEach { backup ->
                        text.appendLine("       ${backup.getName()} (${FileUtils.byteCountToDisplaySize(backup.getSize())})")
                    }
                    text.appendLine(red("   Deleting: ${it.getDeletions()}"))
                    it.getDeleted().forEach { backup ->
                        text.appendLine("       ${backup.getName()} (${FileUtils.byteCountToDisplaySize(backup.getSize())})")
                    }
                } else {
                    if(it.getKept() > 0) {
                        val size = it.getKeptFiles().stream().collect(Collectors.summingLong(Backup::getSize))
                        text.appendLine("   Keeping: ${it.getKept()} (${FileUtils.byteCountToDisplaySize(size)})")
                    }

                    if(it.getDeletions() > 0) {
                        val size = it.getDeleted().stream().collect(Collectors.summingLong(Backup::getSize))
                        text.appendLine("   Deleting: ${it.getDeletions()} (${FileUtils.byteCountToDisplaySize(size)})")
                    }
                }

                text.appendLine("")
            }




            return text.toString()
        }

        private fun colorFormat(month: Month): String {
            val header = "${month.year}.${Utils.strFormat(month.month)}: "
            var color = Ansi.Color.DEFAULT
            if(month.isInPrimaryStoragePhase()) {
                color = Ansi.Color.GREEN
            }
            if(month.isInSecondaryStoragePhase()) {
                color = Ansi.Color.YELLOW
            }
            return Ansi.ansi().fgBright(color).a(header).reset().toString()
        }

        private fun green(text: String): String {
            return Ansi.ansi().fg(Ansi.Color.GREEN).a(text).reset().toString()
        }

        private fun red(text: String): String {
            return Ansi.ansi().fg(Ansi.Color.RED).a(text).reset().toString()
        }

    }

}