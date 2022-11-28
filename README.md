# Lucene-Benchmarks

A few benchmarks to measure performance of different operations to be able to reason about different Lucene operations.

So far, there is only one test that compares performance of document updates (these are implemented as delete+insert). 

## Performance results
### Updating document vs updating doc value only

Note: This test first ingests 10k documents from CRAN.REL dataset and then runs multiple iterations of 10k updates


|Benchmark        |(useReplace)|Mode |Cnt|Score  |Error   |Units|
|:--              |:--         |:--  |:--|:--    |:--     |:--  |
|Main.runBenchmark|true        |thrpt|6  |3.711  |±  0.206|ops/s|
|Main.runBenchmark|false       |thrpt|6  |199.992|± 75.159|ops/s|

**Conclusion:**

* Updating doc values is ~55x times faster than updating entire documents.

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






