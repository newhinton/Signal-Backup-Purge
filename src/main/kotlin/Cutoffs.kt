package de.felixnuesse

import kotlinx.datetime.*

class Cutoffs {


    companion object {


        private var primaryDuration: Int? = null
        private var secondaryDuration: Int? = null

        fun setPrimary(duration: Int) {
            primaryDuration = duration
        }

        fun setSecondary(duration: Int) {
            secondaryDuration = duration
        }

        fun getPrimary(): Instant {
            if(primaryDuration == null) {
                throw IllegalStateException("No primary duration set.")
            }
            val now = Utils.getProgramNow()
            return now.minus(primaryDuration!!, DateTimeUnit.MONTH, Utils.getSystemTimezone())
        }

        fun getSecondary(): Instant {
            if(primaryDuration == null) {
                throw IllegalStateException("No primary duration set.")
            }

            if(secondaryDuration == null) {
                throw IllegalStateException("No secondary duration set.")
            }
            val now = Utils.getProgramNow()
            return now.minus(primaryDuration!!+secondaryDuration!!, DateTimeUnit.MONTH, Utils.getSystemTimezone())
        }

    }

}