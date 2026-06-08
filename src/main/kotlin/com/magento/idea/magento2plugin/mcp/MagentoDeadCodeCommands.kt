/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.magento.idea.magento2plugin.magento.files.ModuleDiXml
import org.json.JSONException
import org.json.JSONObject

internal object MagentoDeadCodeCommands {
    private const val VERIFICATION_WARNING =
        "This tool is best used after a long task or refactoring when the caller provides a specific recently updated or created module. It can be wrong on legacy code, and every found file or declaration requires manual verification because project-specific ways of initializing things can hide static references."

    private val supportedTypes = listOf(
        "unused_html_templates",
        "unused_js",
        "unused_plugins",
        "unused_observers",
        "all"
    )

    fun help(): String = """
        Magento dead code library

        Use this as a three-step lookup flow:
        1. `help`: choose a queryType from this compact catalog.
        2. `detailed_schema`: load parameters only for the chosen queryType.
        3. `query`: run the selected dead-code search.

        Recommended use: run this after completing a long task or refactoring and pass `moduleName` for the recently updated or created module.
        Important: $VERIFICATION_WARNING

        Queries:
        - `unused_html_templates`: Find unreferenced Knockout HTML template candidates.
        - `unused_js`: Find unreferenced RequireJS JavaScript module candidates.
        - `unused_plugins`: Find plugin declarations that cannot resolve to a valid intercepted method.
        - `unused_observers`: Find observer declarations with missing classes, missing execute methods, or no static event dispatch.
        - `all`: Run all dead-code searches.
    """.trimIndent()

    fun detailedSchema(queryType: String): String {
        val normalizedType = normalizeType(queryType)
        if (normalizedType !in supportedTypes) {
            return unknownTypeMessage(normalizedType)
        }

        return """
            queryType: $normalizedType
            Parameters:
            - scope: Optional. One of `app_code`, `vendor`, `theme`, `lib`, or `all`. Default: `all`.
            - moduleName: Optional. Magento module name such as `Vendor_Module`; filters module-owned candidates.
            - includeLowConfidence: Optional boolean. Default: false.
            - limit: Optional integer. Default: 100.
            - showEvidence: Optional boolean. Default: true.

            Important: $VERIFICATION_WARNING

            Example parametersJson:
            {"scope":"app_code","moduleName":"Vendor_Module","limit":50}
        """.trimIndent()
    }

    fun query(project: Project, queryType: String, parametersJson: String): String {
        val normalizedType = normalizeType(queryType)
        if (normalizedType !in supportedTypes) {
            return unknownTypeMessage(normalizedType)
        }

        val parameters = try {
            parseParameters(parametersJson)
        } catch (exception: JSONException) {
            return "parametersJson must be a valid JSON object. ${exception.message}"
        }
        val options = QueryOptions.from(parameters)

        return when (normalizedType) {
            "unused_html_templates" -> formatAssetResults(
                title = "Unused HTML template candidates",
                candidates = findUnusedHtmlTemplates(project, options),
                options = options
            )
            "unused_js" -> formatAssetResults(
                title = "Unused JavaScript candidates",
                candidates = findUnusedJs(project, options),
                options = options
            )
            "unused_plugins" -> formatDeclarationResults(
                title = "Unused plugin candidates",
                candidates = findUnusedPlugins(project, options),
                options = options
            )
            "unused_observers" -> formatDeclarationResults(
                title = "Unused observer candidates",
                candidates = findUnusedObservers(project, options),
                options = options
            )
            "all" -> listOf(
                formatAssetResults("Unused HTML template candidates", findUnusedHtmlTemplates(project, options), options),
                formatAssetResults("Unused JavaScript candidates", findUnusedJs(project, options), options),
                formatDeclarationResults("Unused plugin candidates", findUnusedPlugins(project, options), options),
                formatDeclarationResults("Unused observer candidates", findUnusedObservers(project, options), options)
            ).joinToString("\n\n")
            else -> unknownTypeMessage(normalizedType)
        }
    }

    private fun findUnusedHtmlTemplates(project: Project, options: QueryOptions): List<AssetCandidate> {
        val referenceFiles = collectReferenceFiles(project)
        return allFilesByExtension(project, "html")
            .asSequence()
            .filter { isHtmlTemplateCandidate(it) }
            .mapNotNull { file -> buildTemplateCandidate(project, file) }
            .filter { it.matches(options) }
            .filter { candidate -> !hasStaticReference(candidate.aliases, referenceFiles, candidate.file) }
            .sortedWith(compareBy<AssetCandidate>({ it.moduleName ?: "" }, { it.relativePath }))
            .take(options.limit)
            .toList()
    }

