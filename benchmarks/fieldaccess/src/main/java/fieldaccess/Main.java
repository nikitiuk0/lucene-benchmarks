package fieldaccess;

import common.DataUtils;
import common.Indexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.SearcherManager;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }

    @Benchmark
    @Threads(1)
    @Fork(2)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    public void runBenchmark(BenchmarkState state) {
        state.evaluator.runRandomQuery(state.useDocValues);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param({"true", "false"})
        public boolean useDocValues; // uses stored field if false
        public Indexer indexer;
        private BackgroundIndexer backgroundIndexer;
        private Evaluator evaluator;
        private Searcher searcher;

        @Setup
        public void setup() {
            System.out.println("-----Benchmark setup start");
            try {
                DataUtils.initialize();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Analyzer analyzer = new StandardAnalyzer();
            indexer = new Indexer();
            indexer.setAnalyzer(analyzer);
            try {
                indexer.prepareIndex();
                indexer.writeIndex(Indexer.NUM_SOURCE_DOCS * 10, -1, useDocValues);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            SearcherManager searcherManager;
            try {
                searcherManager = new SearcherManager(indexer.getIndexWriter(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            searcher = new Searcher(searcherManager);
            searcher.setAnalyzer(analyzer);

            evaluator = new Evaluator(searcher);

            backgroundIndexer = new BackgroundIndexer(indexer, searcherManager);
            backgroundIndexer.startIndexerThread(1000, useDocValues);
            System.out.println("-----Benchmark setup end");
        }

        @TearDown
        public void tearDown() throws InterruptedException {
            System.out.println("-----Benchmark tearDown start");
            backgroundIndexer.stopThread();
            try {
                if (backgroundIndexer != null) backgroundIndexer.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                indexer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Thread.sleep(1000);

            System.out.println("----------Num hits total " + evaluator.numHits);
            System.out.println("-----Benchmark tearDown end");
        }

    }

    private static void runManualTests() throws InterruptedException {
        Main main = new Main();
        BenchmarkState bs = new BenchmarkState();
        bs.useDocValues = false;
        bs.setup();
        for (int i = 0; i < 10; ++i) {
            main.runBenchmark(bs);
            Thread.sleep(1000);
        }
        bs.tearDown();
    }
}
