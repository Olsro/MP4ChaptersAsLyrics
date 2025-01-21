package kernel;

public class Main {

    public static void main(String[] args) {
        System.out.println("MP4ChaptersAsLyrics v1.1.0 - Written by OlsroFR");
        System.out.println("Depends from ffmpeg and mp4box (must be in your path)");
        try {
            Config.loadConfig();
        } catch (Exception e) {
            System.err.println("FATAL ERROR WHEN LOADING CONFIGURATION FILE");
            e.printStackTrace();
            return;
        }
        try {
            MusicFilesProcessor.process();
        } catch (Exception e) {
            System.err.println("FATAL ERROR WHILE PROCESSING MUSIC FILES");
            e.printStackTrace();
            return;
        }
    }
}