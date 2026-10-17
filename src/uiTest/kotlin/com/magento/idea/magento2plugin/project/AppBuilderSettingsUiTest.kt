/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.project

import com.intellij.driver.client.Remote
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.codeEditorForFile
import com.intellij.driver.sdk.ui.components.elements.checkBoxWithName
import com.intellij.driver.sdk.ui.components.elements.textField
import com.intellij.driver.sdk.ui.components.settings.settingsDialog
import com.intellij.driver.sdk.waitFor
import com.magento.idea.magento2plugin.ui.clickOkButton
import com.magento.idea.magento2plugin.ui.runWebStormUiTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AppBuilderSettingsUiTest {
    @Test
    fun `settings and navigation work without Magento`() {
        runWebStormUiTest("appBuilder/standalone-events", enableMagento = false) { projectPath ->
            ideFrame {
                waitForIndicators(5.minutes)
                openSettingsDialog()
                settingsDialog {
                    searchTextField.text = "Adobe App Builder"
                    waitFor("App Builder settings", 30.seconds) {
                        settingsTree.collectExpandedPaths().any { it.path.last() == "Adobe App Builder" }
                    }
                    settingsTree.clickRow { it == "Adobe App Builder" }
                    content {
                        val toggle = checkBoxWithName("Enable Adobe App Builder integration").waitFound()
                        check(!toggle.isSelected())
                        driver.withContext(OnDispatcher.EDT) {
                            driver.cast(toggle.component, AppBuilderToggle::class).doClick()
                        }
                        check(toggle.isSelected())
                        textField().text = projectPath.toString()
                    }
                    clickOkButton()
                }
                openSettingsDialog()
                settingsDialog {
                    searchTextField.text = "Adobe App Builder"
                    waitFor("App Builder settings after reopening", 30.seconds) {
                        settingsTree.collectExpandedPaths().any { it.path.last() == "Adobe App Builder" }
                    }
                    settingsTree.clickRow { it == "Adobe App Builder" }
                    content {
                        check(checkBoxWithName("Enable Adobe App Builder integration").waitFound().isSelected())
                        check(textField().text == projectPath.toString())
                    }
                    clickOkButton()
                }
                driver.openFile("app.config.yaml")
                val configEditor = codeEditorForFile("app.config.yaml").waitFound()
                configEditor.moveCaretToOffset(configEditor.text.lastIndexOf("catalog/updated") + 1)
                driver.invokeAction("GotoDeclaration", component = configEditor.component)
                waitFor("Event navigates to action declaration", 30.seconds) {
                    configEditor.interact { getCaretModel().getOffset() } == configEditor.text.indexOf("updated:")
                }
                configEditor.moveCaretToOffset(configEditor.text.indexOf("actions/catalog/updated.js") + 1)
                driver.invokeAction("GotoDeclaration", component = configEditor.component)
                codeEditorForFile("updated.js").waitFound()
            }
        }
    }
}

@Remote("javax.swing.JCheckBox")
private interface AppBuilderToggle {
    fun doClick()
}
