package de.felixnuesse

import kotlinx.datetime.*
import org.apache.commons.io.FileUtils

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

        fun instant(date: LocalDateTime): Instant {
            return date.toInstant(getSystemTimezone())
        }

        fun millis(date: LocalDateTime): Long {
            return instant(date).toEpochMilliseconds()
        }

        fun millis(instant: Instant): Long {
            return instant.toEpochMilliseconds()
        }

        fun human(filesize: Long): String? {
            return FileUtils.byteCountToDisplaySize(filesize)
        }
    }

}