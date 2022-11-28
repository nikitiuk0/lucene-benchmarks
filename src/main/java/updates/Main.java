package updates;

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
        state.indexer.updateIndex(10000, (int)(Math.random()*100), state.useReplace);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param({"true", "false"})
        public boolean useReplace;

        public Indexer indexer;
        private Thread refreshThread;
        private Evaluator evaluator;

        @Setup
        public void setup() {
            FileUtils.initialize();

            Analyzer analyzer = new StandardAnalyzer();
            indexer = new Indexer();
            indexer.setAnalyzer(analyzer);
            indexer.prepareIndex();
            indexer.writeIndex(10000, -1);

            Searcher searcher = new Searcher();
            searcher.setAnalyzer(analyzer);
            searcher.initReader(indexer.writer);

            evaluator = new Evaluator(searcher);

            refreshThread = searcher.startRefreshThread(() -> evaluator.runRandomQuery(), 1000);
        }

        @TearDown
        public void tearDown() throws InterruptedException {
            refreshThread.interrupt();
            try {
                refreshThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            indexer.close();

            Thread.sleep(100);

            System.out.println("Num hits total " + evaluator.numHits);
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
