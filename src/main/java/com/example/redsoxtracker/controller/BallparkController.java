package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.service.BallparkFactorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BallparkController {

    private final BallparkFactorService ballparkFactorService;

    public BallparkController(BallparkFactorService ballparkFactorService) {
        this.ballparkFactorService = ballparkFactorService;
    }

    @GetMapping("/ballpark")
    public String ballpark(Model model) {
        model.addAttribute("parks", ballparkFactorService.getAllParks());
        ballparkFactorService.getFenwayFactors().ifPresent(f -> model.addAttribute("fenway", f));
        return "ballpark";
    }
}
