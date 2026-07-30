package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.repository.TeamRepository;
import com.example.redsoxtracker.repository.TeamStatSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamStatsService {

    private final TeamRepository teamRepository;
    private final TeamStatSnapshotRepository snapshotRepository;

    public TeamStatsService(TeamRepository teamRepository, TeamStatSnapshotRepository snapshotRepository) {
        this.teamRepository = teamRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public Optional<Team> findByCode(String code) {
        return teamRepository.findByTeamCode(code);
    }

    public Optional<Team> findByOpponentName(String opponentName) {
        Optional<Team> byShort = teamRepository.findByShortNameIgnoreCase(opponentName);
        if (byShort.isPresent()) return byShort;
        return teamRepository.findByTeamNameContainingIgnoreCase(opponentName);
    }

    public Optional<TeamStatSnapshot> getLatestStats(Team team) {
        return snapshotRepository.findTopByTeamOrderBySnapshotDateDesc(team);
    }

    public Optional<TeamStatSnapshot> getLatestStats(String teamCode) {
        return teamRepository.findByTeamCode(teamCode)
                .flatMap(snapshotRepository::findTopByTeamOrderBySnapshotDateDesc);
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }
}
