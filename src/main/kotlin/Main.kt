package de.felixnuesse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.datetime.*
import kotlinx.datetime.format.byUnicodePattern
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.ArrayList
import java.util.HashMap


fun main(args: Array<String>) = SignalBackupPurge().main(args)

const val helpString="Scan <source> for Signal Backups. This tool keeps 6 full months of your backups by default, and 3 months after that keep 2 backups. "+
        "\n"+
        "\n"+
        "This is called the secondary retention. Secondary retention uses the first backup in a month, and form all existing backups for that month, the closest to the middle."+
        "After the secondary retention period, only the first backup per month is kept."+
        "\n"+
        "\n"+
        "Note: Only files named \"signal-yyyy-MM-dd-HH-mm-ss.backup\" are recognized. Be careful to check that all the files are named that way! Files that do not start with 'signal' and end with '.backup' are ignored regardless."

class SignalBackupPurge : CliktCommand(printHelpOnEmptyArgs = true, help = helpString) {

    companion object {
        const val KEY_ALL = "all"
    }

    private val delete: Boolean by option("-d", "--delete").flag(default = false).help("Immediately delete Files.")
    private val dry: Boolean by option("-n", "--dry-run").flag(default = false).help("Print all files that would be deleted by -d.")
    private val yes: Boolean by option("-y", "--yes").flag(default = false).help("Answer all prompts with yes. USE CAREFULLY!")
    private val printDeletes: Boolean by option("-p", "--print-deletes").flag(default = false).help("Print a list of shell commands to purge the signal backup folder.")
    private val stats: Boolean by option("-s", "--stats").flag(default = false).help("Print statistics about the purge.")
    private val keep: Int by option("-k", "--keep").int().default(6).help("Primary Retention Period: This determines how many months keep all backup files.")
    private val keepSecondary: Int by option("-c", "--keep-secondary").int().default(3).help("Secondary Retention Period: This determines how many months keep two backup files, beginning with the first month after the primary retention period.")
    private val verbosity: Boolean by option("-v", "--verbose").flag(default = false).help("Increases detail of the output. Shows deletions and kept files.")
    private val source by argument().file().default(File("."))


    override fun run() {

        val validFileList = ArrayList<String>()

        val regex = Regex("signal-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}).*backup")
        source.listFiles()?.forEach {
            if(regex.matches(it.name)) {
                validFileList.add(it.name)
            }
        }

        val tzSystem = TimeZone.currentSystemDefault()

        val cutoffPrimary: Instant = Clock.System.now().minus(keep, DateTimeUnit.MONTH, tzSystem)
        val cutoffSecondary: Instant = Clock.System.now().minus(keep+keepSecondary, DateTimeUnit.MONTH, tzSystem)

        // Year: [Month: List<Names>]}
        val orderedMap = HashMap<String, HashMap<String, ArrayList<String>>>()

        validFileList.sorted().forEach{
            val date = createDateFromTitle(it)


            if(!orderedMap.containsKey("${date.year}")) {
                orderedMap["${date.year}"] = HashMap<String, ArrayList<String>>()
            }

            val assertedMap = orderedMap["${date.year}"]!!


            val key = if(cutoffPrimary.toEpochMilliseconds() < date.toInstant(tzSystem).toEpochMilliseconds()) {
                KEY_ALL
            } else {
                "${date.month.number}"
            }

            if(!assertedMap.containsKey(key)) {
                assertedMap[key] = ArrayList()
            }
            assertedMap[key]!!.add(it)

        }

        val keepList = ArrayList<String>()
        orderedMap.forEach { (year, hashMap) ->
            run {
                //println("#####")
                //println(year)
                hashMap.forEach { (month, files) ->
                    run {
                        //println(month)
                        if(month==KEY_ALL) {
                            keepList.addAll(files)
                            return@run
                        }

                        val sorted = files.sorted()
                        val first = sorted.first()


                        // last year, add two per month
                        val fileDate = createDateFromYearAndMonth(year.toInt(), month.toInt()).toInstant(tzSystem).toEpochMilliseconds()
                        val isAfterPrimaryStoragePhase = cutoffPrimary.toEpochMilliseconds() > fileDate
                        val isAfterSecondaryStoragePhase = cutoffSecondary.toEpochMilliseconds() > fileDate
                        val isInSecondaryStoragePhase = isAfterPrimaryStoragePhase and !isAfterSecondaryStoragePhase

                        if(isInSecondaryStoragePhase) {
                            if(files.size <= 3){
                                keepList.addAll(files)
                            } else {
                                keepList.add(first)
                                val middle = (sorted.size/2)-1 // we start with 0
                                keepList.add(sorted[middle])
                            }
                        }

                        if(isAfterSecondaryStoragePhase) {
                            keepList.add(first)
                        }
                    }
                }
            }
        }


        keepList.sorted().forEach {
            if(verbosity) {
                println("Keep: ${source.absoluteFile}/$it")
            }

        }


        val discardList = ArrayList<String>()
        validFileList.forEach {
            if(!keepList.contains(it)) {
                discardList.add(it)
            }
        }

        if(delete == true) {
            discardList.forEach {
                val target = "${source.absoluteFile}/$it"
                if (YesNoPrompt("Delete: $target", terminal).ask() == true || yes == true) {
                    File(target).delete()
                    if(verbosity) {
                        println("Deleted: $target")
                    }
                }

            }
        }

        if(dry){
            discardList.forEach {
                println("Deleted: ${source.absoluteFile}/$it")
            }
        }

        if(printDeletes){
            discardList.sorted().forEach {
                println("rm ${source.absoluteFile}/$it")
            }
        }


        if(stats) {
            println("Statistics:")

            println("Checking: ${source.absoluteFile}")
            var keepSize = 0L
            keepList.forEach {
                keepSize += FileUtils.sizeOf(File("${source.absoluteFile}/$it"))
            }

            var deleteSize = 0L
            discardList.forEach {
                deleteSize += FileUtils.sizeOf(File("${source.absoluteFile}/$it"))
            }

            println("${keepList.size} Files will be kept. (${FileUtils.byteCountToDisplaySize(keepSize)})")
            println("${discardList.size} Files will be deleted. (${FileUtils.byteCountToDisplaySize(deleteSize)})")
        }

    }

    private fun createDateFromTitle(name: String): LocalDateTime {
        val dateString = name.replace("signal-", "").replace(".backup", "")
        val formatPattern = "yyyy-MM-dd-HH-mm-ss"
        val dateTimeFormat = LocalDateTime.Format { byUnicodePattern(formatPattern) }
        return dateTimeFormat.parse(dateString)
    }

    private fun createDateFromYearAndMonth(year: Int, month: Int): LocalDateTime {
        return LocalDateTime(year, month, 1, 0, 0, 0, 0)
    }
}
