package dev.monsoonstory.billiards.core;

/** Immutable, server-consumed result; independent of translated status messages. */
public record ShotResult(int shooter, int targetPots, boolean valid, boolean clearedGroup,
                         boolean legalBlackWin) {}
