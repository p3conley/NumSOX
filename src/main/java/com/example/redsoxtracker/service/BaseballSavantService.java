package com.example.redsoxtracker.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Reads Baseball Savant's own public CSV leaderboard export (the same endpoint behind
 * their site's "Download CSV" button) for Statcast metrics the MLB Stats API doesn't expose:
 * exit velocity, barrel/hard-hit/whiff rate, pitch velocity, and outs above average.
 */
@Service
public class BaseballSavantService {

    private static final Logger log = LoggerFactory.getLogger(BaseballSavantService.class);
    private static final String BASE = "https://baseballsavant.mlb.com";

    private final RestTemplate rest = new RestTemplate();
    private final CsvMapper csvMapper = new CsvMapper();

    /** Batter Statcast quality-of-contact leaderboard, keyed by MLBAM player_id. */
    public List<Map<String, String>> fetchBatterMetrics(int season) {
        String url = BASE + "/leaderboard/custom?year=" + season + "&type=batter&filter=&min=1"
                + "&selections=barrel_batted_rate,hard_hit_percent,exit_velocity_avg,whiff_percent,launch_angle_avg"
                + "&chart=false&x=barrel_batted_rate&y=barrel_batted_rate&r=no&chartType=beeswarm&csv=true";
        return fetchCsv(url);
    }

    /** Pitcher Statcast contact-allowed + velocity leaderboard, keyed by MLBAM player_id. */
    public List<Map<String, String>> fetchPitcherMetrics(int season) {
        String url = BASE + "/leaderboard/custom?year=" + season + "&type=pitcher&filter=&min=1"
                + "&selections=hard_hit_percent,barrel_batted_rate,whiff_percent,exit_velocity_avg,fastball_avg_speed"
                + "&chart=false&x=hard_hit_percent&y=hard_hit_percent&r=no&chartType=beeswarm&csv=true";
        return fetchCsv(url);
    }

    /** Outs Above Average leaderboard for every fielder, one row per player per position. */
    public List<Map<String, String>> fetchOutsAboveAverage(int season) {
        String url = BASE + "/leaderboard/outs_above_average?type=Fielder&startYear=" + season + "&endYear=" + season
                + "&split=no&team=&range=year&min=1&pos=&roles=&viz=show&csv=true";
        return fetchCsv(url);
    }

    private List<Map<String, String>> fetchCsv(String url) {
        try {
            String csv = rest.getForObject(url, String.class);
            if (csv == null || csv.isBlank()) return List.of();
            if (csv.startsWith("﻿")) csv = csv.substring(1);
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> it = csvMapper.readerFor(Map.class).with(schema).readValues(csv);
            return it.readAll();
        } catch (Exception e) {
            log.warn("Baseball Savant CSV fetch failed: {} - {}", url, e.getMessage());
            return List.of();
        }
    }
}
