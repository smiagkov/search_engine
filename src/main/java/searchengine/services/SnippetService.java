package searchengine.services;

public interface SnippetService {
    String getSnippet(String text, String[] queryLemmas);
}
