package fieldaccess;

import common.Indexer;
import org.apache.lucene.search.SearcherManager;

import java.io.IOException;

public class BackgroundIndexer {
    private final Indexer indexer;
    private SearcherManager searcherManager;
    private volatile boolean stopped = false;

    private Thread thread = null;

    public BackgroundIndexer(Indexer indexer, SearcherManager searcherManager) {
        this.indexer = indexer;
        this.searcherManager = searcherManager;
    }

    void startIndexerThread(int sleepTime, boolean useDocValues) {
        thread = new Thread() {
            public void run() {
                int iteration = 0;
                while (!stopped) {
                    try {
                        indexer.updateIndex(Indexer.NUM_SOURCE_DOCS*10, iteration, true, useDocValues);
                        searcherManager.maybeRefresh();
                        if (iteration % 10 == 9) {
                            indexer.commit();
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
        thread.start();
    }

    void stopThread() {
        stopped = true;
    }

    void join() throws InterruptedException {
        thread.join();
    }
}
