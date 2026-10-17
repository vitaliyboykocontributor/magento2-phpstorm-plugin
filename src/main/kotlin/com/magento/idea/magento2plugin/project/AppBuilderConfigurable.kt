/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.project

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

/** Uses only shared platform APIs so the same settings page is available in every supported IDE. */
class AppBuilderConfigurable(private val project: Project) : SearchableConfigurable {
    private var panel: JPanel? = null
    private var enabled: JBCheckBox? = null
    private var directory: TextFieldWithBrowseButton? = null

    override fun getId(): String = "Magento2.AppBuilder"
    override fun getDisplayName(): String = "Adobe App Builder"

    override fun createComponent(): JComponent {
        panel?.let { return it }
        val toggle = JBCheckBox("Enable Adobe App Builder integration")
        val input = TextFieldWithBrowseButton()
        input.addBrowseFolderListener(project, FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select App Builder Application Directory")
            .withDescription("Select the root directory of your App Builder application."))
        val label = JBLabel("Application directory:")
        label.labelFor = input.textField
        toggle.addActionListener {
            input.isEnabled = toggle.isSelected
        }
        enabled = toggle
        directory = input
        panel = FormBuilder.createFormBuilder()
            .addComponent(toggle)
            .addLabeledComponent(label, input)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val toggle = enabled ?: return false
        val saved = AppBuilderSettings.getInstance(project).state
        return toggle.isSelected != saved.enabled ||
            AppBuilderSettings.normalizeDirectory(directory!!.text) != saved.directory
    }

    override fun apply() {
        val toggle = enabled ?: return
        val path = AppBuilderSettings.normalizeDirectory(directory!!.text)
        if (toggle.isSelected) {
            val valid = try {
                path.isNotEmpty() && Path.of(path).isAbsolute && Files.isDirectory(Path.of(path))
            } catch (_: InvalidPathException) {
                false
            }
            if (!valid) {
                throw ConfigurationException("Select an existing App Builder application directory (absolute path).")
            }
        }
        AppBuilderSettings.getInstance(project).loadState(AppBuilderSettings.State(toggle.isSelected, path))
    }

    override fun reset() {
        val saved = AppBuilderSettings.getInstance(project).state
        enabled?.isSelected = saved.enabled
        directory?.apply {
            text = saved.directory
            isEnabled = saved.enabled
        }
    }

    override fun disposeUIResources() {
        directory?.let { Disposer.dispose(it) }
        directory = null
        enabled = null
        panel = null
    }
}