    private fun findUnusedJs(project: Project, options: QueryOptions): List<AssetCandidate> {
        val referenceFiles = collectReferenceFiles(project)
        return allFilesByExtension(project, "js")
            .asSequence()
            .filter { isJsCandidate(it) }
            .mapNotNull { file -> buildJsCandidate(project, file) }
            .filter { it.matches(options) }
            .filter { candidate -> !hasStaticReference(candidate.aliases, referenceFiles, candidate.file) }
            .sortedWith(compareBy<AssetCandidate>({ it.moduleName ?: "" }, { it.relativePath }))
            .take(options.limit)
            .toList()
    }

    private fun findUnusedPlugins(project: Project, options: QueryOptions): List<DeclarationCandidate> {
        val phpIndex = PhpIndex.getInstance(project)
        val result = mutableListOf<DeclarationCandidate>()

        for (xmlFile in MagentoMcpSupport.findXmlFilesByName(project, ModuleDiXml.FILE_NAME)) {
            ProgressManager.checkCanceled()
            val virtualFile = xmlFile.virtualFile ?: continue
            val fileScope = scopeOf(virtualFile)
            val moduleName = moduleNameFromPath(virtualFile.path)
            if (!matchesFilter(fileScope, moduleName, options)) {
                continue
            }
            val filePath = MagentoMcpSupport.relativePath(project, virtualFile)
            val rootTag = xmlFile.rootTag ?: continue
            for (typeTag in rootTag.findSubTags(ModuleDiXml.TYPE_TAG)) {
                val targetFqn = presentableFqn(typeTag.getAttributeValue(ModuleDiXml.NAME_ATTR)) ?: continue
                val targetClasses = MagentoMcpSupport.resolveTargetClasses(phpIndex, targetFqn)
                for (pluginTag in typeTag.findSubTags(ModuleDiXml.PLUGIN_TAG_NAME)) {
                    if (pluginTag.getAttributeValue(ModuleDiXml.DISABLED_ATTR_NAME).equals("true", ignoreCase = true)) {
                        continue
                    }
                    val pluginName = pluginTag.getAttributeValue(ModuleDiXml.NAME_ATTR) ?: "-"
                    val pluginType = presentableFqn(pluginTag.getAttributeValue(ModuleDiXml.TYPE_ATTR))
                    val reason = when {
                        targetClasses.isEmpty() -> "target type could not be resolved"
                        pluginType == null -> "plugin class is not declared"
                        phpIndex.getClassesByFQN(pluginType).isEmpty() -> "plugin class could not be resolved"
                        !hasMatchingPluginMethod(phpIndex.getClassesByFQN(pluginType), targetClasses) ->
                            "plugin class has no public before/around/after method matching any public target method"
                        else -> null
                    }
                    if (reason != null) {
                        result += DeclarationCandidate(
                            filePath = filePath,
                            summary = "plugin name=$pluginName target=$targetFqn type=${pluginType ?: "-"}",
                            reason = reason,
                            confidence = "high"
                        )
                    }
                }
            }
        }

        return result.sortedWith(compareBy<DeclarationCandidate>({ it.filePath }, { it.summary })).take(options.limit)
    }

