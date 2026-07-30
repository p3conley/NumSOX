package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.service.PlayerStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PlayerStatsController {

    private final PlayerStatsService playerStatsService;

    public PlayerStatsController(PlayerStatsService playerStatsService) {
        this.playerStatsService = playerStatsService;
    }

    @GetMapping("/players")
    public String players(@RequestParam(name = "tab", defaultValue = "roster") String tab, Model model) {
        model.addAttribute("tab", tab);
        model.addAttribute("roster", playerStatsService.getRedSoxRoster());
        model.addAttribute("hitters", playerStatsService.getRedSoxHitters());
        model.addAttribute("starters", playerStatsService.getRedSoxStarters());
        model.addAttribute("bullpen", playerStatsService.getRedSoxBullpen());
        return "player-stats";
    }
}
