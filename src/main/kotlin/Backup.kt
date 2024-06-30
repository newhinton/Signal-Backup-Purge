package de.felixnuesse

import de.felixnuesse.Utils.Companion.millis
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import org.apache.commons.io.FileUtils
import java.io.File

class Backup(private var name: String) {

    private var markedForDeletion: Boolean = false
    private var root: File? = null

    private var size = 0L

    fun setRoot(source: File) {
        root = source
        size = FileUtils.sizeOf(File("${root!!.absoluteFile}/$name"))
    }

    fun isMarkedForDeletion(): Boolean {
        return markedForDeletion
    }

    fun markForDeletion() {
        markedForDeletion = true
    }

    // todo: do not require root.
    fun getSize(): Long {
        return if(root == null) {
            System.err.println("The Size could not be determined!: $name")
            -1
        } else {
            size
        }
    }

    fun getDate(): LocalDateTime {
        val dateString = name.replace("signal-", "").replace(".backup", "")
        val formatPattern = "yyyy-MM-dd-HH-mm-ss"
        val dateTimeFormat = LocalDateTime.Format { byUnicodePattern(formatPattern) }
        return dateTimeFormat.parse(dateString)
    }

    fun getName(): String {
        return name
    }

    fun isInPrimaryStoragePhase(): Boolean {
        return millis(Cutoffs.getPrimary()) < millis(getDate())
    }

    fun isAfterPrimaryStoragePhase(): Boolean {
        val fileDate =  millis(getDate())
        return millis(Cutoffs.getPrimary()) > fileDate
    }

    fun isAfterSecondaryStoragePhase(): Boolean {
        val fileDate = millis(getDate())
        return millis(Cutoffs.getSecondary()) > fileDate
    }

    fun isInSecondaryStoragePhase(): Boolean {
        return isAfterPrimaryStoragePhase() and !isAfterSecondaryStoragePhase()
    }
}