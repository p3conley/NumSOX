package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamStatSnapshotRepository extends JpaRepository<TeamStatSnapshot, Long> {
    Optional<TeamStatSnapshot> findTopByTeamOrderBySnapshotDateDesc(Team team);
    Optional<TeamStatSnapshot> findTopByTeamAndSeasonOrderBySnapshotDateDesc(Team team, Integer season);
}
