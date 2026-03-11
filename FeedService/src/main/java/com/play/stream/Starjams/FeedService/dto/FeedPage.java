package com.play.stream.Starjams.FeedService.dto;

import java.util.List;

/**
 * Paginated feed response containing a unified list of ranked items.
 * Items are heterogeneous — FeedEvent, DigestCard, LivestreamCard,
 * TrendingUserCard, or PopularContent — serialized as JSON objects
 * with a {@code cardType} discriminator field.
 */
public class FeedPage {

    private List<Object> items;    // mixed card types
    private String nextCursor;     // null when no more pages
    private boolean hasMore;
    private int limit;

    public FeedPage() {}

    public FeedPage(List<Object> items, String nextCursor, int limit) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasMore = nextCursor != null;
        this.limit = limit;
    }

    public List<Object> getItems() { return items; }
    public void setItems(List<Object> items) { this.items = items; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
