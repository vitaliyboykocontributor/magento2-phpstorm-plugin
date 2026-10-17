/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.project

import com.magento.idea.magento2plugin.PhysicalMagentoTestCase
import org.json.JSONArray
import java.nio.file.Path
import kotlin.io.path.readText

class AppBuilderProjectFixturesTest : PhysicalMagentoTestCase() {
    override fun getTestDataPath(): String = Path.of("src/test/testData/appBuilder").toAbsolutePath().toString()

    fun testSelectedRootsScopeRealProjectsAndDoNotLeakIntoSiblingApps() {
        val catalog = JSONArray(Path.of(testDataPath, "catalog.json").readText())
        val settings = AppBuilderSettings.getInstance(project)
        Settings.getInstance(project).pluginEnabled = false
        for (index in 0 until catalog.length()) {
            val fixture = catalog.getJSONObject(index)
            val name = fixture.getString("name")
            val workspace = myFixture.copyDirectoryToProject(name, name)
            val roots = fixture.getJSONArray("roots")
            for (rootIndex in 0 until roots.length()) {
                val relative = roots.getString(rootIndex)
                val root = if (relative == ".") workspace else workspace.findFileByRelativePath(relative)!!
                settings.loadState(AppBuilderSettings.State(true, root.path))
                assertTrue(name, settings.isEnabledFor(root.findChild("app.config.yaml")!!))
                val references = fixture.getJSONArray("navigation")
                for (refIndex in 0 until references.length()) {
                    val targetPath = references.getJSONObject(refIndex).getString("target")
                    val target = workspace.findFileByRelativePath(targetPath)!!
                    val belongsToRoot = relative == "." || targetPath.startsWith("$relative/")
                    assertEquals("$name/$targetPath selected $relative", belongsToRoot, settings.isEnabledFor(target))
                }
                val outside = fixture.getJSONArray("outside")
                for (outsideIndex in 0 until outside.length()) {
                    assertFalse(settings.isEnabledFor(workspace.findFileByRelativePath(outside.getString(outsideIndex))!!))
                }
                settings.loadState(AppBuilderSettings.State(false, root.path))
                assertFalse(settings.isEnabledFor(root))
            }
            if (roots.length() == 0) {
                // The negative detection fixture stays outside every explicitly selected application.
                settings.loadState(settings.state.copy(enabled = true))
                assertFalse(settings.isEnabledFor(workspace.findChild("index.js")!!))
            }
        }
        assertFalse(Settings.isEnabled(project))
    }
}
