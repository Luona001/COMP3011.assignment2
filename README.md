# COMP3011 Assignment 2 - Multi-threaded Video Player

A JavaFX video player originally supplied as single-threaded starter code,
refactored into a concurrent architecture that keeps GUI responsiveness while
applying heavy pixel-level effects in parallel.

## 1. Architecture / Thread structure

    ┌──────────────────────────┐   ┌──────────────────────────┐   ┌──────────────────────────────────┐
    │  JavaFX Application      │   │  video-decoder thread    │   │  effect-stage-0..K-1 threads     │
    │  Thread (GUI + clock)    │   │                          │   │                                  │
    │                          │   │  owns FFmpegFrameGrabber │   │  chain split into contiguous     │
    │  AnimationTimer          │   │  exclusively             │   │  chunks; each FrameProcessor     │
    │   ├ writeDueAudio()      │   │   ├ decode loop          │   │  instance confined to one stage  │
    │   ├ poll displayQueue    │◄──┤   ├ FrameCopy before     │──►│   ├ stage 0 ──► stage 1 ──► ...  │
    │   └ displayHeldFrame()   │   │   │   enqueue            │   │   └ ──► FinishedFrame            │
    │       (convert + show)   │   │   ├ command queue:       │   │                                  │
    │                          │   │   │   SeekTo/SetPaused/  │   │  FIFO order preserved            │
    │  AudioPlayer write on    │   │   │   Stop               │   │  epoch drop on every stage       │
    │  this thread             │   │   └ close grabber in     │   │                                  │
    └──────────────────────────┘   │       finally (own thread)│   └──────────────────────────────────┘
                                   └──────────────────────────┘
              decoderQueue (cap 2, backpressure) ──────►      displayQueue (cap 2) ──────► FX polls

Threads: 1 GUI + 1 decoder + K stage threads, where
K = max(1, min(chain length, available cores - 2)).

## 2. Critical regions and how they are protected

| Region | Owner / scope | Protection |
|---|---|---|
| `FFmpegFrameGrabber` | `video-decoder` thread only | Thread confinement: created on FX thread but every `grab`/`setTimestamp` happens on the decoder thread; `stop()`/`close()` run in the decoder's `finally` |
| `FrameProcessor` instances | Decoder (initialise once) + one stage each (process) | Thread confinement; hand-off via `BlockingQueue` gives happens-before for the one-time `initialise` |
| Frame pixel buffers | Decoder | `FrameCopy.imageCopyOf` deep copy before enqueue, because the grabber reuses its buffers |
| `PlaybackClock` | All threads | `synchronized` methods |
| `pendingAudio` | FX (consume) + decoder (produce) | `ConcurrentLinkedQueue` |
| Seek / restart invalidation | Shared | `AtomicLong epoch`; every stage and the FX poll drop frames whose epoch is stale |
| Command channel FX → decoder | Shared | Bounded `ArrayBlockingQueue`; seek commands coalesced (`removeIf`) |

## 3. Why the design is correct

- **Order preserved:** stage queues are FIFO and the chain is split into
  contiguous `subList` chunks, so processors observe frames in exactly the
  original command-line order (repetition included).
- **No shared mutable effect state:** each `FrameProcessor` object is touched
  by exactly one stage thread, so effects need no locks and produce bit-identical
  output to the single-threaded player for the same frame.
- **Bounded memory:** every queue has capacity 2, so at most a handful of HD
  frames are in flight between decoder, stages and display.
- **Seek while paused:** the decoder grabs exactly one frame after a `SeekTo`
  so the display updates immediately, then returns to sleeping.
- **Clean shutdown:** stages are interrupted first, then the decoder is stopped
  and joined; the grabber is always closed on the decoder thread.

## 4. Running

    mvn javafx:run -Djavafx.args="-nsnfwyvdjmbp -a movie.mp4"
    mvn test            # unit tests (CLI, frame copy, pipeline order/epoch)
    mvn verify          # + throughput test

## 5. Performance (Part 2)

Performance is measured with the course-supplied TITAN harness.

Design points that produce the speed-up: effects are pixel-loop heavy and now
run on K stage threads in parallel with decoding and display; the FX thread
only converts and blits ready frames, so the GUI never waits on effects.# COMP3011 Assignment 2 - Java Local Project

For more information on this code and how to get started, see https://learn.adelaide.edu.au/courses/30747/assignments/94505
