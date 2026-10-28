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

class PipelineThroughputTest {

	private static final class Heavy extends FrameProcessor {
        @Override
        public void initialise(InfoVideo videoInfo) {
        }

        @Override
        public void process(Frame frame, InfoFrame info) {
            double acc = 0;
            for (int i = 0; i < 60_000; i++) { acc += Math.sqrt(i + acc) % 7; }
            if (acc == -1) System.out.print("");
        }
    }

    @Test
    void parallelStagesBeatSerialProcessing() throws Exception {
        int frameCount = 40;
        List<FrameProcessor> chain = new ArrayList<>();
        for (int i = 0; i < 12; i++) { chain.add(new Heavy()); }

        // serial baseline (what the single-threaded player did on one core)
        Frame dummy = new Frame();
        InfoFrame info = new InfoFrame(0, 0);
        long serialStart = System.nanoTime();
        for (int f = 0; f < frameCount; f++) {
            for (FrameProcessor p : chain) { p.process(dummy, info); }
        }
        long serialNs = System.nanoTime() - serialStart;

        // pipelined across stage threads
        PlaybackClock clock = new PlaybackClock();
        AtomicLong epoch = new AtomicLong(0);
        BlockingQueue<PipelineFrame> decoderOut = new ArrayBlockingQueue<>(2);
        BlockingQueue<FinishedFrame> displayOut = new LinkedBlockingQueue<>();
        EffectPipeline pipeline = new EffectPipeline(chain, clock, epoch, decoderOut, displayOut);
        long parallelNs;
        try {
            long start = System.nanoTime();
            for (int f = 0; f < frameCount; f++) {
                decoderOut.put(new PipelineFrame(dummy, f, f * 33_333L,
                        System.nanoTime(), epoch.get()));
            }
            for (int f = 0; f < frameCount; f++) {
                assertNotNull(displayOut.poll(30, TimeUnit.SECONDS));
            }
            parallelNs = System.nanoTime() - start;
        } finally {
            pipeline.shutdown();
        }

        double speedup = (double) serialNs / parallelNs;
        System.out.printf("serial=%.1fms parallel=%.1fms speedup=%.2fx%n",
                serialNs / 1e6, parallelNs / 1e6, speedup);
        assertTrue(speedup > 1.5, "expected >1.5x from parallel stages, got " + speedup);
    }
}