import de.felixnuesse.Cutoffs
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class CutoffsTest {

    @Test
    fun testPrimary() {
        assertFailsWith<IllegalStateException> {
            Cutoffs.getPrimary()
        }
        Cutoffs.setPrimary(3)
       assertDoesNotThrow { Cutoffs.getPrimary() }
    }

    @Test
    fun testSecondary() {
        assertFailsWith<IllegalStateException> {
            Cutoffs.getSecondary()
        }
        Cutoffs.setSecondary(3)
        assertDoesNotThrow { Cutoffs.getSecondary() }
    }
}