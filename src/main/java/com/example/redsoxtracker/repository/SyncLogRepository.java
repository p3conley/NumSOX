package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    List<SyncLog> findAllByOrderByDataTypeAsc();
    Optional<SyncLog> findByDataType(String dataType);
}
