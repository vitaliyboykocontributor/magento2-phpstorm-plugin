/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.mcp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for module creation.
 */
public class ModuleCreationRequest {
    private String packageName;
    private String moduleName;
    private String moduleDescription;
    private String moduleVersion;
    private List<String> licenses = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private boolean createReadme = false;

    /**
     * Get package name.
     *
     * @return Package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Set package name.
     *
     * @param packageName Package name
     */
    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    /**
     * Get module name.
     *
     * @return Module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Set module name.
     *
     * @param moduleName Module name
     */
    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Get module description.
     *
     * @return Module description
     */
    public String getModuleDescription() {
        return moduleDescription;
    }

    /**
     * Set module description.
     *
     * @param moduleDescription Module description
     */
    public void setModuleDescription(final String moduleDescription) {
        this.moduleDescription = moduleDescription;
    }

    /**
     * Get module version.
     *
     * @return Module version
     */
    public String getModuleVersion() {
        return moduleVersion;
    }

    /**
     * Set module version.
     *
     * @param moduleVersion Module version
     */
    public void setModuleVersion(final String moduleVersion) {
        this.moduleVersion = moduleVersion;
    }

    /**
     * Get licenses.
     *
     * @return Licenses
     */
    public List<String> getLicenses() {
        return licenses;
    }

    /**
     * Set licenses.
     *
     * @param licenses Licenses
     */
    public void setLicenses(final List<String> licenses) {
        this.licenses = licenses != null ? licenses : new ArrayList<>();
    }

    /**
     * Get dependencies.
     *
     * @return Dependencies
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * Set dependencies.
     *
     * @param dependencies Dependencies
     */
    public void setDependencies(final List<String> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }

    /**
     * Check if README.md should be created.
     *
     * @return True if README.md should be created, false otherwise
     */
    public boolean isCreateReadme() {
        return createReadme;
    }

    /**
     * Set if README.md should be created.
     *
     * @param createReadme True if README.md should be created, false otherwise
     */
    public void setCreateReadme(final boolean createReadme) {
        this.createReadme = createReadme;
    }
}