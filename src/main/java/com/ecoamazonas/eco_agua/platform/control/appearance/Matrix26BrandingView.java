package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

import java.util.List;

public record Matrix26BrandingView(
        PlatformBusinessClient instance,
        Matrix26BrandingForm form,
        List<Matrix26BrandingAssetView> assets,
        boolean draftPresent,
        String updatedBy,
        String updatedAt,
        boolean demoKitAvailable
) {
}
