package com.ai.assist.ai

import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtErrorFormatterTest {
    @Test
    fun unwrapsInvocationTargetException() {
        val root = IllegalStateException("backend failed")
        val wrapped = InvocationTargetException(root)

        assertEquals(root, LiteRtErrorFormatter.unwrap(wrapped))
        assertTrue(LiteRtErrorFormatter.describe(wrapped).contains("backend failed"))
    }
}
