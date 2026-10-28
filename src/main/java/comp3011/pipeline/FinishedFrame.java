/*
 * COMP3011 Assignment 2 - frame record flowing between pipeline stages.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011.pipeline;

import org.bytedeco.javacv.Frame;

/**
 * A frame that has completed processing and is ready for display.
 */
public record FinishedFrame(
        Frame frame,
        int frameNumber,
        long mediaTimestampUs,
        long logicalTimestampUs,
        long targetTimeNs,
        long preparedAtNs,
        long epoch) {
}
