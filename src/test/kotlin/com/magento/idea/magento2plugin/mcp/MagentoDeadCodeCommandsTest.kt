/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp

import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.magento.idea.magento2plugin.BaseProjectTestCase
import org.junit.Test

class MagentoDeadCodeCommandsTest : BaseProjectTestCase() {
    @Test
    fun testHelpExplainsVerificationRequirementAndSupportedQueries() {
        val result = MagentoDeadCodeCommands.help()

        assertContains(result, "Magento dead code library")
        assertContains(result, "`help`")
        assertContains(result, "`detailed_schema`")
        assertContains(result, "`query`")
        assertContains(result, "`unused_html_templates`")
        assertContains(result, "`unused_js`")
        assertContains(result, "`unused_plugins`")
        assertContains(result, "`unused_observers`")
        assertContains(result, "long task or refactoring")
        assertContains(result, "recently updated or created module")
        assertContains(result, "legacy code")
        assertContains(result, "manual verification")
        assertContains(result, "project-specific ways of initializing things")
    }

    @Test
    fun testDetailedSchemaExplainsParameters() {
        val result = MagentoDeadCodeCommands.detailedSchema("all")

        assertContains(result, "queryType: all")
        assertContains(result, "scope")
        assertContains(result, "moduleName")
        assertContains(result, "includeLowConfidence")
        assertContains(result, "limit")
    }

    @Test
    fun testQueryReportsMissingVerificationWarningForInvalidJsonToo() {
        val result = MagentoDeadCodeCommands.query(project, "all", "{")

        assertContains(result, "parametersJson must be a valid JSON object")
    }

    @Test
    fun testUnusedHtmlTemplatesReportsOnlyUnreferencedTemplates() {
        addModuleSkeleton()
        myFixture.addFileToProject(
            "app/code/Dead/Assets/view/frontend/web/template/used-template.html",
            "<div>used</div>"
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/view/frontend/web/template/dead-template.html",
            "<div>dead</div>"
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/view/frontend/web/js/uses-template.js",
            """
            define([], function () {
                return {
                    defaults: {
                        template: 'Dead_Assets/used-template'
                    }
                };
            });
            """.trimIndent()
        )
        waitForIndexes()

        val result = MagentoDeadCodeCommands.query(
            project,
            "unused_html_templates",
            """{"scope":"app_code","moduleName":"Dead_Assets","limit":20}"""
        )

        assertContains(result, "Unused HTML template candidates")
        assertContains(result, "manual verification")
        assertContainsPath(result, "app/code/Dead/Assets/view/frontend/web/template/dead-template.html")
        assertDoesNotContain(result, "used-template.html")
    }

    @Test
    fun testUnusedJsReportsOnlyUnreferencedRequireJsModules() {
        addModuleSkeleton()
        myFixture.addFileToProject(
            "app/code/Dead/Assets/view/frontend/web/js/used-widget.js",
            "define([], function () { return {}; });"
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/view/frontend/web/js/dead-widget.js",
            "define([], function () { return {}; });"
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/view/frontend/templates/init.phtml",
            """
            <div data-mage-init='{"Dead_Assets/js/used-widget": {}}'></div>
            """.trimIndent()
        )
        waitForIndexes()

        val result = MagentoDeadCodeCommands.query(
            project,
            "unused_js",
            """{"scope":"app_code","moduleName":"Dead_Assets","limit":20}"""
        )

        assertContains(result, "Unused JavaScript candidates")
        assertContainsPath(result, "app/code/Dead/Assets/view/frontend/web/js/dead-widget.js")
        assertDoesNotContain(result, "used-widget.js")
        assertDoesNotContain(result, "requirejs-config.js")
    }

