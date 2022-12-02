package fieldaccess;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.util.Pair;
import common.DocumentModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class Searcher {

    private Analyzer analyzer = new StandardAnalyzer();

    private SearcherManager searcherManager;

    Searcher(SearcherManager searcherManager) {
        this.searcherManager = searcherManager;
    }

    void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    List<Pair<Integer, Long>> search(String queryStr, int maxHits, boolean useDocValues) {
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
                            if (useDocValues) {
                                NumericDocValues ndv = context.reader().getNumericDocValues(DocumentModel.RATING);
                                if (ndv.advanceExact(doc)) {
                                    rating = ndv.longValue();
                                }
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
            if (useDocValues) {
                return hits;
            }

            IndexSearcher finalSearcher = searcher;
            return hits.stream().map(p -> {
                Document doc;
                try {
                    doc = finalSearcher.doc(p.fst, ImmutableSet.of(DocumentModel.RATING));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return new Pair<>(p.fst, doc.getField(DocumentModel.RATING).numericValue().longValue());
            }).collect(Collectors.toList());
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
