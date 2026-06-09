package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.AiEmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies a chat message into one of three categories by comparing the message's
 * embedding against small sets of pre-embedded "anchor" exemplar phrases per category
 * (cosine similarity, max-per-category). This lets the chat pipeline branch into a fast
 * chitchat reply, a hybrid catalog retrieval, or a polite out-of-scope rejection without
 * running the full recommendation pipeline on every message.
 */
class ChatIntentPlanner {

    private static final Logger log = LoggerFactory.getLogger(ChatIntentPlanner.class);

    private static final double SIMILARITY_THRESHOLD = 0.5;

    private static final Map<ChatIntentCategory, List<String>> ANCHOR_PHRASES = Map.of(
            ChatIntentCategory.GENERAL_CHAT, List.of(
                    "Who are you?",
                    "What can you do?",
                    "Tell me about Foodya",
                    "Ban la ai?",
                    "Ban co the lam gi?",
                    "Gioi thieu ve Foodya di"
            ),
            ChatIntentCategory.RECOMMENDATION, List.of(
                    "I want something spicy for dinner",
                    "Suggest a cheap place nearby",
                    "What should I eat tonight?",
                    "Goi y mon an cay gan day",
                    "Tim quan an re gan toi",
                    "Toi muon an gi do cho bua trua"
            ),
            ChatIntentCategory.OUT_OF_SCOPE, List.of(
                    "What's the weather today?",
                    "Help me write some code",
                    "Who won the football match last night?",
                    "Thoi tiet hom nay the nao?",
                    "Giup toi viet code voi",
                    "Doi nao thang tran bong da hom qua?"
            )
    );

    private final AiEmbeddingPort aiEmbeddingPort;

    private volatile Map<ChatIntentCategory, List<List<Double>>> anchorEmbeddingsByCategory;
    private final Object anchorEmbeddingsLock = new Object();

    ChatIntentPlanner(AiEmbeddingPort aiEmbeddingPort) {
        this.aiEmbeddingPort = aiEmbeddingPort;
    }

    ChatIntentCategory classify(List<Double> promptEmbedding) {
        if (promptEmbedding == null || promptEmbedding.isEmpty()) {
            return ChatIntentCategory.RECOMMENDATION;
        }

        Map<ChatIntentCategory, List<List<Double>>> anchors = anchorEmbeddings();
        if (anchors.isEmpty()) {
            return ChatIntentCategory.RECOMMENDATION;
        }

        ChatIntentCategory best = ChatIntentCategory.RECOMMENDATION;
        double bestSimilarity = -1d;
        for (Map.Entry<ChatIntentCategory, List<List<Double>>> entry : anchors.entrySet()) {
            for (List<Double> anchorEmbedding : entry.getValue()) {
                double similarity = cosineSimilarity(promptEmbedding, anchorEmbedding);
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    best = entry.getKey();
                }
            }
        }

        if (bestSimilarity < SIMILARITY_THRESHOLD) {
            return ChatIntentCategory.RECOMMENDATION;
        }
        return best;
    }

    private Map<ChatIntentCategory, List<List<Double>>> anchorEmbeddings() {
        Map<ChatIntentCategory, List<List<Double>>> cached = anchorEmbeddingsByCategory;
        if (cached != null) {
            return cached;
        }
        synchronized (anchorEmbeddingsLock) {
            if (anchorEmbeddingsByCategory == null) {
                anchorEmbeddingsByCategory = computeAnchorEmbeddings();
            }
            return anchorEmbeddingsByCategory;
        }
    }

    private Map<ChatIntentCategory, List<List<Double>>> computeAnchorEmbeddings() {
        Map<ChatIntentCategory, List<List<Double>>> result = new LinkedHashMap<>();
        for (Map.Entry<ChatIntentCategory, List<String>> entry : ANCHOR_PHRASES.entrySet()) {
            List<List<Double>> embeddings = new java.util.ArrayList<>();
            for (String phrase : entry.getValue()) {
                try {
                    List<Double> embedding = aiEmbeddingPort.embedText(phrase);
                    if (!embedding.isEmpty()) {
                        embeddings.add(embedding);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to embed chat-intent anchor phrase '{}'; it will be skipped this run", phrase, ex);
                }
            }
            result.put(entry.getKey(), embeddings);
        }
        return result;
    }

    private static double cosineSimilarity(List<Double> left, List<Double> right) {
        int dim = Math.min(left.size(), right.size());
        if (dim == 0) {
            return 0d;
        }

        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < dim; i++) {
            double l = left.get(i) == null ? 0d : left.get(i);
            double r = right.get(i) == null ? 0d : right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0d || rightNorm == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
