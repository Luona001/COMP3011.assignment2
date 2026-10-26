/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. <student name and student number insert here upon modification>
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created in the VideoPlayerApp's main function to receive and parse the video player's command-line arguments.
 *
 * <p>
 * Extracts the video file and supported launch options, reports invalid input
 * or help text, and exposes the resulting launch configuration via methods to the
 * {@link VideoPlayerApp}.
 * </p>
 */
public class CommandLineController {
    private final String[] args;

    private boolean helpRequested;
    private boolean audioRequested;
    private boolean maximiseRequested;
    private Integer displayId;
    private File videoFile;
    private String errorMessage;
    private final List<FrameProcessorType> frameProcessors = new ArrayList<>();

    public CommandLineController(String[] args) {
        this.args = args.clone();
        parse();
        if (errorMessage != null) {
            System.out.println(errorMessage);
        }
        if (helpRequested) {
            printHelp();
        }
    }

    public File getVideoFile() {
        return videoFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getExitCode() {
        return errorMessage == null ? 0 : 1;
    }

    public Integer getDisplayId() {
        return displayId;
    }

    public boolean isAudioRequested() {
        return audioRequested;
    }

    public boolean isMaximiseRequested() {
        return maximiseRequested;
    }

    public boolean shouldLaunchApplication() {
        return errorMessage == null && videoFile != null;
    }
    
    public List<FrameProcessorType> getFrameProcessorTypes() {
        return List.copyOf(frameProcessors);
    }

    private void parse() {
        List<String> videoFiles = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (!parseLongOption(arg)) {
                    return;
                }
            } else if (arg.startsWith("-") && arg.length() > 2) {
                if (!parseStackedShortOptions(arg)) {
                    return;
                }
            } else if (arg.startsWith("-")) {
                if (!parseSingleShortOption(arg)) {
                    return;
                }
            } else {
                videoFiles.add(arg);
            }
        }
        validateVideoFiles(videoFiles);
    }

    private boolean parseLongOption(String arg) {
        switch (arg) {
            case "--help":      helpRequested = true;     return true;
            case "--audio":     audioRequested = true;    return true;
            case "--maximise":  maximiseRequested = true; return true;
            case "--monitor-1": setDisplayId(1);          return true;
            case "--monitor-2": setDisplayId(2);          return true;
            default:
                FrameProcessorType type = FrameProcessorType.findByLongFlag(arg);
                if (type == null) {
                    errorMessage = "Unknown option: " + arg;
                    return false;
                }
                frameProcessors.add(type);
                return true;
        }
    }

    private boolean parseSingleShortOption(String arg) {
        char c = arg.charAt(1);
        switch (c) {
            case 'h': helpRequested = true;      return true;
            case 'a': audioRequested = true;     return true;
            case 'x': maximiseRequested = true;  return true;
            case '1': setDisplayId(1);           return true;
            case '2': setDisplayId(2);           return true;
            default:
                FrameProcessorType type = FrameProcessorType.findByShortFlag(c);
                if (type == null) {
                    errorMessage = "Unknown option: " + arg;
                    return false;
                }
                frameProcessors.add(type);
                return true;
        }
    }

    private boolean parseStackedShortOptions(String arg) {
        for (int i = 1; i < arg.length(); i++) {
            char c = arg.charAt(i);
            FrameProcessorType type = FrameProcessorType.findByShortFlag(c);
            if (type == null) {
                errorMessage = "Unknown option: -" + c;
                return false;
            }
            frameProcessors.add(type);
        }
        return true;
    }

    private void validateVideoFiles(List<String> videoFiles) {
        if (videoFiles.size() > 1) {
            errorMessage = "Usage: VideoPlayer [options] [video-file]";
        } else if (videoFiles.size() == 1) {
            videoFile = new File(videoFiles.get(0));
            if (!videoFile.isFile()) {
                errorMessage = "File not found: " + videoFile.getPath();
                videoFile = null;
            }
        } else {
            if (!helpRequested) {
                errorMessage = "No video file specified.";
            }
        }
    }

    private void setDisplayId(int displayId) {
        if (this.displayId != null && this.displayId != displayId) {
            errorMessage = "Only one display option can be used";
            return;
        }
        this.displayId = displayId;
    }

    private void printHelp() {
        System.out.println("Usage: VideoPlayer [options] [video-file]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help         Show this help message");
        System.out.println("  -a, --audio        Play audio");
        System.out.println("  -x, --maximise     Open the player maximised");
        System.out.println("  -1, --monitor-1    Open the player on display 1");
        System.out.println("  -2, --monitor-2    Open the player on display 2");
    }
}
