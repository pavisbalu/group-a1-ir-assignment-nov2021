package edu.bits.wilp.ir_assignment.index;

import edu.bits.wilp.ir_assignment.tokenize.DocumentTokenizer;
import edu.bits.wilp.ir_assignment.utils.KryoSerDe;
import edu.bits.wilp.ir_assignment.utils.NumberUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Indexes the documents and writes the tf-idf and document models.
 */
public class Indexer {
    private static final Logger LOG = LoggerFactory.getLogger(Indexer.class);

    public static void main(String[] args) throws IOException {
        new Indexer().index("datasets/sample.csv", "reviews.text", "output.bin", "documents.bin");
    }

    /**
     * Start the indexing process.
     *
     * @throws IOException IOException
     */
    public void index(String filename, String inputFile, String modelFile, String documentsFile) throws IOException {
        LOG.info("Opening " + filename + " for reading");
        Reader in = new FileReader(filename);
        Iterable<CSVRecord> records = csvFormat().parse(in);
        LOG.info("Files read, starting to compute DF");
        List<DocumentMeta> documents = processDocuments(inputFile, records);

        final int N = documents.size();
        Map<String, Double> DF = computeDF(documents);
        LOG.info("DF Complete");
        LOG.info("Starting Tf-Idf calculations");
        TfIdf tfIdfTable = new TfIdf(N, DF);

        // compute tf-idf
        // tf-idf(t, d) = tf(t, d) * log(N/(df + 1))
        for (DocumentMeta document : documents) {
            Set<String> tokens = document.tokens();
            for (String token : tokens) {
                double tf = document.tf(token);
                double df = DF.get(token);
                double idf = Math.log(N + 1 / df + 1);
                double tfIdf = NumberUtil.roundDouble(tf * idf, 4);
                tfIdfTable.add(document.docId, token, tfIdf);
            }
        }
        LOG.info("Tf-Idf calculations, done. Writing the model file: " + modelFile);

        KryoSerDe.writeToFile(tfIdfTable, modelFile);

        LOG.info("Persisting the documents: " + documentsFile);
        List<Document> docsForPersist = documents.stream().map(d -> new Document(d.docId, d.text)).collect(Collectors.toList());
        KryoSerDe.writeToFile(new Documents(docsForPersist), documentsFile);

        LOG.info("Indexing Complete");
    }

    private List<DocumentMeta> processDocuments(String fieldName, Iterable<CSVRecord> records) {
        List<DocumentMeta> documents = new ArrayList<>();
        int docId = 0;
        for (CSVRecord record : records) {
            String reviewsText = record.get(fieldName);
            DocumentTokenizer document = new DocumentTokenizer(List.of(reviewsText));
            document.process();
            Map<String, Double> docTf = document.tf();
            documents.add(new DocumentMeta(docTf, docId, reviewsText));

            // update the docId, so we can consider each review as a new document
            docId++;
        }
        return documents;
    }

    private Map<String, Double> computeDF(List<DocumentMeta> documents) {
        Map<String, Double> DF = new HashMap<>();
        Map<String, List<Integer>> termToDocuments = new HashMap<>();
        // df(t) = occurrence of t in N documents
        // to normalize the value, we also divide the occurrence by total number of documents
        for (DocumentMeta doc : documents) {
            Set<String> tokens = doc.tokens();
            for (String token : tokens) {
                List<Integer> existingDocuments = termToDocuments.getOrDefault(token, new ArrayList<>());
                existingDocuments.add(doc.docId);
                termToDocuments.put(token, existingDocuments);
            }
        }

        final int N = documents.size();
        for (String term : termToDocuments.keySet()) {
            DF.put(term, (double) termToDocuments.get(term).size() / N);
        }
        return DF;
    }

    private CSVFormat csvFormat() {
        return CSVFormat.EXCEL.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
    }
}
