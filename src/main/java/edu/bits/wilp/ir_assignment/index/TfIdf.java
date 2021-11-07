package edu.bits.wilp.ir_assignment.index;

import java.util.*;

// We'll persist the instance of this class as the index file (aka) model
public class TfIdf implements Iterable<TfIdItem> {
    private final int totalDocuments;
    private final List<String> vocab;
    private final Map<TfIdfKey, Double> tfIdfVectors;
    private final Map<String, Double> documentFrequencies;

    // for kryo
    private TfIdf() {
        this(0, new HashMap<>());
    }

    public TfIdf(int totalDocuments, Map<String, Double> documentFrequencies) {
        this.totalDocuments = totalDocuments;
        vocab = new ArrayList<>();
        tfIdfVectors = new HashMap<>();
        this.documentFrequencies = documentFrequencies;
    }

    public void add(int docId, String token, double tfIdf) {
        int tokenAsVocabId = vocab.indexOf(token);
        if (tokenAsVocabId == -1) {
            vocab.add(token);
            tokenAsVocabId = vocab.indexOf(token);
        }
        tfIdfVectors.put(new TfIdfKey(docId, tokenAsVocabId), tfIdf);
    }

    public int indexOf(String token) {
        return vocab.indexOf(token);
    }

    // Size of the documents
    public int N() {
        return totalDocuments;
    }

    // Size of the vocab
    public int size() {
        return tfIdfVectors.size();
    }

    public double DF(String token) {
        return this.documentFrequencies.getOrDefault(token, 0.0);
    }

    @Override
    public Iterator<TfIdItem> iterator() {
        return new Iterator<>() {
            private Iterator<Map.Entry<TfIdfKey, Double>> underlying = tfIdfVectors.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return underlying.hasNext();
            }

            @Override
            public TfIdItem next() {
                Map.Entry<TfIdfKey, Double> kv = underlying.next();
                TfIdfKey key = kv.getKey();
                return new TfIdItem(vocab.get(key.getTokenAsVocabId()), key.getDocId(), kv.getValue());
            }
        };
    }
}