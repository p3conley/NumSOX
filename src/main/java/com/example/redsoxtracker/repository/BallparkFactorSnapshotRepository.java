package com.example.redsoxtracker.repository;

import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BallparkFactorSnapshotRepository extends JpaRepository<BallparkFactorSnapshot, Long> {
    Optional<BallparkFactorSnapshot> findTopByTeamOrderBySnapshotDateDesc(Team team);
    Optional<BallparkFactorSnapshot> findTopByVenueNameIgnoreCaseOrderBySnapshotDateDesc(String venueName);
    List<BallparkFactorSnapshot> findAllByOrderByVenueNameAsc();
}
