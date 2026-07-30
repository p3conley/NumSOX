package com.example.redsoxtracker.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "position", length = 5)
    private String position;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(name = "bats", length = 1)
    private String bats;

    @Column(name = "throws_hand", length = 1)
    private String throwsHand;

    @Column(name = "mlb_player_id")
    private Integer mlbPlayerId;

    public Player() {}

    public String getFullName() {
        return (firstName != null ? firstName + " " : "") + lastName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public Integer getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(Integer jerseyNumber) { this.jerseyNumber = jerseyNumber; }
    public String getBats() { return bats; }
    public void setBats(String bats) { this.bats = bats; }
    public String getThrowsHand() { return throwsHand; }
    public void setThrowsHand(String throwsHand) { this.throwsHand = throwsHand; }
    public Integer getMlbPlayerId() { return mlbPlayerId; }
    public void setMlbPlayerId(Integer mlbPlayerId) { this.mlbPlayerId = mlbPlayerId; }
}
