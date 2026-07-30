package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByTeamCode(String teamCode);
    Optional<Team> findByShortNameIgnoreCase(String shortName);
    Optional<Team> findByTeamNameContainingIgnoreCase(String name);
    Optional<Team> findByMlbTeamId(Integer mlbTeamId);
}
