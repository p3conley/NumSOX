package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByOpponentContainingIgnoreCaseOrderByGameDateAsc(String opponent);
    List<Game> findByResultOrderByGameDateAsc(String result);
    List<Game> findAllByOrderByGameDateAsc();
    Optional<Game> findByMlbGameId(Integer mlbGameId);
}
