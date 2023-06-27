package searchengine.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.utils.PageParsingUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PageParsingUtilsTest {
    @Autowired
    private PageParsingUtils pageParsingUtils;

    @Test
    public void getRelativePathTest() {
        assertAll(
                () -> assertEquals("/path/", pageParsingUtils.getRelativePath("https://host/path/")),
                () -> assertEquals("/path/", pageParsingUtils.getRelativePath("/path/")),
                () -> assertEquals("/part1/part2/", pageParsingUtils.getRelativePath("https://host/part1/part2/")),
                () -> assertEquals("/part1/part2?param=value", pageParsingUtils.getRelativePath("https://host/part1/part2?param=value"))
        );

    }

    @Test
    public void isValidChildLinkTest() {
        assertAll(
                () -> assertTrue(pageParsingUtils.isValidChildLink("/path/part1/", "https://host/")),
                () -> assertTrue(pageParsingUtils.isValidChildLink("https://host/path/part1/", "https://host/")),
                () -> assertTrue(pageParsingUtils.isValidChildLink("https://host/path/part1/page.html", "https://host/")),
                () -> assertFalse(pageParsingUtils.isValidChildLink("https://host/path/part1/picture.png", "https://host/")),
                () -> assertFalse(pageParsingUtils.isValidChildLink("https://host/path/part1/", "https://other_host/")),
                () -> assertFalse(pageParsingUtils.isValidChildLink("https://host/path/part1/", "https://other_host/")),
                () -> assertFalse(pageParsingUtils.isValidChildLink("https://host/path/part1/#", "https://host/")),
                () -> assertFalse(pageParsingUtils.isValidChildLink("//path/part1/", "https://other_host/"))
        );

    }
}