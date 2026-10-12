package eu.epitech.allinone

import eu.epitech.allinone.banana.BananaOutputParser
import eu.epitech.allinone.banana.BananaSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Path

class BananaOutputParserTest {

    private val base = Path.of("/project")

    @Test
    fun `parses banana and compiler diagnostics`() {
        val output = """
            disp_stdarg.c:38:5: warning: [Banana] [Minor] missing space after for keyword (C-L3)
               38 |     for(; i < s; i++) {
                  |     ^~~
            ./include/my.h:1:1: warning: [Banana] [Major] file not protected against double inclusion (C-H2)
            disp_stdarg.c:25:40: error: expected ';' after top level declarator
            2 warnings and 1 error generated.
        """.trimIndent()

        val issues = BananaOutputParser.parse(output, base)

        assertEquals(3, issues.size)
        with(issues[0]) {
            assertEquals("/project/disp_stdarg.c", filePath)
            assertEquals(38, line)
            assertEquals(5, column)
            assertEquals(BananaSeverity.MINOR, severity)
            assertEquals("missing space after for keyword", message)
            assertEquals("C-L3", rule)
        }
        assertEquals("/project/include/my.h", issues[1].filePath)
        assertEquals(BananaSeverity.MAJOR, issues[1].severity)
        assertEquals(BananaSeverity.ERROR, issues[2].severity)
        assertNull(issues[2].rule)
    }

    @Test
    fun `removes duplicates reported through several includes`() {
        val line = "./include/my.h:10:7: warning: [Banana] [Info] file not ending with a newline (C-A3)"
        assertEquals(1, BananaOutputParser.parse("$line\n$line\n", base).size)
    }

    @Test
    fun `follows make directory changes`() {
        val output = """
            make[1]: Entering directory '/project/lib/my'
            my_putstr.c:3:1: warning: [Banana] [Minor] trailing space (C-G7)
            make[1]: Leaving directory '/project/lib/my'
            main.c:1:1: warning: [Banana] [Minor] file not starting with standard Epitech header (C-G1)
        """.trimIndent()

        val issues = BananaOutputParser.parse(output, base)

        assertEquals("/project/lib/my/my_putstr.c", issues[0].filePath)
        assertEquals("/project/main.c", issues[1].filePath)
    }
}
