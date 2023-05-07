# Periscope Chat Downloader

A simple command-line application that lets you download chat logs of past [periscope](https://www.pscp.tv/) broadcasts.

Accepts a list of broadcast URLs as input, and outputs JSON API responses as provided by Periscope, without 
post-processing.

## Getting started

### Java

To run the downloader, you will need Java 8 (or a more recent version). Any Java distribution should work, but
it's been tested with Eclipse Temurin only.

If you don't have any Java version installed locally, I suggest using [sdkman.io](https://sdkman.io/).

### Download the jar

You can download the downloader jar (executable) in a couple of ways:

* Download a pre-built jar from the [Releases page][2]. Latest version: [periscope-chat-downloader.jar][3].
* Download a pre-built jar from the most [recent GitHub Actions artifacts][1]
* Build one locally by running `./gradlew clean shadowJar` in the root of the cloned project, 
  and run it from the generated `build/libs`.

### Run the jar

Here's how you can run the chat downloader:

```bash
java -jar periscope-chat-downloader.jar broadcasts.txt chatlogs
```

The first argument must be the file that contains a list of broadcast URLs that you want to download chat history for.

Example ([as file](examples/download/broadcasts.txt)):

```
https://www.pscp.tv/ByronBernstein/1lPKqwmwQvAJb
https://www.pscp.tv/ByronBernstein/1gqxvOrDWmnKB
```

The second argument must be the path to the output directory. The directory must not exist.

The paths can be both absolute and relative.

### Example

You see an example of running the downloader, its log output and the output files it generates
in the [examples directory](examples).

## Output files

You can find an example of what this downloader outputs in [examples/download/chatlogs](examples/download/chatlogs).

### Directories:

Each directory represents one broadcast, and the name of the directory is the id of that broadcast.

Within each directory you will find the following files:

* `accessVideoPublic.json` - some general information about the video, this was needed to extract `chat_token`
* `accessChatPublic.json` - some technical information about the chat of this broadcast, contains needed URLs and tokens
* `history-{n}.json` - the chat logs themselves, where each file contains up to 1000 entries. Files are saved
  as returned by the Periscope Chat API, without post-processing, and in exactly the same batches and order as if you
  watched the whole broadcast from start to finish.

### Root files

* [input.txt](examples/download/chatlogs/input.txt) - list of URLs that were provided to the downloader
* [success.txt](examples/download/chatlogs/success.txt) - list of URLs that were successfully processed and dumped
* [failed.txt](examples/download/chatlogs/failed.txt) - list of URLs the processing of which failed in one way or another

## Version

If you're not sure which version of the downloader you have, you can run it with `-v`:

```text
ignat@workstation> java -jar periscope-chat-downloader.jar -v

##########################################################
###      Periscope chat downloader version 1.0.0       ###
### github.com/IgnatBeresnev/periscope-chat-downloader ###
##########################################################
```

[1]: https://github.com/IgnatBeresnev/periscope-chat-downloader/actions?query=branch%3Amaster
[2]: https://github.com/IgnatBeresnev/periscope-chat-downloader/releases
[3]: https://github.com/IgnatBeresnev/periscope-chat-downloader/releases
