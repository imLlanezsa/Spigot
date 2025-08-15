package io.github.cspigot.utils;

import java.util.*;

/**
 * The ChatPaginator takes a raw string of arbitrary length and breaks it down
 * into an array of strings appropriates for displaying on the Minecraft player
 * console.
 */
public class ChatPaginator {

    public static final int GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH = 55; // Will never wrap, even with the largest characters
    public static final int OPEN_CHAT_PAGE_HEIGHT = 20; // The height of an expanded chat window

    /**
     * Breaks a raw string up into pages using a provided width and height.
     *
     * @param lines The raw string to break.
     * @param pageNumber The page number to fetch.
     * @param pageHeight The desired number of lines in a page.
     * @return A single chat page.
     */
    public static ChatPage paginate(List<String> lines, int pageNumber, int pageHeight) {

        int totalPages = lines.size() / pageHeight + (lines.size() % pageHeight == 0 ? 0 : 1);
        int actualPageNumber = Math.min(pageNumber, totalPages);

        int from = (actualPageNumber - 1) * pageHeight;
        int to = Math.min(from + pageHeight, lines.size());
        List<String> selectedLines = lines.subList(from, to);

        return new ChatPage(selectedLines, actualPageNumber, totalPages);
    }

    public static class ChatPage {

        private final List<String> lines;
        private final int pageNumber;
        private final int totalPages;

        public ChatPage(List<String> lines, int pageNumber, int totalPages) {
            this.lines = lines;
            this.pageNumber = pageNumber;
            this.totalPages = totalPages;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public List<String> getLines() {
            return lines;
        }
    }
}
