package comp3011.pipeline;

import org.bytedeco.javacv.Frame;

/**
 * A decoded frame waiting to be processed by the pipeline.
 *
 * The epoch identifies the playback generation. Frames from an old
 * generation must be discarded after a seek.
 */
public record PipelineFrame(
        Frame frame,
        int frameNumber,
        long mediaTimestampUs,
        long targetTimeNs,
        long epoch) {
}
