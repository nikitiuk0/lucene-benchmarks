package common;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataParser {

    @FunctionalInterface
    public interface DocumentProcessor {
        void process(DocumentModel model);
    }

    public static void readDocument(int numDocs, DocumentProcessor processor) {
        try {
            String text = String.join(" ", Files.readAllLines(DataUtils.DOCS_FILE));
            String lines[] = text.split("\\.I\\s*");
            ArrayList<String> docs = new ArrayList<>(Arrays.asList(lines));
            docs.remove(0);
            docs.subList(numDocs, docs.size()).clear();
            for (String doc: docs) {
                String splits[] = doc.split("\\s*\\.[TABW]\\s*");
                String items[] = new String[5];
                Arrays.fill(items, "");
                System.arraycopy(splits, 0, items, 0, Math.min(splits.length, 5));
                DocumentModel model = new DocumentModel(Integer.parseInt(items[0]), /*items[1], items[2], items[3],*/ items[4]);
                processor.process(model);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getGlobal().log(Level.SEVERE, "Read docs file failed");
            System.exit(1);
        }
    }

    public static ArrayList<String> readQueries() {
        ArrayList<String> queries = new ArrayList<>();
        try {
            String text = String.join(" ", Files.readAllLines(DataUtils.QUERY_FILE));
            text = text.replace("?", "");
            String lines[] = text.split("\\.I.*?.W");
            Collections.addAll(queries, lines);
            queries.remove(0);
            return queries;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getGlobal().log(Level.SEVERE, "Read queries file failed");
            System.exit(1);
        }

        return queries;
    }
}
