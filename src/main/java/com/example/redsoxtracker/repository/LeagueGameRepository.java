package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.LeagueGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueGameRepository extends JpaRepository<LeagueGame, Long> {

    Optional<LeagueGame> findByMlbGameId(Integer mlbGameId);

    List<LeagueGame> findByStatusOrderByGameDateAsc(String status);
}
