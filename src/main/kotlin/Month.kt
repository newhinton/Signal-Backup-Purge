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

    fun markBackups() {
        backupList.sortBy { it.getDate() }
        val first = backupList.first()
        val middle = if(backupList.size > 1) {
            backupList[(backupList.size/2)-1 ] // we start with 0
        } else {
            backupList.first()
        }

        // last year, add two per month
        if(first.isInSecondaryStoragePhase()) {
            if(backupList.size > 3){
                backupList.forEach {
                    if(it != first && it != middle) {
                        it.markForDeletion()
                    }
                }
            }
        }

        if(first.isAfterSecondaryStoragePhase()) {
            backupList.forEach {
                if(it != first) {
                    it.markForDeletion()
                }
            }
        }
    }

}