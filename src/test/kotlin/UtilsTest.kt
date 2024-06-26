import de.felixnuesse.Utils
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

internal class UtilsTest {


    @Test
    fun nowEquality() {
        val instant1 = Utils.getProgramNow().toEpochMilliseconds()
        Thread.sleep(2000)
        val instant2 = Utils.getProgramNow().toEpochMilliseconds()
        assertEquals(instant1, instant2)
        println("Epoch $instant1 equals $instant2")


        val instant3 = Utils.getNow().toEpochMilliseconds()
        // Sequential calls of Clock.System.now() yield the same instant in ms.
        // This should not be an issue, but for the test, wait a tiny bit.
        Thread.sleep(1)
        val instant4 = Utils.getNow().toEpochMilliseconds()
        assertNotEquals(instant3, instant4)
        println("Epoch $instant3 does not equal $instant4")
    }
}