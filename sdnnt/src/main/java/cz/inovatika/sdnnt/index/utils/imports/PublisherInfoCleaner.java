package cz.inovatika.sdnnt.index.utils.imports;

import java.util.*;
import java.util.stream.Collectors;

public class PublisherInfoCleaner {

    private static final List<String> REMOVE_WORDS = Arrays.asList(
        "sro", "a.s", "as", "nakladatelstvi", "vydavatelstvo"
    );

    public static String normalizePublisher(String text) {
        StringBuilder builder = new StringBuilder();
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        List<String> found = new ArrayList<>();
        String lowerText = text.toLowerCase();
        String[] words = text.split("\\s+");
        String collected = Arrays.stream(words)
                .map(ImporterUtils::normalize)
                .filter(word -> !REMOVE_WORDS.contains(word))
                .collect(Collectors.joining(" "));
        return collected;
    }

    public static void main(String[] args) {
        String input1 = "PORTÁL, s.r.o.";
        String input2 = "Wolters Kluwer ČR, a.s.";
        String input3 = "Albatros Media a.s.";
        String input4 = "Nakladatelství XYZ";

        System.out.println(normalizePublisher(input1)); // [sro]
        System.out.println(normalizePublisher(input2)); // [as]
        System.out.println(normalizePublisher(input3)); // [as]
        System.out.println(normalizePublisher(input4)); // [nakladatelstvi]
    }
}
