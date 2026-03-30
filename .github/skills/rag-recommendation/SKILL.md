---
name: rag-recommendation
description: "RAG-style recommendation workflow using embeddings and vector retrieval with deterministic ranking output."
argument-hint: "What user context and catalog domain should recommendations be generated for?"
user-invocable: true
---
# RAG Recommendation

## Purpose
Build a recommendation pipeline using embeddings and vector search.

## Stack
- AI Studio embeddings
- Vector search

## Flow
1. Normalize user input and context signals.
2. Embed user query/context.
3. Search vector store for top-K candidates.
4. Apply business filters (availability, policy, ownership, safety).
5. Rank candidates using relevance plus business weights.
6. Return recommendations with explanation fields.

## Decision Points
- If vector recall is low: expand query with context enrichment and retry once.
- If no valid candidates after filtering: return deterministic fallback list.
- If model output contains out-of-domain items: reject and use internal-catalog-only fallback.

## Rules
- Recommendations must be derived from internal valid catalog data.
- Keep retrieval and ranking stages separate for observability.
- Persist enough metadata for later explainability and audit.

## Completion Checks
- Query embedding and retrieval are measurable (latency and hit count).
- Ranking inputs and weights are documented.
- Empty-result and low-confidence fallback path is implemented.
- Output is stable and reproducible for the same input snapshot.
