package cz.inovatika.sdnnt.index.utils.imports;

import java.util.*;
import java.util.stream.Collectors;

public class AuthorUtils {

    public static List<String> normalizeAndSortAuthorName(String author) {
        if (author == null || author.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleaned = author.replaceAll("[.,]", "");
        List<String> nameParts = Arrays.stream(cleaned.trim().toLowerCase().split("\\s+"))
                .collect(Collectors.toList());
        Collections.sort(nameParts);
        return nameParts;
    }
}
