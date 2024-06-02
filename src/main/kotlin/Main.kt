package de.felixnuesse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.datetime.*
import kotlinx.datetime.format.byUnicodePattern
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashMap


fun main(args: Array<String>) = SignalBackupPurge().main(args)

const val helpString="Copy <source> to <dest>, or multiple <source>(s) to directory <dest>."

class SignalBackupPurge : CliktCommand(printHelpOnEmptyArgs = true, help = helpString) {

    companion object {
        const val KEY_ALL = "all"
    }

    private val delete: Boolean? by option("-d", "--delete").flag(default = false).help("Immediately delete Files.")
    private val dry: Boolean? by option("-n", "--dry-run").flag(default = false).help("Print all files that would be deleted by -d.")
    private val yes: Boolean? by option("-y", "--yes").flag(default = false).help("Answer all promts with yes")
    private val printDeletes: Boolean? by option("-p", "--print-deletes").flag(default = false).help("Print a list of shell commands to purge the signal backup folder")
    private val stats: Boolean? by option("-s", "--stats").flag(default = false).help("print statistics of the purge")
    private val source by argument().file().default(File("."))


    override fun run() {

        val validFileList = ArrayList<String>()

        val regex = Regex("signal-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}).*backup")
        source.listFiles()?.forEach {
            if(regex.matches(it.name)) {
                validFileList.add(it.name)
            }
        }

        val currentMoment: Instant = Clock.System.now()
        val now = currentMoment.toLocalDateTime(TimeZone.UTC)

        // Year: [Month: List<Names>]}
        val orderedMap = HashMap<String, HashMap<String, ArrayList<String>>>()

        validFileList.sorted().forEach{
            val date = createDateFromTitle(it)


            if(!orderedMap.containsKey("${date.year}")) {
                orderedMap["${date.year}"] = HashMap<String, ArrayList<String>>()
            }

            val assertedMap = orderedMap["${date.year}"]!!

            val key = if(date.year == now.year) {
                KEY_ALL
            } else {
                "${date.month}"
            }

            if(!assertedMap.containsKey(key)) {
                assertedMap[key] = ArrayList()
            }
            assertedMap[key]!!.add(it)

        }

        val keepList = ArrayList<String>()
        orderedMap.forEach { (year, hashMap) ->
            run {
                hashMap.forEach { (month, files) ->
                    run {
                        if(month==KEY_ALL) {
                            keepList.addAll(files)
                            return@run
                        }

                        val sorted = files.sorted()
                        val first = sorted.first()

                        // last year, add two per month
                        if(year.toInt() == now.year-1) {
                            if(files.size <= 3){
                                keepList.addAll(files)
                            } else {
                                keepList.add(first)
                                if(year.toInt() == now.year-1) {
                                    val middle = (sorted.size/2)-1 // we start with 0
                                    keepList.add(sorted[middle])
                                }
                            }
                        } else {
                            // in every older year, only add one per month.
                            keepList.add(first)
                        }
                    }
                }
            }
        }

        val discardList = ArrayList<String>()
        validFileList.forEach {
            if(!keepList.contains(it)) {
                discardList.add(it)
            }
        }

        if(printDeletes == true){
            discardList.sorted().forEach {
                println("rm ${source.absoluteFile}/$it")
            }
        }


        if(stats == true) {
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

        if(delete == true) {
            discardList.forEach {
                val target = "${source.absoluteFile}/$it"
                if (YesNoPrompt("Delete: $target", terminal).ask() == true || yes == true) {
                    File(target).delete()
                    println("Deleted: $target")
                }

            }
        }

        if(dry == true){
            discardList.forEach {
                println("Deleted: ${source.absoluteFile}/$it")
            }
        }

    }

    private fun createDateFromTitle(name: String): LocalDateTime {
        val dateString = name.replace("signal-", "").replace(".backup", "")
        val formatPattern = "yyyy-MM-dd-HH-mm-ss"
        val dateTimeFormat = LocalDateTime.Format { byUnicodePattern(formatPattern) }
        return dateTimeFormat.parse(dateString)
    }
}
