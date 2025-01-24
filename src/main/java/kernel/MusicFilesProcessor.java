package kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MusicFilesProcessor {
    private static final List<String> MUSIC_FILES_EXTENSIONS = Arrays.asList(
            ".m4a",
            ".m4b",
            ".aac"
    );

    private MusicFilesProcessor() {

    }

    public static void process() throws Exception {
        final List<File> processList = new ArrayList<>();
        try (Stream<Path> walkStream = Files.walk(Path.of(Config.getValue(ConfigKeys.SRC_FOLDER_PATH)))) {
            walkStream.filter(p -> p.toFile().isFile()).forEach(f -> {
                String sFile = f.getFileName().toString();
                if (sFile.startsWith(".")) {
                    // Ignore hidden files
                    return;
                }
                for (String extension : MUSIC_FILES_EXTENSIONS) {
                    if (sFile.endsWith(extension)) {
                        processList.add(f.toFile());
                        break;
                    }
                }
            });
        }
        if (processList.isEmpty()) {
            System.err.printf("No music to process detected from the folder: %s (and its related sub-directories)%n", Config.getValue(ConfigKeys.SRC_FOLDER_PATH));
            return;
        }
        System.out.printf("%d musics to process from the folder: %s (and its related sub-directories)%n", processList.size(), Config.getValue(ConfigKeys.SRC_FOLDER_PATH));
        int progressPercentage = 0;
        for (int i = 0; i < processList.size(); i++) {
            int currentProgressPercentage = 100 * i / processList.size();
            if (currentProgressPercentage > progressPercentage) {
                progressPercentage = currentProgressPercentage;
                System.out.printf("Progress: %d%% (please wait...)%n", progressPercentage);
            }
            File srcFile = processList.get(i);
            processFile(srcFile);
        }
        System.out.println("Finished !");
    }

    private static void processFile(File file) throws InterruptedException, IOException {
        List<String> cli = new ArrayList<>();
        cli.add("ffmpeg");
        cli.add("-i");
        cli.add(file.toPath().toString());
        cli.add("-f");
        cli.add("ffmetadata");
        cli.add("-");
        ProcessBuilder pb = new ProcessBuilder(cli.toArray(new String[0]));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> inputStreamLines = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))
                .lines().toList();
        String inputStreamContent = String.join("\n", inputStreamLines);
        p.waitFor();
        if (inputStreamLines.isEmpty()) {
            throw new IOException("[FFMPEG] Input stream content must not be empty.\n" + inputStreamContent);
        }
        if (p.exitValue() != 0) {
            throw new IOException("[FFMPEG] Return code: " + p.exitValue() + "\n" + inputStreamContent);
        }
        List<MP4Chapter> chapters = parseChapters(inputStreamLines);
        if (chapters.size() < 2) {
            // Skip chapters-less files and singles
            return;
        }
        String textFromChapters = getTextFromChapters(chapters);
        saveFileWithNewLyrics(file, textFromChapters);
    }

    private static void saveFileWithNewLyrics(File file, String lyrics) throws IOException, InterruptedException {
        List<String> cli = new ArrayList<>();
        cli.add("MP4Box");
        cli.add("-itags");
        cli.add("lyrics=" + lyrics);
        cli.add(file.toPath().toString());
        ProcessBuilder pb = new ProcessBuilder(cli.toArray(new String[0]));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String inputStreamContent = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new IOException("[MP4Box] [SAVE] Return code: " + p.exitValue() + "\nCLI:" + String.join(" ", cli) + "\n" + inputStreamContent);
        }
    }

    private static String getTextFromChapters(List<MP4Chapter> chapters) {
        return chapters.stream().map(MP4Chapter::toHumanFormattedLine).collect(Collectors.joining("\n"));
    }

    private static List<MP4Chapter> parseChapters(List<String> inputStreamLines) throws IOException {
        List<MP4Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < inputStreamLines.size(); i++) {
            String inputStreamLine = inputStreamLines.get(i);
            if (!inputStreamLine.equals("[CHAPTER]")) {
                continue;
            }
            double chapterTimebase = -1;
            long chapterStart = -1;
            long chapterEnd = -1;
            String title = "";
            while (true) {
                i++;
                if (i >= inputStreamLines.size()) {
                    MP4Chapter mp4Chapter = new MP4Chapter(chapterTimebase, chapterStart, chapterEnd, title);
                    if (!MP4Chapter.isValid(mp4Chapter)) {
                        throw new IOException("[FFMPEG] Impossible to parse last chapter.\n" + String.join("\n", inputStreamLines));
                    }
                    chapters.add(mp4Chapter);
                    break;
                }
                inputStreamLine = inputStreamLines.get(i);
                if (inputStreamLine.equals("[CHAPTER]")) {
                    // Next chapter
                    MP4Chapter mp4Chapter = new MP4Chapter(chapterTimebase, chapterStart, chapterEnd, title);
                    if (!MP4Chapter.isValid(mp4Chapter)) {
                        throw new IOException("[FFMPEG] Impossible to parse chapter.\n" + String.join("\n", inputStreamLines));
                    }
                    chapters.add(mp4Chapter);
                    i--;
                    break;
                }
                String[] data = inputStreamLine.split("=", 2);
                switch (data[0].toUpperCase()) {
                    case "TIMEBASE":
                        String[] subData = data[1].split("/");
                        chapterTimebase = Integer.parseInt(subData[0]) / (double) Integer.parseInt(subData[1]);
                        break;
                    case "START":
                        chapterStart = Long.parseLong(data[1]);
                        break;
                    case "END":
                        chapterEnd = Long.parseLong(data[1]);
                        break;
                    case "TITLE":
                        title = data[1]
                                .replace("\\\\", "\\")
                                .replace("\\;", ";")
                                .replace("\\#", "#")
                                .replace("\\=", "=");
                        break;
                }
            }
        }
        return chapters;
    }
}
