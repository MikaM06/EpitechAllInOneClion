package eu.epitech.allinone

import eu.epitech.allinone.header.CommentStyle
import eu.epitech.allinone.header.EpitechHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpitechHeaderTest {

    private val cHeader = EpitechHeader.build(CommentStyle.C_BLOCK, 2026, "mini_printf", "main")

    @Test
    fun `builds the standard C header`() {
        assertEquals(
            "/*\n** EPITECH PROJECT, 2026\n** mini_printf\n** File description:\n** main\n*/\n",
            cHeader,
        )
    }

    @Test
    fun `builds the makefile header`() {
        val header = EpitechHeader.build(CommentStyle.HASH, 2026, "mini_printf", "Makefile")
        assertEquals("##\n## EPITECH PROJECT, 2026\n## mini_printf\n## File description:\n## Makefile\n##\n", header)
    }

    @Test
    fun `picks the comment style from the file name`() {
        assertEquals(CommentStyle.C_BLOCK, EpitechHeader.styleFor("my.h"))
        assertEquals(CommentStyle.HASH, EpitechHeader.styleFor("Makefile"))
        assertEquals(CommentStyle.HASH, EpitechHeader.styleFor("tests.py"))
        assertNull(EpitechHeader.styleFor("README.md"))
    }

    @Test
    fun `inserts header before code with one blank line`() {
        val result = EpitechHeader.apply("\n\nint main(void)\n{\n}\n", CommentStyle.C_BLOCK, cHeader)
        assertEquals(cHeader + "\nint main(void)\n{\n}\n", result)
    }

    @Test
    fun `replaces an existing header`() {
        val old = EpitechHeader.build(CommentStyle.C_BLOCK, 2025, "old_project", "old")
        val result = EpitechHeader.apply(old + "\n#include <unistd.h>\n", CommentStyle.C_BLOCK, cHeader)
        assertEquals(cHeader + "\n#include <unistd.h>\n", result)
    }

    @Test
    fun `keeps shebang first in scripts`() {
        val header = EpitechHeader.build(CommentStyle.HASH, 2026, "tools", "runner")
        val result = EpitechHeader.apply("#!/usr/bin/env python3\nprint(1)\n", CommentStyle.HASH, header)
        assertEquals("#!/usr/bin/env python3\n" + header + "\nprint(1)\n", result)
    }

    @Test
    fun `validates fields like banana`() {
        assertTrue(EpitechHeader.isValidField("ab"))
        assertFalse(EpitechHeader.isValidField("x"))
        assertFalse(EpitechHeader.isValidField("   "))
    }
}
