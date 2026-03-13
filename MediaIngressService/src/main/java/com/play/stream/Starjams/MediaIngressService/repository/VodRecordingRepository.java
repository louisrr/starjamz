package com.play.stream.Starjams.MediaIngressService.repository;

import com.play.stream.Starjams.MediaIngressService.model.VodRecording;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VodRecordingRepository extends JpaRepository<VodRecording, UUID> {

    Page<VodRecording> findByStatusNot(String status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.fileSizeBytes), 0) FROM VodRecording v WHERE v.status = 'ACTIVE'")
    long sumActiveStorageBytes();
}
