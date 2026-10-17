/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.project

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.xmlb.XmlSerializer
import com.magento.idea.magento2plugin.PhysicalMagentoTestCase
import org.junit.Assert.assertThrows

class AppBuilderConfigurableTest : PhysicalMagentoTestCase() {
    private lateinit var form: AppBuilderConfigurable

    override fun setUp() {
        super.setUp()
        form = AppBuilderConfigurable(project)
        form.createComponent()
    }

    override fun tearDown() {
        try {
            form.disposeUIResources()
        } finally {
            super.tearDown()
        }
    }

    private inline fun <reified T> field(name: String): T =
        AppBuilderConfigurable::class.java.getDeclaredField(name).apply { isAccessible = true }.get(form) as T

    fun testEnableApplyPersistResetAndDisableWithoutMagento() {
        val file = myFixture.addFileToProject("app-builder/package.json", "{}").virtualFile
        val input = field<TextFieldWithBrowseButton>("directory")
        val toggle = field<JBCheckBox>("enabled")
        val settings = AppBuilderSettings.getInstance(project)
        Settings.getInstance(project).pluginEnabled = false
        assertFalse(toggle.isSelected)
        assertFalse(input.isEnabled)
        assertFalse(form.isModified)

        toggle.doClick()
        assertTrue(input.isEnabled)
        input.text = "  ${file.parent.path}/  "
        assertTrue(form.isModified)
        form.apply()
        assertEquals(file.parent.path, settings.state.directory)
        assertTrue(settings.state.enabled)
        assertFalse(Settings.isEnabled(project))
        assertFalse(form.isModified)
        assertTrue(settings.isEnabledFor(file))
        assertFalse(settings.isEnabledFor(myFixture.addFileToProject("other/package.json", "{}").virtualFile))

        val reloaded = AppBuilderSettings()
        reloaded.loadState(XmlSerializer.deserialize(XmlSerializer.serialize(settings.state), AppBuilderSettings.State::class.java))
        assertEquals(settings.state, reloaded.state)
        assertEquals(file.parent, reloaded.getRootDirectory())

        input.text = "unsaved"
        toggle.doClick()
        form.reset()
        assertTrue(toggle.isSelected)
        assertTrue(input.isEnabled)
        assertEquals(file.parent.path, input.text)
        toggle.doClick()
        form.apply()
        assertNull(settings.getRootDirectory())
        assertFalse(settings.isEnabledFor(file))
        assertEquals(file.parent.path, settings.state.directory)
    }

    fun testInvalidDirectoryDoesNotChangeSavedConfiguration() {
        val file = myFixture.addFileToProject("app-builder/package.json", "{}").virtualFile
        val input = field<TextFieldWithBrowseButton>("directory")
        field<JBCheckBox>("enabled").doClick()
        input.text = file.parent.path
        form.apply()
        val saved = AppBuilderSettings.getInstance(project).state
        for (invalid in listOf("", "relative/path", "${file.parent.path}/missing", file.path)) {
            input.text = invalid
            assertThrows(ConfigurationException::class.java) { form.apply() }
            assertEquals(saved, AppBuilderSettings.getInstance(project).state)
        }
        // A missing application directory must not prevent disabling the integration.
        field<JBCheckBox>("enabled").doClick()
        form.apply()
        assertFalse(AppBuilderSettings.getInstance(project).state.enabled)
    }

    fun testUiCanBeDisposedAndRecreatedAndStateIsNotAliased() {
        val settings = AppBuilderSettings.getInstance(project)
        val state = AppBuilderSettings.State(true, project.basePath!!)
        settings.loadState(state)
        state.enabled = false
        settings.state.directory = "changed"
        assertTrue(settings.state.enabled)
        assertEquals(project.basePath, settings.state.directory)
        form.disposeUIResources()
        form.createComponent()
        assertTrue(field<JBCheckBox>("enabled").isSelected)
        assertEquals(project.basePath, field<TextFieldWithBrowseButton>("directory").text)
        assertFalse(form.isModified)
    }
}
