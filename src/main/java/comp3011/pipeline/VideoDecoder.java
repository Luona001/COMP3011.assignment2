package comp3011.pipeline;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import comp3011.FrameCopy;
import comp3011.FrameProcessor;
import comp3011.InfoVideo;
import comp3011.PlaybackClock;

public final class VideoDecoder extends Thread {
    private final FFmpegFrameGrabber grabber;
    private final boolean audioAvailable;
    private final String mediaName;
    private final int totalVideoFrames;
    private final double frameRate;
    private final int intFrameRate;
    private final int frameDurationUs;
    private final PlaybackClock clock;
    private final AtomicLong epoch;
    private final BlockingQueue<DecodeCommand> commands =
            new ArrayBlockingQueue<>(16);
    private final BlockingQueue<PipelineFrame> output;
    private final List<FrameProcessor> processors;
    private final AudioSampleSink audioSink;
    private final Runnable onFinished;
    private volatile boolean paused;
    private volatile boolean running = true;
    private volatile Exception error;
    private boolean processorsInitialised;
    private boolean grabSingleFrame;

    @FunctionalInterface
    public interface AudioSampleSink {
        void accept(long timestampUs, Frame frame);
    }

    public VideoDecoder(
            FFmpegFrameGrabber grabber,
            boolean audioAvailable,
            String mediaName,
            int totalVideoFrames,
            double frameRate,
            int intFrameRate,
            int frameDurationUs,
            PlaybackClock clock,
            AtomicLong epoch,
            BlockingQueue<PipelineFrame> output,
            List<FrameProcessor> processors,
            AudioSampleSink audioSink,
            Runnable onFinished) {

        super("video-decoder");
        this.grabber = grabber;
        this.audioAvailable = audioAvailable;
        this.mediaName = mediaName;
        this.totalVideoFrames = totalVideoFrames;
        this.frameRate = frameRate;
        this.intFrameRate = intFrameRate;
        this.frameDurationUs = frameDurationUs;
        this.clock = clock;
        this.epoch = epoch;
        this.output = output;
        this.processors = processors;
        this.audioSink = audioSink;
        this.onFinished = onFinished;
        setDaemon(true);
    }
    
    public void requestSeek(
            long grabTimestampUs,
            long logicalTimestampUs) {
        SeekTo seek = new SeekTo(
                grabTimestampUs,
                logicalTimestampUs);

        if (!commands.offer(seek)) {
            commands.removeIf(command -> command instanceof SeekTo);
            commands.offer(seek);
        }
    }

    public void requestPause(boolean pause) {
        paused = pause;
        commands.offer(new SetPaused(pause));
    }

    public void requestStop() {
        running = false;
        commands.offer(new Stop());
        interrupt();
    }

    public Exception getDecoderError() {
        return error;
    }
    
    @Override
    public void run() {
        try {
            decodeLoop();
        } catch (InterruptedException e) {
            if (running) {
                error = e;
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            error = e;
            e.printStackTrace();
        } finally {
            closeGrabberOnThisThread();
        }
    }

    private void decodeLoop() throws Exception {
        while (running) {
            processCommands();

            if (!running) {
                break;
            }

            if (paused && !grabSingleFrame) {
                Thread.sleep(5);
                continue;
            }

            long frameEpoch = epoch.get();

            Frame raw = grabber.grab();

            if (raw == null) {
                if (running) {
                    onFinished.run();
                }
                return;
            }
            long timestampUs = grabber.getTimestamp();
            if (audioAvailable && audioSink != null) {
                audioSink.accept(timestampUs, raw);
            }
            if (raw.image == null) {
                continue;
            }
            if (!processorsInitialised) {
                initialiseProcessors(raw);
                processorsInitialised = true;
            }
            clock.latchFirstFrame(timestampUs, paused);
            Frame copiedFrame = FrameCopy.imageCopyOf(raw);
            int frameNumber = grabber.getFrameNumber();
            PipelineFrame decoded = new PipelineFrame(
                    copiedFrame,
                    frameNumber,
                    timestampUs,
                    clock.targetTimeNs(timestampUs),
                    frameEpoch);

            while (running
                    && !output.offer(decoded, 5, TimeUnit.MILLISECONDS)) {
                processCommands();
                if (paused) {
                    break;
                }
                if (frameEpoch != epoch.get()) {
                    grabSingleFrame = false;
                    break;
                }
            }
            grabSingleFrame = false;
        }
    }

    private void processCommands() throws Exception {
        DecodeCommand command;
        while ((command = commands.poll()) != null) {
        	if (command instanceof SeekTo seek) {
                grabber.setTimestamp(seek.grabTimestampUs());
                grabSingleFrame = true;
        	 } else if (command instanceof SetPaused pauseCommand) {
                 paused = pauseCommand.paused();
                 if (!paused) {
                     grabSingleFrame = false;
                 }
            } else if (command instanceof Stop) {
                running = false;
                return;
            }
        }
    }

    private void initialiseProcessors(Frame frame) throws Exception {
        InfoVideo info = new InfoVideo(
                mediaName,
                totalVideoFrames,
                frame.imageWidth,
                frame.imageHeight,
                frame.imageDepth,
                frame.imageChannels,
                frame.imageStride,
                frameRate,
                intFrameRate,
                frameDurationUs,
                grabber.getPixelFormat());
        for (FrameProcessor processor : processors) {
            processor.initialise(info);
        }
    }

    private void closeGrabberOnThisThread() {
        try {
            grabber.stop();
        } catch (Exception ignored) {
        }

        try {
            grabber.close();
        } catch (Exception ignored) {
        }
    }
}
