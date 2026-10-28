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
