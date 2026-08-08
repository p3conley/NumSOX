package com.example.redsoxtracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class StartupSyncService {

    private static final Logger log = LoggerFactory.getLogger(StartupSyncService.class);

    private final MlbDataImportService importer;

    public StartupSyncService(MlbDataImportService importer) {
        this.importer = importer;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void onStartup() {
        log.info("=== Startup sync: seeding static data and fetching live MLB data ===");

        // 1. Seed teams / park factors if this is a fresh DB
        importer.seedStaticDataIfNeeded();

        // 2. Pull live data from MLB Stats API
        try {
            log.info("Fetching schedule from MLB Stats API...");
            int games = importer.importSchedule();
            log.info("Schedule: {} games imported", games);
        } catch (Exception e) {
            log.warn("Schedule import failed: {}", e.getMessage());
        }

        try {
            log.info("Fetching league-wide results for historical standings...");
            int leagueGames = importer.importLeagueSchedule();
            log.info("League schedule: {} games imported", leagueGames);
        } catch (Exception e) {
            log.warn("League schedule import failed: {}", e.getMessage());
        }

        try {
            log.info("Fetching standings...");
            importer.importStandings();
        } catch (Exception e) {
            log.warn("Standings import failed: {}", e.getMessage());
        }

        try {
            log.info("Fetching team stats...");
            importer.importTeamStats();
        } catch (Exception e) {
            log.warn("Team stats import failed: {}", e.getMessage());
        }

        try {
            log.info("Fetching player stats...");
            importer.importPlayerStats();
        } catch (Exception e) {
            log.warn("Player stats import failed: {}", e.getMessage());
        }

        try {
            log.info("Fetching advanced Statcast metrics from Baseball Savant...");
            importer.importAdvancedMetrics();
        } catch (Exception e) {
            log.warn("Advanced metrics import failed: {}", e.getMessage());
        }

        log.info("=== Startup sync complete ===");
    }
}
