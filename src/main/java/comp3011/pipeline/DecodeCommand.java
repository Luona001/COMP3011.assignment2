package comp3011.pipeline;

/**
 * Commands sent to the decode thread.
 *
 * The JavaFX thread must not access FFmpegFrameGrabber directly.
 */
public sealed interface DecodeCommand
        permits SeekTo, SetPaused, Stop {
}
