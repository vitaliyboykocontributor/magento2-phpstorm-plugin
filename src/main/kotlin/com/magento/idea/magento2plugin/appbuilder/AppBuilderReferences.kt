/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.appbuilder

import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ProcessingContext
import com.magento.idea.magento2plugin.project.AppBuilderSettings
import org.jetbrains.yaml.psi.*

/** Registered only when the YAML plugin is available; PHP is not required. */
class AppBuilderReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val provider = object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val settings = AppBuilderSettings.getInstance(element.project)
                val file = element.containingFile?.virtualFile ?: return PsiReference.EMPTY_ARRAY
                if (!settings.isEnabledFor(file)) return PsiReference.EMPTY_ARRAY
                val root = settings.getRootDirectory() ?: return PsiReference.EMPTY_ARRAY
                val commerce = if (element is JSLiteralExpression) {
                    commerceLink(element, root) ?: return PsiReference.EMPTY_ARRAY
                } else null
                if (element is YAMLScalar &&
                    (element.parent as? YAMLKeyValue)?.keyText !in setOf("\$include", "function", "runtime_action")) {
                    return PsiReference.EMPTY_ARRAY
                }
                val graph = AppBuilderGraph.get(element.manager, root) ?: return PsiReference.EMPTY_ARRAY
                val link = when (element) {
                    is YAMLScalar -> graph.links[element]
                    is JSLiteralExpression -> commerce
                    else -> null
                } ?: return PsiReference.EMPTY_ARRAY
                return arrayOf(AppBuilderReference(element, link, graph))
            }
        }
        registrar.registerReferenceProvider(psiElement(YAMLScalar::class.java), provider)
        registrar.registerReferenceProvider(psiElement(JSLiteralExpression::class.java), provider)
    }

    private fun commerceLink(element: JSLiteralExpression, root: VirtualFile): Link? {
        val file = element.containingFile.virtualFile
        if (file.parent != root || file.name !in setOf("app.commerce.config.ts", "app.commerce.config.js")) return null
        val value = element.stringValue ?: return null
        val properties = generateSequence(element.parent) { it.parent }.filterIsInstance<JSProperty>().map { it.name }.toList()
        val event = properties == listOf("runtimeActions", "events", "commerce", "eventing") ||
            properties == listOf("runtimeActions", "events", "external", "eventing")
        val webhook = properties == listOf("runtimeAction", "webhooks")
        return if (event || webhook) Link.Action(value) else null
    }
}

private sealed interface Link {
    data class Action(val name: String) : Link
    data class File(val path: String) : Link
}

private class AppBuilderReference(element: PsiElement, private val link: Link, private val graph: AppBuilderGraph) :
    PsiPolyVariantReferenceBase<PsiElement>(element, ElementManipulators.getValueTextRange(element), true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val targets = when (link) {
            is Link.Action -> graph.actions[link.name].orEmpty()
            is Link.File -> listOfNotNull(graph.file(element, link.path))
        }
        return targets.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

/** Traverses only the selected app's manifest and its includes, using committed editor PSI. */
private class AppBuilderGraph(private val manager: PsiManager, private val root: VirtualFile) {
    companion object {
        private val CACHE = Key.create<CachedValue<AppBuilderGraph>>("magento.appBuilder.navigationGraph")

        fun get(manager: PsiManager, root: VirtualFile): AppBuilderGraph? {
            val app = root.findChild("app.config.yaml")?.let(manager::findFile) as? YAMLFile ?: return null
            return CachedValuesManager.getManager(manager.project).getCachedValue(app, CACHE, {
                CachedValueProvider.Result.create(AppBuilderGraph(manager, root), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)
        }
    }

    val links = mutableMapOf<PsiElement, Link>()
    val actions = mutableMapOf<String, MutableList<YAMLKeyValue>>()

    init {
        val app = root.findChild("app.config.yaml")?.let { manager.findFile(it) as? YAMLFile }
        val config = mapping(app?.documents?.firstOrNull()?.topLevelValue)
        section(config?.getKeyValueByKey("application")?.value)
        mapping(config?.getKeyValueByKey("extensions")?.value)?.keyValues?.forEach { section(it.value) }
    }

    fun file(source: PsiElement, path: String): PsiFile? {
        if (path.startsWith("/") || path.contains('$') || path.contains(":") || path.contains('\\')) return null
        val candidate = source.containingFile.virtualFile.parent.findFileByRelativePath(path) ?: return null
        if (!com.intellij.openapi.vfs.VfsUtilCore.isAncestor(root, candidate, false) || candidate.isDirectory) return null
        return manager.findFile(candidate)
    }

    private fun mapping(value: YAMLValue?, visited: Set<PsiElement> = emptySet()): YAMLMapping? {
        ProgressManager.checkCanceled()
        val map = value as? YAMLMapping ?: return null
        if (map in visited || visited.size >= 64) return null
        val include = map.getKeyValueByKey("\$include")?.value as? YAMLScalar ?: return map
        links[include] = Link.File(include.textValue)
        val included = file(include, include.textValue) as? YAMLFile ?: return null
        return mapping(included.documents.firstOrNull()?.topLevelValue, visited + map)
    }

    private fun section(value: YAMLValue?) {
        val section = mapping(value) ?: return
        val manifest = mapping(section.getKeyValueByKey("runtimeManifest")?.value)
        val packages = mapping(manifest?.getKeyValueByKey("packages")?.value)
        packages?.keyValues?.forEach { pkg ->
            val definition = mapping(pkg.value)
            mapping(definition?.getKeyValueByKey("actions")?.value)?.keyValues?.forEach { action ->
                actions.getOrPut("${pkg.keyText}/${action.keyText}") { mutableListOf() }.add(action)
                val handler = mapping(action.value)?.getKeyValueByKey("function")?.value as? YAMLScalar
                if (handler != null) links[handler] = Link.File(handler.textValue)
            }
        }
        val events = mapping(section.getKeyValueByKey("events")?.value)
        mapping(events?.getKeyValueByKey("registrations")?.value)?.keyValues?.forEach { registration ->
            val action = mapping(registration.value)?.getKeyValueByKey("runtime_action")?.value as? YAMLScalar
            if (action != null) links[action] = Link.Action(action.textValue)
        }
    }
}