    @Test
    fun testUnusedPluginsReportsInvalidDeclarationsAndSkipsValidPlugin() {
        addModuleSkeleton()
        myFixture.addFileToProject(
            "app/code/Dead/Assets/Model/Target.php",
            """
            <?php
            namespace Dead\Assets\Model;
            class Target
            {
                public function save()
                {
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/Plugin/ValidPlugin.php",
            """
            <?php
            namespace Dead\Assets\Plugin;
            class ValidPlugin
            {
                public function beforeSave()
                {
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/Plugin/NoMethodPlugin.php",
            """
            <?php
            namespace Dead\Assets\Plugin;
            class NoMethodPlugin
            {
                public function unrelated()
                {
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/etc/di.xml",
            """
            <?xml version="1.0"?>
            <config>
                <type name="Dead\Assets\Model\Target">
                    <plugin name="valid_plugin" type="Dead\Assets\Plugin\ValidPlugin"/>
                    <plugin name="no_method_plugin" type="Dead\Assets\Plugin\NoMethodPlugin"/>
                    <plugin name="missing_plugin_class" type="Dead\Assets\Plugin\MissingPlugin"/>
                </type>
                <type name="Dead\Assets\Model\MissingTarget">
                    <plugin name="missing_target_plugin" type="Dead\Assets\Plugin\ValidPlugin"/>
                </type>
            </config>
            """.trimIndent()
        )
        waitForIndexes()

        val result = MagentoDeadCodeCommands.query(
            project,
            "unused_plugins",
            """{"scope":"app_code","moduleName":"Dead_Assets","limit":20}"""
        )

        assertContains(result, "Unused plugin candidates")
        assertContains(result, "plugin name=no_method_plugin")
        assertContains(result, "reason: plugin class has no public before/around/after method matching any public target method")
        assertContains(result, "plugin name=missing_plugin_class")
        assertContains(result, "reason: plugin class could not be resolved")
        assertContains(result, "plugin name=missing_target_plugin")
        assertContains(result, "reason: target type could not be resolved")
        assertDoesNotContain(result, "plugin name=valid_plugin")
    }

    @Test
    fun testUnusedObserversReportsUndispatchedEventsAndInvalidObserverClasses() {
        addModuleSkeleton()
        myFixture.addFileToProject(
            "app/code/Dead/Assets/Observer/UsedObserver.php",
            """
            <?php
            namespace Dead\Assets\Observer;
            class UsedObserver
            {
                public function execute()
                {
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/Observer/NoExecuteObserver.php",
            """
            <?php
            namespace Dead\Assets\Observer;
            class NoExecuteObserver
            {
                public function notExecute()
                {
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/Model/Dispatcher.php",
            """
            <?php
            namespace Dead\Assets\Model;
            class Dispatcher
            {
                public function run(${'$'}eventManager)
                {
                    ${'$'}eventManager->dispatch('dead_assets_used_event');
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/etc/events.xml",
            """
            <?xml version="1.0"?>
            <config>
                <event name="dead_assets_used_event">
                    <observer name="used_observer" instance="Dead\Assets\Observer\UsedObserver"/>
                </event>
                <event name="dead_assets_unused_event">
                    <observer name="unused_observer" instance="Dead\Assets\Observer\UsedObserver"/>
                    <observer name="missing_observer" instance="Dead\Assets\Observer\MissingObserver"/>
                    <observer name="no_execute_observer" instance="Dead\Assets\Observer\NoExecuteObserver"/>
                </event>
            </config>
            """.trimIndent()
        )
        waitForIndexes()

        val result = MagentoDeadCodeCommands.query(
            project,
            "unused_observers",
            """{"scope":"app_code","moduleName":"Dead_Assets","limit":20}"""
        )

        assertContains(result, "Unused observer candidates")
        assertContains(result, "observer=unused_observer")
        assertContains(result, "reason: event has no static dispatch reference")
        assertContains(result, "observer=missing_observer")
        assertContains(result, "reason: observer class could not be resolved")
        assertContains(result, "observer=no_execute_observer")
        assertContains(result, "reason: observer class has no public execute method")
        assertDoesNotContain(result, "observer=used_observer")
    }

    private fun addModuleSkeleton() {
        myFixture.addFileToProject(
            "app/code/Dead/Assets/etc/module.xml",
            """
            <?xml version="1.0"?>
            <config>
                <module name="Dead_Assets"/>
            </config>
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/code/Dead/Assets/registration.php",
            """
            <?php
            use Magento\Framework\Component\ComponentRegistrar;
            ComponentRegistrar::register(ComponentRegistrar::MODULE, 'Dead_Assets', __DIR__);
            """.trimIndent()
        )
    }

    private fun waitForIndexes() {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    private fun assertContains(text: String, expected: String) {
        assertTrue("Expected to find <$expected> in:\n$text", text.contains(expected))
    }

    private fun assertContainsPath(text: String, expectedPathSuffix: String) {
        val normalized = text.replace('\\', '/')
        assertTrue("Expected to find path suffix <$expectedPathSuffix> in:\n$text", normalized.contains(expectedPathSuffix))
    }

    private fun assertDoesNotContain(text: String, unexpected: String) {
        assertTrue("Did not expect to find <$unexpected> in:\n$text", !text.contains(unexpected))
    }
}
