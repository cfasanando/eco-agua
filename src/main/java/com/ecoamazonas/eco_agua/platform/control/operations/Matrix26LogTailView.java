package com.ecoamazonas.eco_agua.platform.control.operations;

import java.util.List;

public record Matrix26LogTailView(
        Matrix26LogInventoryItem log,
        List<String> lines,
        boolean available,
        String message
) {
}
