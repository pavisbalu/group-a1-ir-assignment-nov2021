package edu.bits.wilp.ir_assignment.tokenizing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

// Refer to tests for documentation / process
public class DocumentTokenizer {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentTokenizer.class);
    private final List<String> linesInDocument;
    private final Map<String, Double> tf;

    public DocumentTokenizer(List<String> linesInDocument) {
        this.linesInDocument = linesInDocument;
        this.tf = new HashMap<>();
    }

    public void process() {
        // we don't want to tokenize the document twice and mess up the termFrequency calculations
        if (tf.size() > 0) {
            LOG.warn("Calling tokenize twice is not recommended. Please call DocumentTokenizer#tokenize only once in your code.");
            return;
        }

        Map<String, Double> termCounts = linesInDocument.stream()
                .map(this::lineToTokens)
                .flatMap(this::preprocess)
                .reduce(new HashMap<>(), termCount(), DocumentTokenizer.mergeCounters());

        // round the tf to 4 decimal places
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);

        // compute TF
        // tf(t,d) = count of t in d / number of words in d
        tf.clear(); // paranoid check to avoid tf calculations mismatch
        int N = termCounts.size();
        for (String token : termCounts.keySet()) {
            tf.put(token, Double.valueOf(df.format(termCounts.get(token) / N)));
        }
    }

    public Set<String> tokens() {
        return this.tf.keySet();
    }

    public Map<String, Double> tf() {
        return this.tf;
    }

    private String[] lineToTokens(String line) {
        return line.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+");
    }

    private Stream<String> preprocess(String[] tokens) {
        return Arrays.stream(tokens)
                .map(Stemmer::stemWord)
                .filter(StopWords::isNotStopWord);
    }

    private BiFunction<Map<String, Double>, String, Map<String, Double>> termCount() {
        return (counter, word) -> {
            Double count = counter.getOrDefault(word, 0.0);
            counter.put(word, count + 1.0);
            return counter;
        };
    }

    public static BinaryOperator<Map<String, Double>> mergeCounters() {
        return (left, right) -> {
            Map<String, Double> mergedCounter = new HashMap<>();
            left.keySet().forEach(word -> {
                Double counterSoFar = mergedCounter.getOrDefault(word, 0.0);
                mergedCounter.put(word, counterSoFar + left.get(word));
            });

            right.keySet().forEach(word -> {
                Double counterSoFar = mergedCounter.getOrDefault(word, 0.0);
                mergedCounter.put(word, counterSoFar + right.get(word));
            });

            return mergedCounter;
        };
    }
}