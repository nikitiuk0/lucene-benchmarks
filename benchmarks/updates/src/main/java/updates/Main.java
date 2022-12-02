package updates;

import common.DataUtils;
import common.Indexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
        try {
            state.indexer.updateIndex(1000, (int)(Math.random()*100), state.useReplace);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param({"true", "false"})
        public boolean useReplace;

        public Indexer indexer;
        private Thread refreshThread;
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
                indexer.writeIndex(10000, -1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            searcher = new Searcher();
            searcher.setAnalyzer(analyzer);
            searcher.initReader(indexer.getIndexWriter());

            evaluator = new Evaluator(searcher);

            refreshThread = searcher.startRefreshCommitThread(() -> evaluator.runRandomQuery(), 1000);
            System.out.println("-----Benchmark setup end");
        }

        @TearDown
        public void tearDown() throws InterruptedException {
            System.out.println("-----Benchmark tearDown start");
            searcher.stopThread();
            try {
                refreshThread.join();
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
        bs.useReplace = true;
        bs.setup();
        for (int i = 0; i < 10; ++i) {
            main.runBenchmark(bs);
        }
        bs.tearDown();
    }
}
