package de.felixnuesse

import de.felixnuesse.Utils.Companion.millis
import kotlinx.datetime.LocalDateTime
import java.util.ArrayList

class Month(var year: Int, var month: Int) {

    val backupList = ArrayList<Backup>()

    fun getDeleted(): List<Backup> {
        return backupList.filter { it.isMarkedForDeletion() }.sortedBy { it.getDate() }
    }

    fun getKeptFiles(): List<Backup> {
        return backupList.filter { !it.isMarkedForDeletion() }.sortedBy { it.getDate() }
    }

    fun getDeletions(): Int {
        return backupList.filter { it.isMarkedForDeletion() }.count()
    }

    fun getKept(): Int {
        return backupList.filter { !it.isMarkedForDeletion() }.count()
    }

    fun isInPrimaryStoragePhase(): Boolean {
        return millis(Cutoffs.getPrimary()) <= millis(getMonthDate())
    }

    /**
     * only in secondary when it is not in primary, and not after secondary.
     */
    fun isInSecondaryStoragePhase(): Boolean {
        return millis(Cutoffs.getSecondary()) <= millis(getMonthDate()) && !isInPrimaryStoragePhase()
    }

    private fun getMonthDate(): LocalDateTime {
        return LocalDateTime(year, month, 1, 0, 0, 0, 0)
    }

}