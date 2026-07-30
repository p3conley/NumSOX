package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.Player;
import com.example.redsoxtracker.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeamOrderByLastNameAsc(Team team);
    List<Player> findByTeamAndPositionOrderByLastNameAsc(Team team, String position);
    Optional<Player> findByMlbPlayerId(Integer mlbPlayerId);
}
