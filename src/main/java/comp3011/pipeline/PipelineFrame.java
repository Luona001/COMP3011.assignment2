
package comp3011.pipeline;

import org.bytedeco.javacv.Frame;

public record PipelineFrame(
        Frame frame,
        int frameNumber,
        long mediaTimestampUs,
        long targetTimeNs,
        long epoch) {
}
