package com.aloute.repository.mysql;

import com.aloute.model.entity.Friendship;
import com.aloute.model.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requesterId = :user1 AND f.addresseeId = :user2) OR " +
           "(f.requesterId = :user2 AND f.addresseeId = :user1)")
    Optional<Friendship> findFriendshipBetween(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT CASE WHEN f.requesterId = :userId THEN f.addresseeId ELSE f.requesterId END " +
           "FROM Friendship f WHERE (f.requesterId = :userId OR f.addresseeId = :userId) " +
           "AND f.status = 'ACCEPTED'")
    List<Long> findFriendIdsByUserId(@Param("userId") Long userId);
    
    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status);

    List<Friendship> findByRequesterIdAndStatus(Long requesterId, FriendshipStatus status);

    long countByStatus(FriendshipStatus status);
}