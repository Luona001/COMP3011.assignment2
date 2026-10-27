
package comp3011;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineControllerTest {

    @Test
    void helpTextMatchesSpecification() {
        String expected = """
                Usage: VideoPlayer [options] [video-file]

                Options:
                  -h, --help         Show this help message
                  -a, --audio        Play audio
                  -x, --maximise     Open the player maximised
                  -1, --monitor-1    Open the player on display 1
                  -2, --monitor-2    Open the player on display 2

                Frame processors:
                  -n, --number-frames    Render the frame number onto each frame
                  -s, --scratch-frames   Render vertical film scratches
                  -f, --flicker-frames   Randomly dim frames
                  -w, --black-and-white  Convert frames to black and white
                  -y, --yellow-frames    Apply a warmer colour temperature
                  -v, --vignette-frames  Darken the frame edges
                  -d, --dust-frames      Render dust and hair marks
                  -j, --jitter-frames    Randomly displace frames by a few pixels
                  -m, --mottle-frames    Add cloudy emulsion mottling
                  -b, --bleed-frames     Bleed light into frames
                  -p, --pepper-frames    Pepper frames with dark spots/blotches

                Frame processors are applied in command-line order and may be repeated.
                Example: -nssnfwyvdjmbp numbers, scratches twice, numbers again, flickers,
                converts, warms, vignettes, dusts, jitters, mottles, bleeds, then peppers.
                """;

        assertEquals(expected, CommandLineController.buildHelp());
    }

    @Test
    void stackedShortOptionsPreserveOrderAndRepetition(
            @TempDir Path tempDir) throws IOException {

        Path movie = Files.createFile(tempDir.resolve("movie.mp4"));

        CommandLineController c = new CommandLineController(
                new String[]{"-nssnfwyvdjmbp", movie.toString()});

        assertEquals(List.of(
                FrameProcessorType.NUMBER,
                FrameProcessorType.SCRATCH,
                FrameProcessorType.SCRATCH,
                FrameProcessorType.NUMBER,
                FrameProcessorType.FLICKER,
                FrameProcessorType.BLACK_AND_WHITE,
                FrameProcessorType.YELLOW,
                FrameProcessorType.VIGNETTE,
                FrameProcessorType.DUST,
                FrameProcessorType.JITTER,
                FrameProcessorType.MOTTLE,
                FrameProcessorType.BLEED,
                FrameProcessorType.PEPPER
        ), c.getFrameProcessorTypes());
    }

    @Test
    void mixedLongAndShortFormsAreEquivalent(
            @TempDir Path tempDir) throws IOException {

        Path movie = Files.createFile(tempDir.resolve("movie.mp4"));

        String[] shortArgs = {
                "-n", "-s", "-f", "-w", "-y", "-v",
                "-d", "-j", "-m", "-b", "-p",
                movie.toString()
        };

        String[] longArgs = {
                "--number-frames",
                "--scratch-frames",
                "--flicker-frames",
                "--black-and-white",
                "--yellow-frames",
                "--vignette-frames",
                "--dust-frames",
                "--jitter-frames",
                "--mottle-frames",
                "--bleed-frames",
                "--pepper-frames",
                movie.toString()
        };

        CommandLineController shortController =
                new CommandLineController(shortArgs);
        CommandLineController longController =
                new CommandLineController(longArgs);

        assertNull(shortController.getErrorMessage());
        assertNull(longController.getErrorMessage());

        assertEquals(
                shortController.getFrameProcessorTypes(),
                longController.getFrameProcessorTypes()
        );
    }

    @Test
    void everyTypeCreatesCorrectClassAndDistinctInstances() {
        for (FrameProcessorType type : FrameProcessorType.values()) {
            FrameProcessor first = type.create();
            FrameProcessor second = type.create();

            assertNotNull(first);
            assertNotNull(second);
            assertNotSame(first, second);
        }
    }

    @Test
    void errorsUnknownOptionMissingFileTwoFiles(
            @TempDir Path tempDir) throws IOException {

        Path first = Files.createFile(tempDir.resolve("first.mp4"));
        Path second = Files.createFile(tempDir.resolve("second.mp4"));

        CommandLineController unknown = new CommandLineController(
                new String[]{"--illegal-argument", first.toString()});

        assertTrue(unknown.getErrorMessage().contains("Unknown option"));
        assertEquals(1, unknown.getExitCode());

        CommandLineController missing =
                new CommandLineController(new String[0]);

        assertEquals(
                "No video file specified.",
                missing.getErrorMessage()
        );
        assertEquals(1, missing.getExitCode());

        CommandLineController twoFiles = new CommandLineController(
                new String[]{first.toString(), second.toString()});

        assertEquals(
                "Usage: VideoPlayer [options] [video-file]",
                twoFiles.getErrorMessage()
        );
        assertEquals(1, twoFiles.getExitCode());
    }

    @Test
    void audioMaximiseAndMonitorOptionsAreParsed(
            @TempDir Path tempDir) throws IOException {

        Path movie = Files.createFile(tempDir.resolve("movie.mp4"));

        CommandLineController c = new CommandLineController(
                new String[]{
                        "--audio",
                        "--maximise",
                        "--monitor-2",
                        movie.toString()
                });

        assertNull(c.getErrorMessage());
        assertTrue(c.isAudioRequested());
        assertTrue(c.isMaximiseRequested());
        assertEquals(2, c.getDisplayId());
    }

    @Test
    void conflictingMonitorOptionsAreRejected(
            @TempDir Path tempDir) throws IOException {

        Path movie = Files.createFile(tempDir.resolve("movie.mp4"));

        CommandLineController c = new CommandLineController(
                new String[]{
                        "--monitor-1",
                        "--monitor-2",
                        movie.toString()
                });

        assertNotNull(c.getErrorMessage());
        assertTrue(c.getErrorMessage().contains(
                "Only one display option can be used"));
        assertEquals(1, c.getExitCode());
    }
}
