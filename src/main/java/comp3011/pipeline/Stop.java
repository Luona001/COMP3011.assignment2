/*
 * COMP3011 Assignment 2 - dedicated thread that owns FFmpegFrameGrabber.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011.pipeline;

/**
 * Request the decode thread to stop.
 */
public record Stop() implements DecodeCommand {
}
