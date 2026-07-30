package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.HitterStatSnapshot;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.Player;
import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.repository.HitterStatSnapshotRepository;
import com.example.redsoxtracker.repository.PitcherStatSnapshotRepository;
import com.example.redsoxtracker.repository.PlayerRepository;
import com.example.redsoxtracker.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerStatsService {

    private static final int CURRENT_SEASON = 2026;

    private final HitterStatSnapshotRepository hitterRepo;
    private final PitcherStatSnapshotRepository pitcherRepo;
    private final PlayerRepository playerRepo;
    private final TeamRepository teamRepository;

    public PlayerStatsService(HitterStatSnapshotRepository hitterRepo,
                              PitcherStatSnapshotRepository pitcherRepo,
                              PlayerRepository playerRepo,
                              TeamRepository teamRepository) {
        this.hitterRepo = hitterRepo;
        this.pitcherRepo = pitcherRepo;
        this.playerRepo = playerRepo;
        this.teamRepository = teamRepository;
    }

    public List<Player> getRedSoxRoster() {
        return teamRepository.findByTeamCode("BOS")
                .map(playerRepo::findByTeamOrderByLastNameAsc)
                .orElse(List.of());
    }

    public List<HitterStatSnapshot> getRedSoxHitters() {
        return teamRepository.findByTeamCode("BOS")
                .map(t -> hitterRepo.findByTeamAndSeasonOrderByWarDesc(t, CURRENT_SEASON))
                .orElse(List.of());
    }

    public List<PitcherStatSnapshot> getRedSoxStarters() {
        return teamRepository.findByTeamCode("BOS")
                .map(t -> pitcherRepo.findByTeamAndSeasonAndRoleOrderByWarDesc(t, CURRENT_SEASON, "SP"))
                .orElse(List.of());
    }

    public List<PitcherStatSnapshot> getRedSoxBullpen() {
        return teamRepository.findByTeamCode("BOS")
                .map(t -> pitcherRepo.findByTeamAndSeasonAndRoleOrderByWarDesc(t, CURRENT_SEASON, "RP"))
                .orElse(List.of());
    }

    public Optional<PitcherStatSnapshot> getBestStarterForTeam(Team team) {
        return pitcherRepo.findTopByTeamAndSeasonAndRoleOrderByWarDesc(team, CURRENT_SEASON, "SP");
    }

    public Optional<PitcherStatSnapshot> getPitcherSnapshotByMlbPlayerId(Integer mlbPlayerId) {
        if (mlbPlayerId == null) return Optional.empty();
        return playerRepo.findByMlbPlayerId(mlbPlayerId)
                .flatMap(player -> pitcherRepo.findTopByPlayerAndSeasonOrderBySnapshotDateDesc(player, CURRENT_SEASON));
    }

    public Optional<PitcherStatSnapshot> getRedSoxPitcherSnapshotByName(String fullName) {
        if (fullName == null || fullName.isBlank()) return Optional.empty();
        String target = fullName.trim();
        return teamRepository.findByTeamCode("BOS")
                .flatMap(team -> pitcherRepo.findByTeamAndSeasonOrderByWarDesc(team, CURRENT_SEASON).stream()
                        .filter(snapshot -> target.equalsIgnoreCase(snapshot.getPlayer().getFullName()))
                        .findFirst());
    }

    public List<HitterStatSnapshot> getHittersForTeam(Team team) {
        return hitterRepo.findByTeamAndSeasonOrderByWarDesc(team, CURRENT_SEASON);
    }

    public List<PitcherStatSnapshot> getPitchersForTeam(Team team) {
        return pitcherRepo.findByTeamAndSeasonOrderByWarDesc(team, CURRENT_SEASON);
    }
}
