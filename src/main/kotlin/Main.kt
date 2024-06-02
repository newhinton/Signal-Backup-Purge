package de.felixnuesse

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.datetime.*
import kotlinx.datetime.format.byUnicodePattern
import java.io.File
import java.util.ArrayList
import java.util.HashMap


fun main(args: Array<String>) = SignalBackupPurge().main(args)

const val helpString="Copy <source> to <dest>, or multiple <source>(s) to directory <dest>."

class SignalBackupPurge : CliktCommand(printHelpOnEmptyArgs = false, help = helpString) {

    companion object {
        const val KEY_ALL = "all"
    }

    val dryRun: Boolean? by option("-d", "--dry-run").boolean().default(true).help("Should we make changes?")
    private val source by argument().file().default(File(""))


    override fun run() {
        println(source.absolutePath)

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


        println("Keep: ")
        keepList.sorted().forEach {
            println(it)
        }

        println("Discard: ")
        discardList.sorted().forEach {
            println(it)
        }

    }

    private fun createDateFromTitle(name: String): LocalDateTime {
        val dateString = name.replace("signal-", "").replace(".backup", "")
        val formatPattern = "yyyy-MM-dd-HH-mm-ss"
        val dateTimeFormat = LocalDateTime.Format { byUnicodePattern(formatPattern) }
        return dateTimeFormat.parse(dateString)
    }
}
