package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

class FrameCopyTest {

    @Test
    void copiedPixelsRemainUnchangedWhenSourceChanges() {
        Frame source = new Frame(4, 4, Frame.DEPTH_UBYTE, 4);

        try {
            assertNotNull(source.image);
            assertNotNull(source.image[0]);

            Buffer sourceBuffer = source.image[0];
            assertEquals(true, sourceBuffer instanceof ByteBuffer);

            ByteBuffer sourcePixels = (ByteBuffer) sourceBuffer;

            sourcePixels.put(0, (byte) 42);

            Frame copy = FrameCopy.imageCopyOf(source);

            try {
                assertNotNull(copy.image);
                assertNotNull(copy.image[0]);

                assertNotSame(source.image[0], copy.image[0]);

                ByteBuffer copiedPixels = (ByteBuffer) copy.image[0];

                sourcePixels.put(0, (byte) 99);
                assertEquals(42, copiedPixels.get(0) & 0xFF);
                assertEquals(99, sourcePixels.get(0) & 0xFF);
            } finally {
                copy.close();
            }
        } finally {
            source.close();
        }
    }
}
