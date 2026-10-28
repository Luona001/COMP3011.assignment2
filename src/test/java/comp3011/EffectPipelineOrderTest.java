package comp3011;

import comp3011.pipeline.EffectPipeline;
import comp3011.pipeline.FinishedFrame;
import comp3011.pipeline.PipelineFrame;
import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class EffectPipelineOrderTest {

	private static final class Tag extends FrameProcessor {
        private final StringBuilder sb;
        private final String tag;

        Tag(StringBuilder sb, String tag) {
            this.sb = sb;
            this.tag = tag;
        }

        @Override
        public void initialise(InfoVideo videoInfo) {
        }

        @Override
        public void process(Frame frame, InfoFrame info) {
            sb.append(tag);
        }
    }

    @Test
    void framesKeepOrderAndProcessorsRunInChainOrder() throws Exception {
        StringBuilder sb = new StringBuilder();
        List<FrameProcessor> chain = List.of(
                new Tag(sb, "A"), new Tag(sb, "B"), new Tag(sb, "C"));
        PlaybackClock clock = new PlaybackClock();
        AtomicLong epoch = new AtomicLong(0);
        BlockingQueue<PipelineFrame> decoderOut = new ArrayBlockingQueue<>(2);
        BlockingQueue<FinishedFrame> displayOut = new LinkedBlockingQueue<>();

        EffectPipeline pipeline = new EffectPipeline(chain, clock, epoch, decoderOut, displayOut);
        try {
            int frames = 30;
            for (int i = 0; i < frames; i++) {
                decoderOut.put(new PipelineFrame(new Frame(), i, i * 33_333L,
                        System.nanoTime() + 10_000_000_000L, epoch.get()));
            }
            List<Integer> seen = new ArrayList<>();
            for (int i = 0; i < frames; i++) {
                FinishedFrame f = displayOut.poll(5, TimeUnit.SECONDS);
                assertNotNull(f);
                seen.add(f.frameNumber());
            }
            assertEquals(frames, sb.length() / 3, "every frame hit all three processors");
            assertEquals(List.of(0), seen.subList(0, 1));
            assertEquals(frames - 1, seen.get(frames - 1).intValue());
            for (int i = 1; i < frames; i++) {
                assertEquals(seen.get(i - 1) + 1, seen.get(i), "frames arrived FIFO");
            }
        } finally {
            pipeline.shutdown();
        }
    }

    @Test
    void framesFromOldEpochAreDroppedAfterSeek() throws Exception {
        StringBuilder sb = new StringBuilder();
        List<FrameProcessor> chain = List.of(new Tag(sb, "X"));
        PlaybackClock clock = new PlaybackClock();
        AtomicLong epoch = new AtomicLong(0);
        BlockingQueue<PipelineFrame> decoderOut = new ArrayBlockingQueue<>(4);
        BlockingQueue<FinishedFrame> displayOut = new LinkedBlockingQueue<>();

        EffectPipeline pipeline = new EffectPipeline(chain, clock, epoch, decoderOut, displayOut);
        try {
            decoderOut.put(new PipelineFrame(new Frame(), 0, 0,
                    System.nanoTime(), epoch.get()));
            FinishedFrame f = displayOut.poll(5, TimeUnit.SECONDS);
            assertNotNull(f);
            assertEquals(0, f.epoch());

            epoch.incrementAndGet();
            decoderOut.put(new PipelineFrame(new Frame(), 1, 33_333,
                    System.nanoTime(), epoch.get()));
            FinishedFrame f2 = displayOut.poll(5, TimeUnit.SECONDS);
            assertNotNull(f2);
            assertEquals(1, f2.epoch());

            decoderOut.put(new PipelineFrame(new Frame(), 99, 0,
                    System.nanoTime(), 0));
            assertNull(displayOut.poll(500, TimeUnit.MILLISECONDS));
            assertEquals(2, sb.length(), "only two frames were processed");
        } finally {
            pipeline.shutdown();
        }
    }
}