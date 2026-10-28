package comp3011.pipeline;

/**
 * Request the decode thread to seek to a new position.
 */
public record SeekTo(
        long grabTimestampUs,
        long logicalTimestampUs) implements DecodeCommand {
}
