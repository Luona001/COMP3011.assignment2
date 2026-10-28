package comp3011;

public final class PlaybackClock {
    private static final long NO_TIMESTAMP = -1;

    private long firstTimestampUs = NO_TIMESTAMP;
    private long logicalBaseUs;
    private long startNs;
    private long pauseStartedNs;
    private long positionUs;

    synchronized void reset(long logicalTimestampUs) {
        firstTimestampUs = NO_TIMESTAMP;
        logicalBaseUs = logicalTimestampUs;
        positionUs = logicalTimestampUs;
        startNs = 0;
        pauseStartedNs = 0;
    }

    public synchronized void latchFirstFrame(long mediaTimestampUs, boolean paused) {
        if (firstTimestampUs != NO_TIMESTAMP) {
            return;
        }
        firstTimestampUs = mediaTimestampUs;
        startNs = System.nanoTime();
        if (paused) {
            pauseStartedNs = startNs;
        }
    }

    synchronized void pause() {
        if (pauseStartedNs == 0) {
            pauseStartedNs = System.nanoTime();
        }
    }

    synchronized void resume(long nowNs) {
        if (pauseStartedNs > 0 && startNs > 0) {
            startNs += nowNs - pauseStartedNs;
        }
        pauseStartedNs = 0;
    }

    public synchronized long targetTimeNs(long mediaTimestampUs) {
        if (firstTimestampUs == NO_TIMESTAMP || startNs <= 0) {
            return 0;
        }
        long relativeUs = Math.max(0, mediaTimestampUs - firstTimestampUs);
        return startNs + relativeUs * 1_000L;
    }

    synchronized long logicalTimestampUs(long mediaTimestampUs) {
        if (firstTimestampUs == NO_TIMESTAMP) {
            return logicalBaseUs;
        }
        return logicalBaseUs + Math.max(0, mediaTimestampUs - firstTimestampUs);
    }

    synchronized long dueAudioTimestampUs(long nowNs, long audioLeadNs) {
        if (firstTimestampUs == NO_TIMESTAMP || startNs <= 0) {
            return Long.MIN_VALUE;
        }
        return firstTimestampUs + (nowNs + audioLeadNs - startNs) / 1_000L;
    }

    synchronized long firstTimestampUs() { return firstTimestampUs; }
    synchronized long positionUs() { return positionUs; }
    synchronized void setPositionUs(long us) { positionUs = us; }
    synchronized boolean latched() { return firstTimestampUs != NO_TIMESTAMP && startNs > 0; }
}