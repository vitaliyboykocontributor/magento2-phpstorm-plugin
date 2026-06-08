/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp

import com.intellij.mcpserver.annotations.McpDescription
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagentoMcpDeadCodeToolDescriptionTest {
    @Test
    fun testMagentoDeadCodeToolDescriptionExplainsVerificationRequirement() {
        val method = MagentoMcpToolset::class.java.declaredMethods.first { toolMethod ->
            toolMethod.name == "magentoDeadCode" && toolMethod.getAnnotation(McpDescription::class.java) != null
        }

        val description = method.getAnnotation(McpDescription::class.java)?.description
        assertNotNull(description)
        assertTrue(description!!.contains("mode `help`"))
        assertTrue(description.contains("mode `detailed_schema`"))
        assertTrue(description.contains("mode `query`"))
        assertTrue(description.contains("unused_html_templates"))
        assertTrue(description.contains("unused_plugins"))
        assertTrue(description.contains("unused_observers"))
        assertTrue(description.contains("long task or refactoring"))
        assertTrue(description.contains("recently updated or created module"))
        assertTrue(description.contains("legacy code"))
        assertTrue(description.contains("manual verification"))
        assertTrue(description.contains("project-specific ways"))
    }
}
