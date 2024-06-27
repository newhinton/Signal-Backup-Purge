package de.felixnuesse

import de.felixnuesse.Utils.Companion.millis
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import java.time.Year
import java.util.ArrayList
import kotlin.math.abs

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

        var middle_ish = backupList.first()
        //use upper for a little better spacing
        val thisMonthMiddle = Month(month).length(Year.of(year).isLeap).floorDiv(2)+1

        // try to find the second backup as far in the middle as possible
        backupList.forEach {
            val distanceCurrent = thisMonthMiddle - it.getDate().dayOfMonth
            val distanceLast = thisMonthMiddle - middle_ish.getDate().dayOfMonth
            if(abs(distanceCurrent) < abs(distanceLast)) {
                middle_ish = it
            }
        }

        // last year, add two per month
        if(first.isInSecondaryStoragePhase()) {
            if(backupList.size > 3){
                backupList.forEach {
                    if(it != first && it != middle_ish) {
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