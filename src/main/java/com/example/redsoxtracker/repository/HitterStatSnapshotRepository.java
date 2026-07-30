package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.HitterStatSnapshot;
import com.example.redsoxtracker.domain.Player;
import com.example.redsoxtracker.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HitterStatSnapshotRepository extends JpaRepository<HitterStatSnapshot, Long> {
    List<HitterStatSnapshot> findByTeamAndSeasonOrderByWarDesc(Team team, Integer season);
    Optional<HitterStatSnapshot> findTopByPlayerAndSeasonOrderBySnapshotDateDesc(Player player, Integer season);
}
