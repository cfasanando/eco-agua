package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.Instant;

public record Matrix26ProcessInfo(
        long pid,
        Long parentPid,
        String executable,
        String commandLine,
        String user,
        Instant startedAt,
        boolean alive
) {
}
