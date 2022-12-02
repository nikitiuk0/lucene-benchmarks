package updates;

import com.sun.tools.javac.util.Pair;
import common.DataParser;

import java.util.ArrayList;

class Evaluator {

    private final Searcher searcher;
    private final ArrayList<String> queries = DataParser.readQueries();

    static final int MAX_HITS = 50;
    long numHits = 0;

    Evaluator(Searcher searcher) {
        this.searcher = searcher;
    }

    void runRandomQuery() {
        int i = (int)(Math.random() * queries.size());
        ArrayList<Pair<Integer, Long>> hitDocIds = searcher.search(queries.get(i), MAX_HITS);
        // Uncomment to verify search results:
        // String msg = "Ran search query '" + queries.get(i) + "'. Num hits = " + hitDocIds.size();
        // if (!hitDocIds.isEmpty()) {
        //     msg += ". First hit rating: " + hitDocIds.get(0).snd;
        // }
        // System.out.println(msg);
        numHits += hitDocIds.size();
    }

}
