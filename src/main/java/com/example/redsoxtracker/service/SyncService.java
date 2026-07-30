package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.SyncLog;
import com.example.redsoxtracker.repository.SyncLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SyncService {

    private final SyncLogRepository syncLogRepository;
    private final MlbDataImportService importer;

    public SyncService(SyncLogRepository syncLogRepository, MlbDataImportService importer) {
        this.syncLogRepository = syncLogRepository;
        this.importer = importer;
    }

    public List<SyncLog> getAllSyncLogs() {
        return syncLogRepository.findAllByOrderByDataTypeAsc();
    }

    @Transactional
    public void refreshSchedule() {
        try {
            int count = importer.importSchedule();
            markSynced("schedule", "MLB Stats API", count + " games imported/updated");
        } catch (Exception e) {
            markFailed("schedule", e.getMessage());
        }
    }

    @Transactional
    public void refreshStandings() {
        try {
            importer.importStandings();
        } catch (Exception e) {
            markFailed("standings", e.getMessage());
        }
    }

    @Transactional
    public void refreshTeamStats() {
        try {
            importer.importTeamStats();
        } catch (Exception e) {
            markFailed("team_stats", e.getMessage());
        }
    }

    @Transactional
    public void refreshRoster() {
        try {
            int count = importer.importRoster();
            markSynced("rosters", "MLB Stats API", count + " active roster players imported/updated");
        } catch (Exception e) {
            markFailed("rosters", e.getMessage());
        }
    }

    @Transactional
    public void refreshPlayerStats() {
        try {
            int count = importer.importPlayerStats();
            markSynced("player_stats", "MLB Stats API", count + " player stat snapshots imported/updated");
        } catch (Exception e) {
            markFailed("player_stats", e.getMessage());
        }
    }

    @Transactional
    public void refreshAdvancedMetrics() {
        try {
            importer.importAdvancedMetrics();
        } catch (Exception e) {
            markFailed("advanced_metrics", e.getMessage());
        }
    }

    @Transactional
    public void refreshAll() {
        refreshSchedule();
        refreshStandings();
        refreshTeamStats();
        refreshRoster();
        refreshPlayerStats();
        refreshAdvancedMetrics();
    }

    private void markSynced(String type, String source, String notes) {
        SyncLog log = syncLogRepository.findByDataType(type).orElse(new SyncLog());
        log.setDataType(type);
        log.setLastUpdated(LocalDateTime.now());
        log.setSourceName(source);
        log.setStatus("Success");
        log.setNotes(notes);
        syncLogRepository.save(log);
    }

    private void markFailed(String type, String error) {
        SyncLog log = syncLogRepository.findByDataType(type).orElse(new SyncLog());
        log.setDataType(type);
        log.setLastUpdated(LocalDateTime.now());
        log.setStatus("Failed");
        log.setNotes("Error: " + error);
        syncLogRepository.save(log);
    }
}
