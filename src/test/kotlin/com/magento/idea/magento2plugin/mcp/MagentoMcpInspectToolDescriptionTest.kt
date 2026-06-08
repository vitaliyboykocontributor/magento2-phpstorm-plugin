/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp

import com.intellij.mcpserver.annotations.McpDescription
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagentoMcpInspectToolDescriptionTest {
    @Test
    fun testMagentoInspectToolDescriptionExplainsThreeStepFlow() {
        val method = MagentoMcpToolset::class.java.declaredMethods.first { toolMethod ->
            toolMethod.name == "magentoInspect" && toolMethod.getAnnotation(McpDescription::class.java) != null
        }

        val description = method.getAnnotation(McpDescription::class.java)?.description
        assertNotNull(description)
        assertTrue(description!!.contains("mode `help`"))
        assertTrue(description.contains("mode `detailed_schema`"))
        assertTrue(description.contains("mode `query`"))
        assertTrue(description.contains("queryType"))
        assertTrue(description.contains("di_config"))
        assertTrue(description.contains("acl_or_menu"))
        assertTrue(description.contains("list, inspect one schema, then query"))
    }
}
