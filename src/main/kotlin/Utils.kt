package de.felixnuesse

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class Utils {

    companion object {

        private var now: Instant? = null

        /***
         * This function returns the program-unique runtime.
         * It will not change during runtime. It will always return the instant
         * when the instant was initially requested.
         */
        fun getProgramNow(): Instant {
            if(now == null) {
                now = Clock.System.now()
            }
            return now!!
        }

        /**
         * This returns the current Instant.
         * It is always up-to-date.
         */
        fun getNow(): Instant {
            return Clock.System.now()
        }

        fun getSystemTimezone(): TimeZone {
            return TimeZone.currentSystemDefault()
        }
    }

}