    private fun findUnusedObservers(project: Project, options: QueryOptions): List<DeclarationCandidate> {
        val phpIndex = PhpIndex.getInstance(project)
        val dispatchedEvents = collectDispatchedEvents(project)
        val result = mutableListOf<DeclarationCandidate>()

        for (xmlFile in MagentoMcpSupport.findXmlFilesByName(project, "events.xml")) {
            ProgressManager.checkCanceled()
            val virtualFile = xmlFile.virtualFile ?: continue
            val fileScope = scopeOf(virtualFile)
            val moduleName = moduleNameFromPath(virtualFile.path)
            if (!matchesFilter(fileScope, moduleName, options)) {
                continue
            }
            val filePath = MagentoMcpSupport.relativePath(project, virtualFile)
            val rootTag = xmlFile.rootTag ?: continue
            for (eventTag in rootTag.findSubTags("event")) {
                val eventName = eventTag.getAttributeValue("name") ?: continue
                val eventIsDispatched = eventName in dispatchedEvents
                for (observerTag in eventTag.findSubTags("observer")) {
                    if (observerTag.getAttributeValue("disabled").equals("true", ignoreCase = true)) {
                        continue
                    }
                    val observerName = observerTag.getAttributeValue("name") ?: "-"
                    val observerInstance = presentableFqn(observerTag.getAttributeValue("instance"))
                    val observerClasses = observerInstance?.let { phpIndex.getClassesByFQN(it) }.orEmpty()
                    val reason = when {
                        observerInstance == null -> "observer class is not declared"
                        observerClasses.isEmpty() -> "observer class could not be resolved"
                        observerClasses.none { phpClass -> phpClass.methods.any { it.name == "execute" && it.access.isPublic } } ->
                            "observer class has no public execute method"
                        !eventIsDispatched -> "event has no static dispatch reference"
                        else -> null
                    }
                    if (reason != null) {
                        result += DeclarationCandidate(
                            filePath = filePath,
                            summary = "event=$eventName observer=$observerName instance=${observerInstance ?: "-"}",
                            reason = reason,
                            confidence = if (reason == "event has no static dispatch reference") "medium" else "high"
                        )
                    }
                }
            }
        }

        return result.sortedWith(compareBy<DeclarationCandidate>({ it.filePath }, { it.summary })).take(options.limit)
    }

    private fun hasMatchingPluginMethod(pluginClasses: Collection<PhpClass>, targetClasses: Collection<PhpClass>): Boolean {
        val targetMethodNames = targetClasses
            .flatMap { collectPublicMethodNames(it).asIterable() }
            .map { it.replaceFirstChar { char -> char.uppercase() } }
            .toSet()
        if (targetMethodNames.isEmpty()) {
            return false
        }

        return pluginClasses.any { pluginClass ->
            pluginClass.methods.any { method ->
                method.access.isPublic && targetMethodNames.any { targetMethodName ->
                    method.name == "before$targetMethodName" ||
                        method.name == "around$targetMethodName" ||
                        method.name == "after$targetMethodName"
                }
            }
        }
    }

    private fun collectPublicMethodNames(phpClass: PhpClass, visited: MutableSet<String> = mutableSetOf()): Set<String> {
        if (!visited.add(phpClass.fqn)) {
            return emptySet()
        }
        val methodNames = linkedSetOf<String>()
        methodNames += phpClass.methods
            .filter { it.access.isPublic }
            .map { it.name }
        for (parent in phpClass.supers) {
            methodNames += collectPublicMethodNames(parent, visited)
        }
        return methodNames
    }

    private fun collectDispatchedEvents(project: Project): Set<String> {
        val dispatchPattern = Regex("""->\s*dispatch\s*\(\s*['"]([^'"]+)['"]""")
        return allFilesByExtension(project, "php")
            .asSequence()
            .flatMap { file -> dispatchPattern.findAll(loadText(file)).map { it.groupValues[1] } }
            .toSet()
    }

    private fun collectReferenceFiles(project: Project): List<VirtualFile> {
        return listOf("js", "xml", "php", "phtml", "html")
            .flatMap { allFilesByExtension(project, it) }
            .distinctBy { it.url }
    }

    private fun hasStaticReference(
        aliases: Set<String>,
        referenceFiles: List<VirtualFile>,
        candidateFile: VirtualFile
    ): Boolean {
        if (aliases.isEmpty()) {
            return false
        }
        return referenceFiles.any { referenceFile ->
            ProgressManager.checkCanceled()
            if (referenceFile.url == candidateFile.url) {
                return@any false
            }
            val text = loadText(referenceFile)
            aliases.any { alias -> text.contains(alias) }
        }
    }

    private fun buildTemplateCandidate(project: Project, file: VirtualFile): AssetCandidate? {
        val moduleName = moduleNameFromPath(file.path) ?: return null
        val relativeFromWeb = relativePathAfterWeb(file.path) ?: return null
        val baseAlias = stripExtension(relativeFromWeb)
            .removePrefix("template/")
            .removePrefix("templates/")
        val aliases = linkedSetOf(
            "$moduleName/$baseAlias",
            "$moduleName/template/$baseAlias",
            "$moduleName/templates/$baseAlias"
        )
        aliases += "$moduleName/${stripExtension(relativeFromWeb)}"
        return AssetCandidate(
            file = file,
            relativePath = MagentoMcpSupport.relativePath(project, file),
            moduleName = moduleName,
            scope = scopeOf(file),
            aliases = aliases
        )
    }

