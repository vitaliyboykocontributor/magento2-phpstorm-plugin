/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp

import com.intellij.mcpserver.annotations.McpDescription
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagentoMcpScaffoldToolDescriptionTest {
    @Test
    fun testMagentoScaffoldToolDescriptionExplainsThreeStepFlow() {
        val method = MagentoMcpToolset::class.java.declaredMethods.first { toolMethod ->
            toolMethod.name == "magentoScaffold" && toolMethod.getAnnotation(McpDescription::class.java) != null
        }

        val description = method.getAnnotation(McpDescription::class.java)?.description
        assertNotNull(description)
        assertTrue(description!!.contains("mode `help`"))
        assertTrue(description.contains("mode `detailed_schema`"))
        assertTrue(description.contains("mode `render`"))
        assertTrue(description.contains("parametersJson"))
        assertTrue(description.contains("entity_crud"))
        assertTrue(description.contains("product_eav_attribute"))
        assertTrue(description.contains("list, load one schema, then render"))
    }
}
