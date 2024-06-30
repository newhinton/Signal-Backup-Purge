package de.felixnuesse

import de.felixnuesse.Utils.Companion.ceilDiv
import de.felixnuesse.Utils.Companion.millis
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import kotlin.math.absoluteValue

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
        val thisMonthLength = Month(month).length(Year.of(year).isLeap)

        // --- Process Secondary Phase --- //
        if(first.isInSecondaryStoragePhase()) {
            val minimumBackups = thisMonthLength*first.secondaryStoragePhaseProgession().toDouble().div(6)

            // never drop below 2.
            if(backupList.size > maxOf(minimumBackups.toInt(), 2)){
                markSecondaryPhase(minimumBackups.toInt())
            }
        }


        // --- Process Tertiary Phase --- //

        // Generate middle target
        val thisMonthMiddle = first.getJavaDate().withDayOfMonth(thisMonthLength/2)
        val middle_ish = backupList
            .toMutableList()
            .filter { it != first }
            .minByOrNull { ChronoUnit.HOURS.between(it.getJavaDate(), thisMonthMiddle).absoluteValue }

        if(first.isAfterSecondaryStoragePhase()) {
            backupList.forEach {
                if(it != first && it != middle_ish) {
                    it.markForDeletion()
                }
            }
        }
    }

    fun markSecondaryPhase(targetBackups: Int) {
        // Sort dates (they should be sorted already if input is well-formed)
        backupList.sortBy { millis(it.getDate()) }


        // Determine the first and last date in the list
        val first = backupList.first().getJavaDate()
        val lastDateTime = backupList.last().getJavaDate()

        // get duration in hours, to reduce the rounding error. In short intervals, this will purge later backups.
        val totalDuration = ChronoUnit.HOURS.between(first, lastDateTime).toInt()
        val numberOfIntervals = targetBackups.coerceAtLeast(1)

        // calculate the interval length in hours
        val intervalLength = (totalDuration / numberOfIntervals).coerceAtLeast(1).toLong()


        println("#### $year.$month")
        println("$numberOfIntervals $intervalLength")


        // Create the pruned list
        val keepDates = mutableListOf(backupList.first())
        var currentIntervalStart = first

        for (i in 0 ..< numberOfIntervals) {

            // if the interval is very long, we prevent extremes.
            // eg, 2 should be kept: then without it the first and last would be stored.
            // this way, we get the first and one in the middle
            val betweenIntervalStartAndEnd = currentIntervalStart.plusHours(intervalLength/2)
            println("Interval Start to match: $betweenIntervalStartAndEnd")


            // This only takes the closest of all matches. This will filter out anything but the first backup per day.
            // If multiple backups per day exist, only the closest is kept.
            val clostestBackup = backupList.minBy { ChronoUnit.HOURS.between(it.getJavaDate(), betweenIntervalStartAndEnd).absoluteValue }
            keepDates.add(clostestBackup)

            // move interval to next start point
            currentIntervalStart = currentIntervalStart.plusHours(intervalLength)
            println("nextIntervalStart $currentIntervalStart")
        }

        backupList.forEach {
            if(!keepDates.contains(it)) {
                it.markForDeletion()
            }
        }
    }

}