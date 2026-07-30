package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.GameNote;
import com.example.redsoxtracker.repository.GameNoteRepository;
import com.example.redsoxtracker.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameNoteRepository gameNoteRepository;

    public GameService(GameRepository gameRepository, GameNoteRepository gameNoteRepository) {
        this.gameRepository = gameRepository;
        this.gameNoteRepository = gameNoteRepository;
    }

    public List<Game> findGames(String opponent, String result) {
        if (opponent != null && !opponent.isBlank()) {
            return gameRepository.findByOpponentContainingIgnoreCaseOrderByGameDateAsc(opponent);
        }
        if (result != null && !result.isBlank()) {
            return gameRepository.findByResultOrderByGameDateAsc(result);
        }
        return gameRepository.findAllByOrderByGameDateAsc();
    }

    public Game findById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id: " + id));
    }

    @Transactional
    public void addNote(Long gameId, String noteText, String tag) {
        Game game = findById(gameId);
        GameNote note = new GameNote();
        note.setGame(game);
        note.setNoteText(noteText);
        note.setTag(tag);
        gameNoteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        gameNoteRepository.deleteById(noteId);
    }

    public long countWins() {
        return gameRepository.findByResultOrderByGameDateAsc("W").size();
    }

    public long countLosses() {
        return gameRepository.findByResultOrderByGameDateAsc("L").size();
    }
}
