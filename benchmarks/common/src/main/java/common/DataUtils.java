package common;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;

public class DataUtils {
    private static final Path TEMP_DIR = Paths.get("temp/");
    private static final Path DOCS_DIR = TEMP_DIR.resolve("docs/");

    public static final Path INDEX_DIR = TEMP_DIR.resolve("index/");

    public static final Path DOCS_FILE = DOCS_DIR.resolve("cran.all.1400");
    public static final Path QUERY_FILE = DOCS_DIR.resolve("cran.qry");

    private static final URL DOCS_URL;

    static {
        try {
            DOCS_URL = new URL("http://ir.dcs.gla.ac.uk/resources/test_collections/cran/cran.tar.gz");
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    public static void initialize() throws IOException {
        createDirectory(INDEX_DIR, true);
        createDirectory(DOCS_DIR, false);
        Path gzipFile = fetchDocs(DOCS_URL, TEMP_DIR);
        decompress(gzipFile, DOCS_DIR);
    }

    private static void decompress(Path gzipFile, Path dir) {
        final TarGZipUnArchiver unarchiver = new TarGZipUnArchiver();
        ConsoleLoggerManager manager = new ConsoleLoggerManager();
        manager.initialize();
        unarchiver.enableLogging(manager.getLoggerForComponent(null));
        unarchiver.setSourceFile(gzipFile.toFile());
        unarchiver.setDestDirectory(dir.toFile());
        unarchiver.extract();
    }

    private static void createDirectory(Path path, boolean deleteIfExists) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        } else if (deleteIfExists) {
            FileUtils.deleteDirectory(path.toFile());
        }
    }

    private static Path fetchDocs(URL url, Path toDir) {
        String urlStr = url.toString();
        int index = urlStr.lastIndexOf('/');
        String fileName = urlStr.substring(index + 1);
        Path path = toDir.resolve(fileName);

        try {
            if (Files.notExists(path)) {
                InputStream in = url.openStream();
                Files.copy(in, path);
                in.close();
            }
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getGlobal().log(Level.SEVERE, "Fetch documents failed");
            System.exit(1);
        }

        return path;
    }
}