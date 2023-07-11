package searchengine.dto.parsing;

import org.jsoup.nodes.Document;

public record ParsingResult(int statusCode, Document document) {
}
