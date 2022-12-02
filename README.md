# Lucene-Benchmarks

A few benchmarks to measure performance of different operations to be able to reason about different Lucene operations.

So far, there is only one test that compares performance of document updates (these are implemented as delete+insert). 

## Performance results
### Updating document vs updating doc value only

This test first ingests 10k documents from CRAN.REL dataset and then runs multiple iterations of 10k updates

|Benchmark        |(useReplace)|Mode |Cnt|Score  |Error   |Units|
|:--              |:--         |:--  |:--|:--    |:--     |:--  |
|Main.runBenchmark|true        |thrpt|6  | 39.214|±   1.186|ops/s|
|Main.runBenchmark|false       |thrpt|6  |1775.49|± 123.876|ops/s|

**Conclusion:**

* Updating doc values is ~45x times faster than updating entire documents.

### Fetching stored numeric field vs numeric doc value

This test first ingests 10k documents from CRAN.REL dataset and then runs multiple searches (with having updates and
refreshes performed in background)

|Benchmark        |(useDocValues)|Mode |Cnt|Score  |Error   |Units|
|:--              |:--           |:--  |:--|:--    |:--     |:--  |
|Main.runBenchmark|true          |thrpt|6  |3930.220|± 380.659|ops/s|
|Main.runBenchmark|false         |thrpt|6  |4899.003|± 502.397|ops/s|

**Conclusion:**

* A search with fetching a single number from a doc value is ~20% slower than a search with fetching that number from a stored field
* Fetching doc values vs fetching stored fields have a limited impact on search performance, as the majority of time is spent in query phase compared to fetch phase. 

## How to run

```bash
# Make sure gradle is installed.
git clone https://github.com/nikitiuk0/lucene-benchmarks.git
cd lucene-benchmarks
./gradlew run
```

## Class Structure

|Class|Functionality|
|:--:|:--|
|FileUtils|Create local folder structures, automatically download and decompress [Cranfield](http://ir.dcs.gla.ac.uk/resources/test_collections/cran/) files. Provide access to all necessary files (Cranfield files, index files).|
|FileParser|A parser used to parse and load Cranfield files into a Java supported data structre. |
|DocumentModel|A simple object-oriented way to represent Cranfield documents.|
|Indexer|Responsible for creating an index and writing/updating the data.|
|Searcher|Read indices created by Indexer, parse the query string and exectute search. Also runs a background thread the performs a periodic refresh.|
|Evaluator|Executes a search, can inspect the results for testing.|
|Main|Entry of program, runs performance tests.|

## Acknowledgements
This project is partially based on https://github.com/PointerFLY/Lucene-Example.git
