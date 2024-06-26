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

        // first, prepare cutoffs.
        Cutoffs.setPrimary(keep)
        Cutoffs.setSecondary(keepSecondary)


        val validFileList = ArrayList<Backup>()

        val regex = Regex("signal-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}).*backup")
        source.listFiles()?.forEach {
            if(regex.matches(it.name)) {
                validFileList.add(Backup(it.name))
            }
        }

        // Year: [Month: List<Names>]}
        val orderedMap = HashMap<String, HashMap<String, ArrayList<Backup>>>()

        validFileList.sortBy { it.getDate() }
        validFileList.forEach{
            val date = it.getDate()

            if(!orderedMap.containsKey("${date.year}")) {
                orderedMap["${date.year}"] = HashMap<String, ArrayList<Backup>>()
            }

            val assertedMap = orderedMap["${date.year}"]!!


            val key = if(Cutoffs.getPrimary().toEpochMilliseconds() < date.toInstant(Utils.getSystemTimezone()).toEpochMilliseconds()) {
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
                            files.forEach {
                                keepList.add(it.getName())
                            }
                            return@run
                        }

                        files.sortBy { it.getDate() }
                        val first = files.first()


                        // last year, add two per month
                        if(first.isInSecondaryStoragePhase()) {
                            if(files.size <= 3){
                                files.forEach {
                                    keepList.add(it.getName())
                                }
                            } else {
                                keepList.add(first.getName())
                                val middle = (files.size/2)-1 // we start with 0
                                keepList.add(files[middle].getName())
                            }
                        }

                        if(first.isAfterSecondaryStoragePhase()) {
                            keepList.add(first.getName())
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
            if(!keepList.contains(it.getName())) {
                discardList.add(it.getName())
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

            // Todo: Migrate each backup into a "backup" class, which has its date already as an object,
            // aswell as a "delete or keep" attribution. This makes stuff like this way easier.
            // todo: add "saved storage" column
            // todo: add "used storage" column
            println("┌────────────┬──────┬─────────┐")
            println("│ Year.Month │ Kept │ Deleted │")
            println("├────────────┼──────┼─────────┤")
            orderedMap.forEach { (year, hashMap) ->
                run {
                    val sortedMap = hashMap.toSortedMap(object : Comparator <String> {
                        override fun compare (p0: String, pi: String) : Int {
                            val p0_ = p0.padStart(3, '0')
                            val pi_ = pi.padStart(3, '0')
                            return pi_.compareTo(p0_)
                        }
                    })
                    sortedMap.forEach { (month, files) ->
                        run {
                            var kept = 0
                            var deleted = 0

                            files.forEach {
                                if (keepList.contains(it.getName())){
                                    kept++
                                }
                                if (discardList.contains(it.getName())){
                                    deleted++
                                }
                            }
                            println("│ $year.${month.padEnd(5)} │ ${kept.toString().padEnd(4)} │ ${deleted.toString().padEnd(7)} │")
                        }
                    }
                }
                println("├────────────┼──────┼─────────┤")
            }
            println("│ Overall    │ ${keepList.size.toString().padEnd(4)} │ ${discardList.size.toString().padEnd(7)} │")
            println("└────────────┴──────┴─────────┘")

            println("${keepList.size} Files will be kept. (${FileUtils.byteCountToDisplaySize(keepSize)})")
            println("${discardList.size} Files will be deleted. (${FileUtils.byteCountToDisplaySize(deleteSize)})")
        }

    }
}
