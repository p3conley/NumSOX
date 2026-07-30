package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.GameNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameNoteRepository extends JpaRepository<GameNote, Long> {
}
