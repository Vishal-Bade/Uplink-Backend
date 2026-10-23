package com.example.backend.search;

import java.util.List;

public interface SearchProvider {
    List<SearchResult> search(String query, int maxResults);
    String getProviderName();
}
