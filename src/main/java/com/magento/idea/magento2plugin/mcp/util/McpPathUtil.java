/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.magento.idea.magento2plugin.magento.packages.Package;
import com.magento.idea.magento2plugin.project.Settings;
import com.magento.idea.magento2plugin.util.magento.MagentoBasePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for MCP server paths.
 */
public final class McpPathUtil {
    private static final Logger LOGGER = Logger.getInstance(McpPathUtil.class);

    private McpPathUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Find the base directory for a vendor.
     *
     * @param project Project
     * @param vendorName Vendor name
     * @return Base directory for the vendor or null if not found
     */
    @Nullable
    public static PsiDirectory findBaseDirectoryByVendor(
            final @NotNull Project project,
            final @NotNull String vendorName
    ) {
        final String magentoPath = Settings.getMagentoPath(project);
        if (magentoPath == null) {
            LOGGER.warn("Magento path not found in project settings");
            return null;
        }

        // Check if Magento path is valid
        if (!MagentoBasePathUtil.isMagentoFolderValid(magentoPath)) {
            LOGGER.warn("Invalid Magento path: " + magentoPath);
            return null;
        }

        // Find app/code directory
        final VirtualFile magentoRoot = LocalFileSystem.getInstance().findFileByPath(magentoPath);
        if (magentoRoot == null) {
            LOGGER.warn("Magento root directory not found: " + magentoPath);
            return null;
        }

        final VirtualFile appDir = magentoRoot.findChild("app");
        if (appDir == null || !appDir.isDirectory()) {
            LOGGER.warn("app directory not found in Magento root");
            return null;
        }

        final VirtualFile codeDir = appDir.findChild("code");
        if (codeDir == null) {
            LOGGER.warn("code directory not found in app directory");
            return null;
        }

        // Find or create vendor directory
        VirtualFile vendorDir = codeDir.findChild(vendorName);
        if (vendorDir == null) {
            LOGGER.info("Vendor directory not found, will be created: " + vendorName);
            // Return the code directory, the vendor directory will be created by the generator
            return PsiManager.getInstance(project).findDirectory(codeDir);
        }

        return PsiManager.getInstance(project).findDirectory(vendorDir);
    }

    /**
     * Get the path to the app/code directory.
     *
     * @param project Project
     * @return Path to the app/code directory or null if not found
     */
    @Nullable
    public static String getAppCodePath(final @NotNull Project project) {
        final String magentoPath = Settings.getMagentoPath(project);
        if (magentoPath == null) {
            LOGGER.warn("Magento path not found in project settings");
            return null;
        }

        return magentoPath + "/app/code";
    }
}