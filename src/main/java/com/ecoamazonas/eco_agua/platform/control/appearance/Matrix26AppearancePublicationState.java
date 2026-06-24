package com.ecoamazonas.eco_agua.platform.control.appearance;

public record Matrix26AppearancePublicationState(
        boolean allowed,
        boolean targetConfigurationPresent,
        Integer targetPublishedVersion,
        boolean synchronizedWithMatrix26,
        String message
) {
}
