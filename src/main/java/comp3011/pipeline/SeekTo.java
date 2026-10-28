/*
 * COMP3011 Assignment 2 - pause/resume command for the decode thread.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011.pipeline;

/**
 * Request the decode thread to seek to a new position.
 */
public record SeekTo(
        long grabTimestampUs,
        long logicalTimestampUs) implements DecodeCommand {
}
