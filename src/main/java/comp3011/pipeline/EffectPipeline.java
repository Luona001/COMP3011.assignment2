/*
 * COMP3011 Assignment 2 - frame record leaving the last pipeline stage.
 *
 * Authors:
 *   1. An Nhu Luong - a1953283
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.bytedeco.javacv.Frame;

import comp3011.FrameProcessor;
import comp3011.InfoFrame;
import comp3011.PlaybackClock;

/**
 * Splits the ordered effect chain across K stage threads.
 *
 * <p>Correctness argument: each FrameProcessor instance is confined to exactly one
 * stage thread (no shared mutable state, no locks inside effects), and frames flow
 * through the stages in FIFO order, so every processor observes frames in exactly
 * the same order as the original single-threaded player. Command-line effect
 * ordering and deterministic effect output are therefore preserved by construction.</p>
 *
 * <p>Queue capacity 2 provides backpressure: stages block on put/take so the decoder
 * cannot race arbitrarily far ahead of the display, bounding memory to a handful of
 * HD frames.</p>
 */
public final class EffectPipeline {
    private final List<Thread> stages = new ArrayList<>();

    public EffectPipeline(List<FrameProcessor> chain, PlaybackClock clock, AtomicLong epoch,
                          BlockingQueue<PipelineFrame> decoderOut,
                          BlockingQueue<FinishedFrame> displayOut) {
        int cores = Runtime.getRuntime().availableProcessors();
        int stageCount = Math.max(1, Math.min(chain.size(), cores - 2));

BlockingQueue<PipelineFrame> in = decoderOut;

for (int i = 0; i < stageCount; i++) {
    int from = chain.size() * i / stageCount;
    int to = chain.size() * (i + 1) / stageCount;

    List<FrameProcessor> mine = chain.subList(from, to);
    boolean last = (i == stageCount - 1);

    BlockingQueue<PipelineFrame> out = last ? null : new ArrayBlockingQueue<>(2);
    BlockingQueue<PipelineFrame> stageIn = in;
    BlockingQueue<PipelineFrame> stageOut = out;

    Thread t = new Thread(
            () -> runStage(
                    mine,
                    stageIn,
                    stageOut,
                    clock,
                    epoch,
                    displayOut
            ),
            "effect-stage-" + i
    );
    t.setDaemon(true);
    stages.add(t);
    in = out;
}

stages.forEach(Thread::start);

    }

    private void runStage(List<FrameProcessor> mine, BlockingQueue<PipelineFrame> in,
                          BlockingQueue<PipelineFrame> out, PlaybackClock clock,
                          AtomicLong epoch, BlockingQueue<FinishedFrame> displayOut) {
        try {
            while (true) {
                PipelineFrame item = in.take();
                if (item.epoch() != epoch.get()) {
                    continue;
                }
                Frame frame = item.frame();
                InfoFrame info = new InfoFrame(item.frameNumber(), item.mediaTimestampUs());
                for (FrameProcessor p : mine) {
                    p.process(frame, info);
                }
                if (out != null) {
                    out.put(item);
                } else {
                    displayOut.put(new FinishedFrame(frame, item.frameNumber(), item.mediaTimestampUs(),
                            clock.logicalTimestampUs(item.mediaTimestampUs()), item.targetTimeNs(),
                            System.nanoTime(), item.epoch()));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        stages.forEach(Thread::interrupt);
        for (Thread t : stages) {
            try { t.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}