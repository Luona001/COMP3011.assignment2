package comp3011.pipeline;

/**
 * Request a pause or resume on the decode thread.
 */
public record SetPaused(boolean paused) implements DecodeCommand {
}
