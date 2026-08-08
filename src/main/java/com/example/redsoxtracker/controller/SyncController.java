package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.service.SyncService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/sync")
    public String sync(Model model) {
        model.addAttribute("syncLogs", syncService.getAllSyncLogs());
        return "sync";
    }

    /** Pulls fresh schedule/score data then returns to the dashboard, for the auto-refresh meta tag. */
    @GetMapping("/live-refresh")
    public String liveRefresh() {
        syncService.refreshSchedule();
        return "redirect:/";
    }

    @PostMapping("/sync/refresh")
    public String refresh(@RequestParam(name = "type", defaultValue = "all") String type,
                          @RequestParam(name = "returnTo", required = false) String returnTo) {
        switch (type) {
            case "schedule"   -> syncService.refreshSchedule();
            case "league_schedule" -> syncService.refreshLeagueSchedule();
            case "standings"  -> syncService.refreshStandings();
            case "team_stats" -> syncService.refreshTeamStats();
            case "rosters"    -> syncService.refreshRoster();
            case "player_stats" -> syncService.refreshPlayerStats();
            case "advanced_metrics" -> syncService.refreshAdvancedMetrics();
            default           -> syncService.refreshAll();
        }
        if (returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/sync";
    }
}
