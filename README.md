# MP4 Chapters As Lyrics
*A simple program calling ffmpeg and mp4box to put human readable chapters data of an mp4 container into the lyrics metadata (so iPods can use it to see the titles of the songs when CDs are encoded as gapless tracks for example).*

## Features
- A textual configuration file parsed at launch !
- Can look in the source folder (**and all the related sub-folders**) for mp4 files to process !
- Show the current progress in your console, percent by percent to keep track of what is going on
- Multiplatform (Java 17). **You will need to install Java 17 in order to run this program**.

## External dependencies
You must install ffmpeg and mp4box in your path in order for this program to work.

## How to use
1) Download the latest release: https://github.com/Olsro/MP4ChaptersAsLyrics/releases
2) Unzip the release then extract everyting where your music files are located
3) Open a terminal window in this folder then run the program using this simple command: ```java -jar cal.jar```

If everything went great, you should see the lyrics when you transfer the resulting file into iTunes:
![Alt text](images/result.jpeg?raw=true "Result")

## The configuration file
```SRC_FOLDER_PATH``` is the path to the source folder. The program will scan all music files.

## Support my work
You can tip me on Patreon: https://www.patreon.com/Olsro and star + follow my repos, thank you !