    private fun buildJsCandidate(project: Project, file: VirtualFile): AssetCandidate? {
        val moduleName = moduleNameFromPath(file.path)
        val scope = scopeOf(file)
        val aliases = linkedSetOf<String>()
        if (moduleName != null) {
            val relativeFromWeb = relativePathAfterWeb(file.path) ?: return null
            val baseAlias = stripExtension(relativeFromWeb)
            aliases += "$moduleName/$baseAlias"
            if (baseAlias.startsWith("js/")) {
                aliases += "$moduleName/${baseAlias.removePrefix("js/")}"
            }
        } else if (scope == "lib") {
            val libPath = file.path.substringAfter("/lib/web/", "")
            if (libPath.isEmpty()) {
                return null
            }
            aliases += stripExtension(libPath)
        } else {
            return null
        }

        return AssetCandidate(
            file = file,
            relativePath = MagentoMcpSupport.relativePath(project, file),
            moduleName = moduleName,
            scope = scope,
            aliases = aliases
        )
    }

    private fun isHtmlTemplateCandidate(file: VirtualFile): Boolean {
        val path = file.path.replace('\\', '/')
        return !file.isDirectory &&
            file.extension == "html" &&
            (path.contains("/view/frontend/web/template/") ||
                path.contains("/view/frontend/web/templates/") ||
                path.contains("/view/adminhtml/web/template/") ||
                path.contains("/view/adminhtml/web/templates/") ||
                path.contains("/view/base/web/template/") ||
                path.contains("/view/base/web/templates/") ||
                path.contains("/app/design/") && (path.contains("/web/template/") || path.contains("/web/templates/")))
    }

    private fun isJsCandidate(file: VirtualFile): Boolean {
        val path = file.path.replace('\\', '/')
        if (file.isDirectory || file.extension != "js") {
            return false
        }
        if (file.name == "requirejs-config.js" || file.name.endsWith(".min.js") || path.contains("/generated/")) {
            return false
        }
        return path.contains("/view/frontend/web/") ||
            path.contains("/view/adminhtml/web/") ||
            path.contains("/view/base/web/") ||
            path.contains("/app/design/") && path.contains("/web/") ||
            path.contains("/lib/web/")
    }

    private fun formatAssetResults(title: String, candidates: List<AssetCandidate>, options: QueryOptions): String {
        val lines = mutableListOf(
            title,
            VERIFICATION_WARNING,
            "Found ${candidates.size} candidate(s)."
        )
        if (candidates.isEmpty()) {
            return lines.joinToString("\n")
        }
        for (candidate in candidates.take(options.limit)) {
            lines += ""
            lines += candidate.relativePath
            lines += "confidence: medium"
            if (options.showEvidence) {
                lines += "aliasesChecked: ${candidate.aliases.joinToString(", ")}"
            }
        }
        return lines.joinToString("\n")
    }

    private fun formatDeclarationResults(
        title: String,
        candidates: List<DeclarationCandidate>,
        options: QueryOptions
    ): String {
        val lines = mutableListOf(
            title,
            VERIFICATION_WARNING,
            "Found ${candidates.size} candidate(s)."
        )
        if (candidates.isEmpty()) {
            return lines.joinToString("\n")
        }
        for (candidate in candidates.take(options.limit)) {
            lines += ""
            lines += candidate.summary
            lines += "file: ${candidate.filePath}"
            lines += "reason: ${candidate.reason}"
            lines += "confidence: ${candidate.confidence}"
            if (options.showEvidence) {
                lines += "evidence: static PHP/XML validation found this declaration unresolved or unreferenced"
            }
        }
        return lines.joinToString("\n")
    }

    private fun allFilesByExtension(project: Project, extension: String): Collection<VirtualFile> {
        return FilenameIndex.getAllFilesByExt(project, extension, GlobalSearchScope.allScope(project))
    }

