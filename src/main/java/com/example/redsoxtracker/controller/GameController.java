package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/games")
    public String games(@RequestParam(required = false) String opponent,
                        @RequestParam(required = false) String result,
                        Model model) {
        model.addAttribute("games", gameService.findGames(opponent, result));
        model.addAttribute("opponent", opponent);
        model.addAttribute("result", result);
        return "games";
    }

    @GetMapping("/games/{id}")
    public String gameDetail(@PathVariable Long id, Model model) {
        model.addAttribute("game", gameService.findById(id));
        return "game-detail";
    }

    @PostMapping("/games/{id}/notes")
    public String addNote(@PathVariable Long id,
                          @RequestParam String noteText,
                          @RequestParam(required = false) String tag) {
        gameService.addNote(id, noteText, tag);
        return "redirect:/games/" + id;
    }

    @PostMapping("/notes/{noteId}/delete")
    public String deleteNote(@PathVariable Long noteId,
                             @RequestParam Long gameId) {
        gameService.deleteNote(noteId);
        return "redirect:/games/" + gameId;
    }
}
