package common;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Indexer {

    public static final int NUM_SOURCE_DOCS = 1000;
    private final DocumentModel[] sourceDocs = new DocumentModel[NUM_SOURCE_DOCS];

    private IndexWriter writer = null;

    private Analyzer analyzer = new StandardAnalyzer();

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public IndexWriter getIndexWriter() {
        return writer;
    }

    public void prepareIndex() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        Directory dir = FSDirectory.open(DataUtils.INDEX_DIR);
        writer = new IndexWriter(dir, config);

        prepareDocuments(NUM_SOURCE_DOCS);
    }

    public void writeIndex(int numDocsToIngest, int generation) throws IOException {
        for (int i = 0; i < numDocsToIngest; ++i) {
            int round = i / NUM_SOURCE_DOCS;
            int offset = i % NUM_SOURCE_DOCS;
            DocumentModel documentModel = sourceDocs[offset];
            Document luceneDoc = new Document();

            StringField id = new StringField(DocumentModel.ID, Integer.toString(documentModel.getId(round * NUM_SOURCE_DOCS)), Field.Store.YES);
            TextField content = new TextField(DocumentModel.CONTENT, documentModel.getContent(), Field.Store.NO);

            luceneDoc.add(id);
            luceneDoc.add(content);
            luceneDoc.add(new NumericDocValuesField(DocumentModel.RATING, generation ));

            writer.addDocument(luceneDoc);
        }
    }

    /**
     *
     * @param numDocsToUpdate number of documents to update
     * @param generation used to write docvalue rating field value
     * @param replace to replace entire document (delete+insert), or update doc value only otherwise
     */
    public void updateIndex(int numDocsToUpdate, int generation, boolean replace) throws IOException {
        for (int i = 0; i < numDocsToUpdate; ++i) {
            int round = i / NUM_SOURCE_DOCS;
            int offset = i % NUM_SOURCE_DOCS;
            DocumentModel documentModel = sourceDocs[offset];
            String docIdStr = Integer.toString(documentModel.getId(round * NUM_SOURCE_DOCS));

            if (replace) {
                StringField id = new StringField(DocumentModel.ID, docIdStr, Field.Store.YES);
                TextField content = new TextField(DocumentModel.CONTENT, documentModel.getContent(), Field.Store.NO);

                Document luceneDoc = new Document();
                luceneDoc.add(id);
                luceneDoc.add(content);
                luceneDoc.add(new NumericDocValuesField(DocumentModel.RATING, generation ));
                writer.updateDocument(new Term(DocumentModel.ID, docIdStr), luceneDoc);
            } else {
                writer.updateNumericDocValue(new Term(DocumentModel.ID, docIdStr), DocumentModel.RATING, generation);
            }
        }
    }

    public void close() throws IOException {
        writer.commit();
        writer.close();
        writer = null;
    }

    private void prepareDocuments(int numDocs) throws IOException {
        int refInt[] = {0};
        DataParser.readDocument(numDocs, document -> {
            int i = refInt[0];
            sourceDocs[i++] = document;
            refInt[0] = i;
        });
    }
}
