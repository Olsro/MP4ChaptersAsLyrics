package kernel;

import java.text.DecimalFormat;
import java.time.Duration;

public record MP4Chapter(
        double timebase,
        long start,
        long end,
        String title
) {
    public static boolean isValid(MP4Chapter mp4Chapter) {
        if (mp4Chapter.timebase() < 0) {
            return false;
        }
        if (mp4Chapter.start() < 0) {
            return false;
        }
        if (mp4Chapter.end() < 0) {
            return false;
        }
        return true;
    }

    public static String toHumanFormattedLine(MP4Chapter mp4Chapter) {
        Duration duration = Duration.ofMillis(mp4Chapter.start());
        // FIXME What if in the metadata it's using a different base than 1/1000 ? Though my version of FFMPEG seems to always produce a 1/1000 base...
        DecimalFormat df = new DecimalFormat("00");
        StringBuilder sb = new StringBuilder();
        sb.append(duration.toHoursPart());
        sb.append(":");
        sb.append(df.format(duration.toMinutesPart()));
        sb.append(":");
        sb.append(df.format(duration.toSecondsPart()));
        if (!mp4Chapter.title().isBlank()) {
            sb.append(" ");
            sb.append(mp4Chapter.title().trim());
        }
        return sb.toString();
    }
}