/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp

import com.intellij.mcpserver.annotations.McpTool
import org.junit.Assert.assertEquals
import org.junit.Test

class MagentoMcpPhpToolsetTest {
    @Test
    fun testPhpToolsetExposesExpectedToolNames() {
        val toolNames = MagentoMcpToolset::class.java.declaredMethods
            .mapNotNull { it.getAnnotation(McpTool::class.java)?.name }
            .toSet()

        assertEquals(
            setOf(
                "magento_scaffold",
                "magento_inspect",
                "magento_dead_code"
            ),
            toolNames
        )
    }
}
