package com.play.stream.Starjams.LikeService;

import com.play.stream.Starjams.LikeService.Models.LikeModel;

import java.util.List;
import java.util.UUID;

public interface LikeService {

    /**
     * Adds a like to a stream by a user.
     *
     * @param userId The ID of the user who is liking the stream.
     * @param streamId The ID of the stream being liked.
     */
    void addLike(UUID userId, UUID streamId);

    /**
     * Removes a like from a stream by a user.
     *
     * @param userId The ID of the user whose like is to be removed.
     * @param streamId The ID of the stream from which the like is to be removed.
     */
    void removeLike(UUID userId, UUID streamId);

    /**
     * Counts the total number of likes for a specific stream.
     *
     * @param streamId The ID of the stream.
     * @return The total number of likes.
     */
    long countLikes(UUID streamId);

    /**
     * Checks if a specific user has already liked a particular stream.
     *
     * @param userId The ID of the user.
     * @param streamId The ID of the stream.
     * @return true if the user has liked the stream, false otherwise.
     */
    boolean checkLikeStatus(UUID userId, UUID streamId);

    /**
     * Retrieves a list of stream IDs that a specific user has liked.
     *
     * @param userId The ID of the user.
     * @return A list of stream IDs liked by the user.
     */
    List<String> getLikedStreams(UUID userId);

    /**
     * Gets a list of user IDs who have liked a specific stream.
     *
     * @param streamId The ID of the stream.
     * @return A list of user IDs who have liked the stream.
     */
    List<String> getLikesByStream(UUID streamId);
}
