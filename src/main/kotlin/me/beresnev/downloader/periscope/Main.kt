package me.beresnev.downloader.periscope

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

private const val VERSION = "1.0.0" // easier than trying to version jars

suspend fun main(args: Array<String>) {
    if (args.contains("-v") || args.contains("--version")) {
        printVersionHeader()
        return
    }
    require(args.size == 2) {
        "Expected two arguments: a file with broadcast URLs and an output directory path"
    }

    val broadcastUrls = parseBroadcastUrls(args[0])
    val outputDirectory = parseOutputDirectory(args[1])

    println(System.lineSeparator())
    printVersionHeader()
    println(System.lineSeparator())
    println("Starting the download for ${broadcastUrls.size} broadcasts.")
    println("Will dump files to $outputDirectory")
    println("--------------------------")

    val (successfullyDumpedUrls, failedUrls) = downloadBroadcastChatHistories(
        broadcastUrls = broadcastUrls,
        outDir = outputDirectory
    )

    println("Finished dumping periscope chat history:")
    println("* Successfully dumped broadcasts: ${successfullyDumpedUrls.size}")
    println("* Failed: ${failedUrls.size}")

    dumpBroadcastsToFile(broadcastUrls, outputDirectory.resolve("input.txt"))
    dumpBroadcastsToFile(successfullyDumpedUrls, outputDirectory.resolve("success.txt"))
    dumpBroadcastsToFile(failedUrls, outputDirectory.resolve("failed.txt"))
}

private fun printVersionHeader() {
    println("##########################################################")
    println("###      Periscope chat downloader version $VERSION       ###")
    println("### github.com/IgnatBeresnev/periscope-chat-downloader ###")
    println("##########################################################")
}

private fun parseBroadcastUrls(filePath: String): List<String> {
    val broadcastsFile = File(filePath).absoluteFile.also {
        require(it.exists()) { "Expected the first argument to be a file file with broadcast URLs: $it" }
        require(!it.isDirectory) { "Expected the file with broadcast URls to not be a directory: $it" }
    }

    val broadcastUrls = broadcastsFile
        .readLines()
        .filter { it.isNotBlank() }
        .distinct()

    require(broadcastUrls.isNotEmpty()) {
        "Expected the file with broadcast URLs to not be empty"
    }
    require(broadcastUrls.none { it.contains("?") || it.contains("&") }) {
        "The URLs have to be plain and simple. Please, remove any request parameters such as ?t=20s or any other."
    }
    return broadcastUrls
}

fun parseOutputDirectory(outputDirPath: String): File {
    val dir = File(outputDirPath).absoluteFile
    require(!dir.exists()) {
        "Expected output directory to not exist. Please delete or set another one. If you want to continue" +
                "a previously failed download, it's not supported - you'll have to merge the results by hand."
    }
    check(dir.mkdirs()) {
        "Unable to create the directories for output directory: $dir"
    }
    require(dir.isDirectory) {
        "Expected the output directory path to be pointing to a directory, not a file: $dir"
    }
    return dir
}

/**
 * @param broadcastUrls list of full video urls, such as https://www.pscp.tv/ByronBernstein/1ypKdNrmERQJW
 * @return list of successfully processed broadcasts to failed ones
 */
suspend fun downloadBroadcastChatHistories(
    broadcastUrls: List<String>,
    outDir: File
): Pair<List<String>, List<String>> {
    require(outDir.exists()) { "Expected out directory to exist: $outDir" }

    val failedUrls = mutableListOf<String>()

    val httpClient = HttpClient(CIO)
    broadcastUrls.forEachIndexed { index, broadcastUrl ->
        println("Processing broadcast #${index + 1}/${broadcastUrls.size}; URL: $broadcastUrl")
        try {
            val broadcastId = resolveUrlToId(broadcastUrl)
            SingleBroadcastChatDownloader(
                httpClient = httpClient,
                broadcastId = broadcastId,
                broadcastOutDir = outDir.resolve(broadcastId).also {
                    check(it.mkdir()) { "Unable to create an output directory for broadcastUrl $broadcastId" }
                }
            ).download()
        } catch (e: Exception) {
            println("Unable to download broadcast $broadcastUrl:")
            e.printStackTrace(System.out) // Avoid system.err because messages can be out of sync with system.out

            failedUrls.add(broadcastUrl)
        }
    }

    val successfullyDumpedUrls = (broadcastUrls.minus(failedUrls.toSet()))
    return successfullyDumpedUrls to failedUrls
}

/**
 * Some URLs are very long for some reason. However, once you open it in your browser,
 * you'll get redirected to a shorter URL that contains the broadcast id in it.
 *
 * Example: `https://www.pscp.tv/w/aLdqjTY0NDI0NHwxTVl4TnJkWmxreXh3gPBLn2M1B4cniqyW4AgrGnOosIVEe4YeJywT-uzS8c8=`
 *
 * Will get resolved to: `https://www.pscp.tv/w/1vAGRbpaPAkGl`
 *
 * @return id of the broadcast that can be used in API queries, such as `1vAGRbpaPAkGl`
 */
private fun resolveUrlToId(url: String): String {
    try {
        val resolvedUrl = Jsoup.parse(URL(url), TimeUnit.SECONDS.toMillis(10).toInt())
            .select("head > link[data-react-helmet=true]")
            .firstOrNull()
            ?.attr("href")
            ?: error("Did not find the expected HTML element on the given page: head > link[data-react-helmet=true]")

        if (url != resolvedUrl) {
            println("Resolved URL to $resolvedUrl")
        }
        return resolvedUrl.substringAfterLast("/")
    } catch (e: Exception) {
        println("Unable to resolve URL $url")
        throw e
    }
}

/**
 * Helps with debugging and understanding which broadcasts were downloaded this execution
 */
fun dumpBroadcastsToFile(broadcastUrls: List<String>, file: File) {
    val text = broadcastUrls.joinToString(separator = System.lineSeparator())
    file.also { it.createNewFile() }.writeText(text)
}
