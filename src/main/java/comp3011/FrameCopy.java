/*
 * COMP3011 Assignment 2 - deep copy of JavaCV frame image planes.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.nio.Buffer;

import org.bytedeco.javacv.Frame;

public final class FrameCopy {

    private FrameCopy() {
    }

    public static Frame imageCopyOf(Frame src) {
        Frame dst = new Frame(
                src.imageWidth,
                src.imageHeight,
                src.imageDepth,
                src.imageChannels);

        for (int i = 0; i < src.image.length; i++) {
            Buffer from = src.image[i];
            Buffer to = dst.image[i];

            Buffer source = copyBufferView(from);

            source.clear();
            to.clear();

            if (source instanceof java.nio.ByteBuffer sourceBytes
                    && to instanceof java.nio.ByteBuffer targetBytes) {
                targetBytes.put(sourceBytes);
            } else if (source instanceof java.nio.ShortBuffer sourceShorts
                    && to instanceof java.nio.ShortBuffer targetShorts) {
                targetShorts.put(sourceShorts);
            } else if (source instanceof java.nio.IntBuffer sourceInts
                    && to instanceof java.nio.IntBuffer targetInts) {
                targetInts.put(sourceInts);
            } else if (source instanceof java.nio.FloatBuffer sourceFloats
                    && to instanceof java.nio.FloatBuffer targetFloats) {
                targetFloats.put(sourceFloats);
            } else if (source instanceof java.nio.LongBuffer sourceLongs
                    && to instanceof java.nio.LongBuffer targetLongs) {
                targetLongs.put(sourceLongs);
            } else if (source instanceof java.nio.DoubleBuffer sourceDoubles
                    && to instanceof java.nio.DoubleBuffer targetDoubles) {
                targetDoubles.put(sourceDoubles);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported image buffer type: "
                                + source.getClass().getName());
            }

            to.flip();
        }

        return dst;
    }

    private static Buffer copyBufferView(Buffer buffer) {
        if (buffer instanceof java.nio.ByteBuffer b) return b.duplicate();
        if (buffer instanceof java.nio.ShortBuffer b) return b.duplicate();
        if (buffer instanceof java.nio.IntBuffer b) return b.duplicate();
        if (buffer instanceof java.nio.FloatBuffer b) return b.duplicate();
        if (buffer instanceof java.nio.LongBuffer b) return b.duplicate();
        if (buffer instanceof java.nio.DoubleBuffer b) return b.duplicate();

        throw new IllegalArgumentException(
                "Unsupported image buffer type: " + buffer.getClass().getName());
    }
}
