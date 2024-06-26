package de.felixnuesse

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant

class Backup(private var name: String) {

    var toBeDeleted: Boolean = false
    var size: Long = 0


    fun getDate(): LocalDateTime {
        val dateString = name.replace("signal-", "").replace(".backup", "")
        val formatPattern = "yyyy-MM-dd-HH-mm-ss"
        val dateTimeFormat = LocalDateTime.Format { byUnicodePattern(formatPattern) }
        return dateTimeFormat.parse(dateString)
    }

    fun getName(): String {
        return name
    }

    private fun getYearMonthDate(): LocalDateTime {
        return LocalDateTime(getDate().year, getDate().month, 1, 0, 0, 0, 0)
    }

    fun isAfterPrimaryStoragePhase(): Boolean {
        val fileDate =  getYearMonthDate().toInstant(Utils.getSystemTimezone()).toEpochMilliseconds()
        return Cutoffs.getPrimary().toEpochMilliseconds() > fileDate
    }

    fun isAfterSecondaryStoragePhase(): Boolean {
        val fileDate =  getYearMonthDate().toInstant(Utils.getSystemTimezone()).toEpochMilliseconds()
        return Cutoffs.getSecondary().toEpochMilliseconds() > fileDate
    }

    fun isInSecondaryStoragePhase(): Boolean {
        return isAfterPrimaryStoragePhase() and !isAfterSecondaryStoragePhase()
    }
}