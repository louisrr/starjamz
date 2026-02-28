package com.play.stream.Starjams.UploadService.repositories;

import com.play.stream.Starjams.UploadService.models.UploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadRecordRepository extends JpaRepository<UploadRecord, UUID> {
}
