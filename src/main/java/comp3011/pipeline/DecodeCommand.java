/*
 * COMP3011 Assignment 2 - multi-threaded effect pipeline stage split.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011.pipeline;

/**
 * Commands sent to the decode thread.
 *
 * The JavaFX thread must not access FFmpegFrameGrabber directly.
 */
public sealed interface DecodeCommand
        permits SeekTo, SetPaused, Stop {
}
