package com.foodya.backend.application.ports.out;

import java.util.List;

public interface AiEmbeddingPort {

    List<Double> embedText(String text);
}
