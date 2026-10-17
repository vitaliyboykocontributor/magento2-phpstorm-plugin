/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager

/** App Builder integration is independent of the Magento PHP integration. */
@State(name = "AdobeAppBuilderSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class AppBuilderSettings(private val project: Project? = null) : PersistentStateComponent<AppBuilderSettings.State> {
    data class State(var enabled: Boolean = false, var directory: String = "")

    private var configuration = State()

    override fun getState(): State = configuration.copy()

    override fun loadState(state: State) {
        val updated = state.copy(directory = normalizeDirectory(state.directory))
        if (configuration == updated) return
        configuration = updated
        // YAML and JavaScript cache contributed references independently of this service.
        if (project != null && project.isInitialized && !project.isDisposed) {
            val application = ApplicationManager.getApplication()
            val invalidate = Runnable { if (!project.isDisposed) PsiManager.getInstance(project).dropPsiCaches() }
            if (application.isDispatchThread || application.isWriteAccessAllowed) invalidate.run()
            else application.invokeLater(invalidate)
        }
    }

    fun getRootDirectory(): VirtualFile? = configuration.takeIf { it.enabled }
        ?.directory?.takeIf { it.isNotEmpty() }
        ?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        ?.takeIf { it.isDirectory }

    fun isEnabledFor(file: VirtualFile): Boolean = getRootDirectory()
        ?.let { VfsUtilCore.isAncestor(it, file, false) } == true

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AppBuilderSettings = project.getService(AppBuilderSettings::class.java)

        internal fun normalizeDirectory(value: String): String =
            FileUtil.toCanonicalPath(FileUtil.toSystemIndependentName(value.trim()))
    }
}
