package de.felixnuesse

import de.felixnuesse.Utils.Companion.getSystemTimezone
import de.felixnuesse.Utils.Companion.millis
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.apache.commons.io.FileUtils
import java.io.File
import java.time.temporal.ChronoUnit

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

    fun getJavaDate(): java.time.LocalDateTime {
        return getDate().toJavaLocalDateTime()
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

    /**
     * Returns the amount of months we are already into the secondary phase.
     * -1 for either before or after
     */
    fun secondaryStoragePhaseProgession(): Long {
        if(!isInSecondaryStoragePhase()) {
            return -1
        }
        return ChronoUnit.MONTHS.between(Cutoffs.getSecondary().toLocalDateTime(getSystemTimezone()).toJavaLocalDateTime(), getJavaDate())
    }
}