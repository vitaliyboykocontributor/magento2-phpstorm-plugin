/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.appbuilder

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.magento.idea.magento2plugin.PhysicalMagentoTestCase
import com.magento.idea.magento2plugin.project.AppBuilderSettings
import java.nio.file.Path

class AppBuilderNavigationTest : PhysicalMagentoTestCase() {
    override fun getTestDataPath(): String = Path.of("src/test/testData/appBuilder").toAbsolutePath().toString()

    private fun enable(fixture: String, root: String = ".") {
        val directory = myFixture.copyDirectoryToProject(fixture, fixture)
        val selected = if (root == ".") directory else directory.findFileByRelativePath(root)!!
        AppBuilderSettings.getInstance(project).loadState(AppBuilderSettings.State(true, selected.path))
    }

    private fun references(file: String, text: String): List<PsiReference> {
        val virtualFile = myFixture.findFileInTempDir(file)
        val psi = PsiManager.getInstance(project).findFile(virtualFile)!!
        val offset = psi.text.lastIndexOf(text).also { assertTrue("Missing $text in $file", it >= 0) } + 1
        return generateSequence(psi.findElementAt(offset)) { it.parent }
            .flatMap { it.references.asSequence() }
            .filter { it.rangeInElement.shiftRight(it.element.textRange.startOffset).contains(offset) }
            .toList()
    }

    private fun targets(file: String, text: String): List<PsiElement> = references(file, text).flatMap {
        if (it is PsiPolyVariantReference) it.multiResolve(false).mapNotNull { result -> result.element }
        else listOfNotNull(it.resolve())
    }

    private fun assertTarget(file: String, text: String, target: String, declaration: String? = null) {
        val matching = targets(file, text).filter { it.containingFile.virtualFile == myFixture.findFileInTempDir(target) }
        assertTrue("$file: $text must resolve to $target", matching.isNotEmpty())
        if (declaration != null) assertTrue(matching.any { it.text.startsWith("$declaration:") })
    }

    fun testStandaloneEventNavigatesToActionThenHandler() {
        enable("standalone-events")
        assertTarget("standalone-events/app.config.yaml", "catalog/updated", "standalone-events/app.config.yaml", "updated")
        assertTarget("standalone-events/app.config.yaml", "actions/catalog/updated.js", "standalone-events/actions/catalog/updated.js")
    }

    fun testCommerceEventsResolveThroughNestedIncludesAndKeepPackageIdentity() {
        enable("commerce-integration")
        val config = "commerce-integration/app.commerce.config.ts"
        for ((pkg, flow) in listOf("order-commerce" to "commerce", "order-backoffice" to "external")) {
            val directory = "commerce-integration/src/commerce-extensibility-1/actions/order/$flow"
            assertTarget(config, "$pkg/updated", "$directory/actions.config.yaml", "updated")
            assertTarget("$directory/actions.config.yaml", "updated/index.js", "$directory/updated/index.js")
        }
        assertTarget(config, "audit/record", "commerce-integration/src/commerce-extensibility-1/ext.config.yaml", "record")
        assertTarget("commerce-integration/app.config.yaml", "src/commerce-extensibility-1/ext.config.yaml", "commerce-integration/src/commerce-extensibility-1/ext.config.yaml")
    }

    fun testWebhookNavigatesToActionAndHandler() {
        enable("checkout-webhook")
        val ext = "checkout-webhook/src/commerce-extensibility-1/ext.config.yaml"
        assertTarget("checkout-webhook/app.commerce.config.ts", "webhook/check-stock", ext, "check-stock")
        assertTarget(ext, "actions/check-stock/index.js", "checkout-webhook/src/commerce-extensibility-1/actions/check-stock/index.js")
    }

