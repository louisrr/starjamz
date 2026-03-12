package com.play.stream.Starjams.MusicService.repository;

import com.play.stream.Starjams.MusicService.entity.StemRoyaltyLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StemRoyaltyLedgerRepository extends JpaRepository<StemRoyaltyLedger, UUID> {

    List<StemRoyaltyLedger> findByHostUserId(UUID hostUserId);

    @Query("SELECT SUM(r.royaltyAmount) FROM StemRoyaltyLedger r WHERE r.hostUserId = :hostUserId")
    BigDecimal totalEarnedByHost(@Param("hostUserId") UUID hostUserId);

    List<StemRoyaltyLedger> findBySessionId(UUID sessionId);
}
