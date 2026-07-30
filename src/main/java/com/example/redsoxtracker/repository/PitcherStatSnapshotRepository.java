package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.Player;
import com.example.redsoxtracker.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PitcherStatSnapshotRepository extends JpaRepository<PitcherStatSnapshot, Long> {
    List<PitcherStatSnapshot> findByTeamAndSeasonOrderByWarDesc(Team team, Integer season);
    List<PitcherStatSnapshot> findByTeamAndSeasonAndRoleOrderByWarDesc(Team team, Integer season, String role);
    Optional<PitcherStatSnapshot> findTopByPlayerAndSeasonOrderBySnapshotDateDesc(Player player, Integer season);
    Optional<PitcherStatSnapshot> findTopByTeamAndSeasonAndRoleOrderByWarDesc(Team team, Integer season, String role);
}
