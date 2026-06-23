package com.ecoamazonas.eco_agua.platform.module;

import java.util.List;

public interface PlatformModuleInstaller {

    String moduleKey();

    String displayName();

    String currentVersion();

    boolean isInstalled();

    boolean isEnabled();

    List<PlatformModuleInstallStep> installationSteps(boolean demoData);

    void setEnabled(boolean enabled);
}
