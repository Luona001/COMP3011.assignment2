/*
 * COMP3011 Assignment 2 - stop command for the decode thread.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011.pipeline;

/**
 * Request a pause or resume on the decode thread.
 */
public record SetPaused(boolean paused) implements DecodeCommand {
}
