package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.service.TeamStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class TeamStatsController {

    private final TeamStatsService teamStatsService;

    public TeamStatsController(TeamStatsService teamStatsService) {
        this.teamStatsService = teamStatsService;
    }

    @GetMapping("/team-stats")
    public String teamStats(@RequestParam(defaultValue = "BOS") String teamCode, Model model) {
        Optional<Team> team = teamStatsService.findByCode(teamCode);
        team.ifPresent(t -> {
            model.addAttribute("team", t);
            teamStatsService.getLatestStats(t).ifPresent(s -> model.addAttribute("stats", s));
        });
        model.addAttribute("selectedCode", teamCode);
        model.addAttribute("allTeams", teamStatsService.getAllTeams());
        return "team-stats";
    }
}
