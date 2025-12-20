package de.felixnuesse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.YesNoPrompt
import org.apache.commons.io.FileUtils
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.AnsiMode
import java.io.File
import java.util.*


fun main(args: Array<String>) = SignalBackupPurge().main(args)
const val default_primary = 6
const val default_secondary = 6

const val helpString="Scan <source> for Signal Backups. This tool keeps $default_primary full months of your backups by default, and $default_secondary months after that keep 2 backups. "+
        "\n"+
        "\n"+
        "This is called the secondary retention. Secondary retention tries to evenly distribute kept backups over the month,"+
        "keeping less and less backups the further in they are in the secondary phase."+
        "After the secondary retention period, only the first and the middle-most backup per month is kept."+
        "\n"+
        "\n"+
        "Note: Only files named \"signal-yyyy-MM-dd-HH-mm-ss.backup\" are recognized. Be careful to check that all the files are named that way! Files that do not start with 'signal' and end with '.backup' are ignored regardless."

class SignalBackupPurge : CliktCommand(printHelpOnEmptyArgs = true, help = helpString) {

    init {
        versionOption("1.0.0")
    }

    companion object {
        const val KEY_ALL = "all"
    }

    private val delete: Boolean by option("-d", "--delete").flag(default = false).help("Immediately delete Files.")
    private val move: Boolean by option("-m", "--move").flag(default = false).help("Move files to 'deleted' folder instead of deleting. Takes precedent above -d")
    private val dry: Boolean by option("-n", "--dry-run").flag(default = false).help("Print all files that would be deleted by -d.")
    private val yes: Boolean by option("-y", "--yes").flag(default = false).help("Answer all prompts with yes. USE CAREFULLY!")
    private val printDeletes: Boolean by option("-p", "--print-manual-deletion").flag(default = false).help("Print a list of shell commands to purge the signal backup folder manually.")
    private val stats: Boolean by option("-s", "--stats").flag(default = false).help("Print statistics about the purge.")
    private val stats_extensive: Boolean by option("-e", "--stats-extensive").flag(default = false).help("Print even more statistics about the purge.")
    private val keep: Int by option("-k", "--keep").int().default(default_primary).help("Primary Retention Period: This determines how many months keep all backup files.")
    private val keepSecondary: Int by option("-c", "--keep-secondary").int().default(default_secondary).help("Secondary Retention Period: This determines how many months keep two backup files, beginning with the first month after the primary retention period.")
    private val verbosity: Boolean by option("-v", "--verbose").flag(default = false).help("Increases detail of the output. Shows deletions and kept files.")
    private val tiny: Boolean by option("-t", "--tiny").flag(default = false).help("Create a tiny log output in form of a flowing text")
    private val nocolor: Boolean by option("--nocolor").flag(default = false).help("Do not use colored output")
    private val source by argument().file().default(File("."))


    override fun run() {

        // first, prepare cutoffs.
        Cutoffs.setPrimary(keep)
        Cutoffs.setSecondary(keepSecondary)

        // Process files into usable structure
        val months = processMonths()

        val allDeleted = months.flatMap { it.getDeleted() }
        val allKept = months.flatMap { it.getKeptFiles() }

        allDeleted.sortedBy { it.getDate() }
        allKept.sortedBy { it.getDate() }

        if(verbosity) {
            allKept.forEach{
                println("Keep: ${source.absoluteFile}/${it.getName()}")
            }
        }

        if(printDeletes){
            allDeleted.forEach {
                println("rm ${source.absoluteFile}/${it.getName()}")
            }
        }

        if(move && !dry) {
            val newRoot = File(source.absolutePath, "deleted")
            newRoot.mkdir()
            allDeleted.forEach {
                val source = "${source.absoluteFile}/${it.getName()}"
                val target = "${newRoot.absoluteFile}/${it.getName()}"
                File(source).renameTo(File(target))
                if(verbosity) {
                    println("Moved: $target")
                }
            }
        }

        if(delete && !dry && !move) {
            allDeleted.forEach {
                val target = "${source.absoluteFile}/${it.getName()}"
                if (yes || YesNoPrompt("Delete: $target", terminal).ask() == true) {
                    File(target).delete()
                    if(verbosity) {
                        println("Deleted: $target")
                    }
                }

            }
        }

        val statisticsTable = if(stats_extensive && !tiny) {
            TableFormatter.format(months, true)
        } else if(tiny) {
            TinyFormatter.format(months, stats_extensive)
        } else if(stats) {
            TableFormatter.format(months)
        } else {
            ""
        }

        if(nocolor) {
            AnsiConsole.systemInstall()
            AnsiConsole.out().mode = AnsiMode.Strip
        }


        if(stats or stats_extensive or tiny) {
            if(stats_extensive) {
                println("Extensive Statistics:")
            } else {
                println("Statistics:")
            }
            println("Checking: ${source.absoluteFile}")

            println(statisticsTable)

            val freed = months.sumOf { it.getDeleted().sumOf { inner -> inner.getSize() }}
            val leftover = months.sumOf { it.getKeptFiles().sumOf { inner -> inner.getSize() }}

            val willOrWere = if(dry) {
                "will be"
            } else {
                "were"
            }
            println("${allKept.size} Files $willOrWere kept. (${FileUtils.byteCountToDisplaySize(leftover)})")
            println("${allDeleted.size} Files $willOrWere deleted. (${FileUtils.byteCountToDisplaySize(freed)})")
        }
    }

    private fun processMonths(): ArrayList<Month> {
        val validFileList = generateBackupList()
        val monthMap = HashMap<Int, Month>()
        validFileList.forEach{
            val date = it.getDate()
            // Generate the key from year and month
            val key = (date.year*100)+date.monthNumber

            if(!monthMap.containsKey(key)) {
                monthMap[key] = Month(date.year, date.monthNumber)
            }
            monthMap[key]!!.backupList.add(it)
        }


        var asList = arrayListOf<Month>()
        monthMap.forEach { (_, month) ->
            month.markBackups()
            asList.add(month)
        }
        asList.sortBy { it.year*100+it.month }
        asList.reverse()
        return asList
    }

    fun generateBackupList(): ArrayList<Backup> {
        val validFileList = ArrayList<Backup>()

        val regex = Regex("signal-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}).*backup")
        source.listFiles()?.forEach {
            if(regex.matches(it.name)) {
                val backup = Backup(it.name)
                backup.setRoot(source)
                validFileList.add(backup)
            }
        }
        return validFileList
    }
}
