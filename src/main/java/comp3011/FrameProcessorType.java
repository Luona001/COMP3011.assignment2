/*
 * COMP3011 Assignment 2 - effect types and their command-line flags.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.util.function.Supplier;

public enum FrameProcessorType {
	NUMBER       ('n', "--number-frames",    "Render the frame number onto each frame", FrameNumberer::new),
    SCRATCH      ('s', "--scratch-frames",   "Render vertical film scratches",          FrameScratcher::new),
    FLICKER      ('f', "--flicker-frames",   "Randomly dim frames",                     FrameFlickerer::new),
    BLACK_AND_WHITE('w', "--black-and-white", "Convert frames to black and white",      FrameBlackAndWhiter::new),
    YELLOW       ('y', "--yellow-frames",    "Apply a warmer colour temperature",       FrameYellower::new),
    VIGNETTE     ('v', "--vignette-frames",  "Darken the frame edges",                  FrameVignetter::new),
    DUST         ('d', "--dust-frames",      "Render dust and hair marks",              FrameDuster::new),
    JITTER       ('j', "--jitter-frames",    "Randomly displace frames by a few pixels",FrameJitterer::new),
    MOTTLE       ('m', "--mottle-frames",    "Add cloudy emulsion mottling",            FrameMottler::new),
    BLEED        ('b', "--bleed-frames",     "Bleed light into frames",                 FrameBleeder::new),
    PEPPER       ('p', "--pepper-frames",    "Pepper frames with dark spots/blotches",  FramePepperer::new);
    private final char shortFlag;
    private final String longFlag;
    private final String description;
    private final Supplier<FrameProcessor> factory;
    FrameProcessorType(char shortFlag, String longFlag, String description,
                       Supplier<FrameProcessor> factory) {
        this.shortFlag = shortFlag;
        this.longFlag = longFlag;
        this.description = description;
        this.factory = factory;
    }
    public FrameProcessor create() {
        return factory.get();
    }

    public char shortFlag() {
        return shortFlag;
    }

    public String longFlag() {
        return longFlag;
    }

    public String helpLine() {
        return String.format("  -%c, %-17s  %s", shortFlag, longFlag, description);
    }

    public static FrameProcessorType findByShortFlag(char c) {
        for (FrameProcessorType type : values()) {
            if (type.shortFlag == c) {
                return type;
            }
        }
        return null;
    }

    public static FrameProcessorType findByLongFlag(String flag) {
        for (FrameProcessorType type : values()) {
            if (type.longFlag.equals(flag)) {
                return type;
            }
        }
        return null;
    }
}