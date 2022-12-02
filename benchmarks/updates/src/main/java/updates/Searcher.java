package updates;

import com.sun.tools.javac.util.Pair;
import common.DocumentModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class Searcher {

    private Analyzer analyzer = new StandardAnalyzer();

    private SearcherManager searcherManager;
    private IndexWriter writer;

    private volatile boolean stopped = false;


    void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    void initReader(IndexWriter writer) {
        this.writer = writer;
        try {
            searcherManager = new SearcherManager(writer, null);

        } catch (IOException e) {
            e.printStackTrace();
            Logger.getGlobal().log(Level.SEVERE, "Read index failed");
            System.exit(1);
        }
    }

    Thread startRefreshCommitThread(Runnable postRefresh, int sleepTime) {
        Thread t = new Thread() {
            public void run() {
                int iteration = 0;
                while (!stopped) {
                    try {
                        searcherManager.maybeRefresh();
                        postRefresh.run();
                        if (iteration % 10 == 9) {
                            writer.commit();
                            iteration = 0;
                        }
                        Thread.sleep(sleepTime);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        return;
                    }
                    ++iteration;
                }
            }
        };
        t.start();
        return t;
    }

    void stopThread() {
        stopped = true;
    }

    ArrayList<Pair<Integer, Long>> search(String queryStr, int maxHits) {
        String fields[] = new String[] { DocumentModel.CONTENT };
        QueryParser parser = new MultiFieldQueryParser(fields, analyzer);

        IndexSearcher searcher = null;
        try {
            searcher = searcherManager.acquire();
            Query query = parser.parse(queryStr);

            ArrayList<Pair<Integer, Long>> hits = new ArrayList<>();
            searcher.search(query, new Collector() {
                @Override
                public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
                    return new LeafCollector() {
                        @Override
                        public void setScorer(Scorer scorer) throws IOException { }

                        @Override
                        public void collect(int doc) throws IOException {
                            long rating = -1;
                            NumericDocValues ndv = context.reader().getNumericDocValues(DocumentModel.RATING);
                            if (ndv.advanceExact(doc)) {
                                rating = ndv.longValue();
                            }
                            hits.add(new Pair<>(doc, rating));
                        }
                    };
                }

                @Override
                public boolean needsScores() {
                    return true;
                }
            });
            hits.subList(maxHits, hits.size()).clear();
            return hits;
        } catch (ParseException e) {
            e.printStackTrace();
            Logger.getGlobal().log(Level.SEVERE, "Can't parse query");
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } finally {
            if (searcher != null) {
                try {
                    searcherManager.release(searcher);
                } catch (IOException e) { }
            }
        }

        return null;
    }
}