    private fun loadText(file: VirtualFile): String {
        return try {
            VfsUtilCore.loadText(file)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun parseParameters(parametersJson: String): JSONObject {
        val trimmed = parametersJson.trim()
        return if (trimmed.isEmpty()) {
            JSONObject()
        } else {
            JSONObject(trimmed)
        }
    }

    private fun normalizeType(queryType: String): String {
        return when (queryType.trim().lowercase().replace('-', '_')) {
            "templates", "html_templates", "unused_templates" -> "unused_html_templates"
            "js", "javascript", "unused_javascript" -> "unused_js"
            "plugins" -> "unused_plugins"
            "observers" -> "unused_observers"
            else -> queryType.trim().lowercase().replace('-', '_')
        }
    }

    private fun unknownTypeMessage(queryType: String): String {
        return "Unknown queryType `$queryType`. Supported values: ${supportedTypes.joinToString(", ")}. Call magento_dead_code with mode `help` for the compact catalog."
    }

    private fun presentableFqn(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }
        return PhpLangUtil.toPresentableFQN(trimmed.removePrefix("\\"))
    }

    private fun scopeOf(file: VirtualFile): String = when {
        file.path.contains("/app/code/") -> "app_code"
        file.path.contains("/app/design/") -> "theme"
        file.path.contains("/lib/web/") -> "lib"
        file.path.contains("/vendor/") -> "vendor"
        else -> "all"
    }

    private fun moduleNameFromPath(rawPath: String): String? {
        val path = rawPath.replace('\\', '/')
        path.substringAfterOrNull("/app/code/")?.let { tail ->
            val parts = tail.split('/')
            if (parts.size >= 2) {
                return "${parts[0]}_${parts[1]}"
            }
        }
        path.substringAfterOrNull("/vendor/")?.let { tail ->
            val parts = tail.split('/')
            if (parts.size >= 2 && parts[1].startsWith("module-")) {
                return "${parts[0].replaceFirstChar { it.uppercase() }}_${kebabToPascal(parts[1].removePrefix("module-"))}"
            }
        }
        path.substringAfterOrNull("/app/design/")?.let { tail ->
            val parts = tail.split('/')
            if (parts.size >= 5 && parts[4] == "web" && parts[3].contains('_')) {
                return parts[3]
            }
        }
        return null
    }

    private fun relativePathAfterWeb(rawPath: String): String? {
        val path = rawPath.replace('\\', '/')
        val marker = "/web/"
        val index = path.indexOf(marker)
        if (index < 0) {
            return null
        }
        return path.substring(index + marker.length)
    }

    private fun stripExtension(path: String): String {
        val extensionIndex = path.lastIndexOf('.')
        return if (extensionIndex > 0) path.substring(0, extensionIndex) else path
    }

    private fun kebabToPascal(value: String): String {
        return value.split('-')
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun String.substringAfterOrNull(delimiter: String): String? {
        val index = indexOf(delimiter)
        return if (index < 0) null else substring(index + delimiter.length)
    }

    private fun AssetCandidate.matches(options: QueryOptions): Boolean {
        return matchesFilter(scope, moduleName, options)
    }

    private fun matchesFilter(scope: String, moduleName: String?, options: QueryOptions): Boolean {
        if (options.scope != "all" && scope != options.scope) {
            return false
        }
        if (options.moduleName != null && moduleName != options.moduleName) {
            return false
        }
        return true
    }

    private data class QueryOptions(
        val scope: String,
        val moduleName: String?,
        val includeLowConfidence: Boolean,
        val showEvidence: Boolean,
        val limit: Int
    ) {
        companion object {
            fun from(parameters: JSONObject): QueryOptions {
                val scope = parameters.optString("scope", "all").trim().lowercase().replace('-', '_')
                return QueryOptions(
                    scope = if (scope in setOf("app_code", "vendor", "theme", "lib", "all")) scope else "all",
                    moduleName = parameters.optString("moduleName", "").trim().ifEmpty { null },
                    includeLowConfidence = parameters.optBoolean("includeLowConfidence", false),
                    showEvidence = parameters.optBoolean("showEvidence", true),
                    limit = parameters.optInt("limit", 100).coerceIn(1, 1000)
                )
            }
        }
    }

    private data class AssetCandidate(
        val file: VirtualFile,
        val relativePath: String,
        val moduleName: String?,
        val scope: String,
        val aliases: Set<String>
    )

    private data class DeclarationCandidate(
        val filePath: String,
        val summary: String,
        val reason: String,
        val confidence: String
    )
}
