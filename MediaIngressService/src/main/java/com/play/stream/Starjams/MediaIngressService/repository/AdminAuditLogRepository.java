package com.play.stream.Starjams.MediaIngressService.repository;

import com.play.stream.Starjams.MediaIngressService.model.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
    Page<AdminAuditLog> findByTargetIdOrderByCreatedAtDesc(String targetId, Pageable pageable);
}