    fun testDisabledIntegrationAndUnselectedSiblingHaveNoAppBuilderReferences() {
        enable("multi-app-workspace", "apps/catalog")
        val root = "multi-app-workspace/apps"
        assertTarget("$root/catalog/app.config.yaml", "actions/updated/index.js", "$root/catalog/actions/updated/index.js")
        assertTrue(references("$root/orders/app.config.yaml", "actions/updated/index.js").none { it.javaClass.name.contains("appbuilder") })
        AppBuilderSettings.getInstance(project).loadState(AppBuilderSettings.State())
        assertTrue(references("$root/catalog/app.config.yaml", "actions/updated/index.js").none { it.javaClass.name.contains("appbuilder") })
        val orders = myFixture.findFileInTempDir("$root/orders")
        AppBuilderSettings.getInstance(project).loadState(AppBuilderSettings.State(true, orders.path))
        assertTarget("$root/orders/app.config.yaml", "actions/updated/index.js", "$root/orders/actions/updated/index.js")
        assertTrue(references("$root/catalog/app.config.yaml", "actions/updated/index.js").none { it.javaClass.name.contains("appbuilder") })
    }

    fun testBrokenReferencesAreUnresolvedWithoutInventingTargets() {
        enable("incomplete-app")
        for (value in listOf("catalog/old-name", "actions/deleted/index.js", "src/missing/ext.config.yaml")) {
            val refs = references("incomplete-app/app.config.yaml", value)
            assertTrue("Expected a navigable reference for $value", refs.any { it.javaClass.name.contains("appbuilder") })
            assertTrue(targets("incomplete-app/app.config.yaml", value).isEmpty())
        }
    }

    fun testUnrelatedPropertiesAndUnreachableManifestAreIgnored() {
        enable("commerce-integration")
        myFixture.addFileToProject("commerce-integration/unrelated.yaml", "function: missing.js\nruntime_action: order-commerce/updated\n")
        assertTrue(references("commerce-integration/unrelated.yaml", "order-commerce/updated").none { it.javaClass.name.contains("appbuilder") })
        myFixture.addFileToProject("commerce-integration/other.ts", "const x = { runtimeActions: ['order-commerce/updated'] };")
        assertTrue(references("commerce-integration/other.ts", "order-commerce/updated").none { it.javaClass.name.contains("appbuilder") })
    }

    fun testUnsavedManifestChangesInvalidateNavigation() {
        enable("standalone-events")
        val file = "standalone-events/app.config.yaml"
        assertTarget(file, "catalog/updated", file, "updated")
        val psi = PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir(file))!!
        val document = PsiDocumentManager.getInstance(project).getDocument(psi)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(document.text.replace("          updated:", "          renamed:"))
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        assertTrue(targets(file, "catalog/updated").isEmpty())
    }

    fun testCreatingMissingIncludeAndHandlerUpdatesExistingReferences() {
        enable("incomplete-app")
        val config = "incomplete-app/app.config.yaml"
        assertTrue(targets(config, "src/missing/ext.config.yaml").isEmpty())
        myFixture.addFileToProject("incomplete-app/src/missing/ext.config.yaml", "runtimeManifest: {}\n")
        assertTarget(config, "src/missing/ext.config.yaml", "incomplete-app/src/missing/ext.config.yaml")
        assertTrue(targets(config, "actions/deleted/index.js").isEmpty())
        myFixture.addFileToProject("incomplete-app/actions/deleted/index.js", "exports.main = async () => ({});")
        assertTarget(config, "actions/deleted/index.js", "incomplete-app/actions/deleted/index.js")
    }

    fun testIncludeCycleAndEscapingPathsDoNotResolveOutsideSelectedApp() {
        enable("standalone-events")
        myFixture.addFileToProject("outside.js", "exports.main = async () => ({});")
        myFixture.addFileToProject("standalone-events/app.config.yaml", """
            application:
              runtimeManifest:
                packages:
                  demo:
                    actions:
                      escape:
                        function: ../outside.js
            extensions:
              cyclic:
                ${'$'}include: cycle.yaml
        """.trimIndent())
        myFixture.addFileToProject("standalone-events/cycle.yaml", "${'$'}include: app.config.yaml\n")
        assertTrue(targets("standalone-events/app.config.yaml", "../outside.js").isEmpty())
        assertTarget("standalone-events/app.config.yaml", "cycle.yaml", "standalone-events/cycle.yaml")
    }
}
