package com.example.redsoxtracker.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service("teamLogoService")
public class TeamLogoService {

    private static final String LOGO_DIR = "/images/team-logos/";
    private static final String FALLBACK_LOGO = "/images/numsox-logo-cropped.png";

    private static final Map<String, String> LOGOS = Map.ofEntries(
            entry("yankees", "1YankeesLogo.png"),
            entry("newyorkyankees", "1YankeesLogo.png"),
            entry("dodgers", "2DodgersLogo.png"),
            entry("losangelesdodgers", "2DodgersLogo.png"),
            entry("cubs", "3CubsLogo.png"),
            entry("chicagocubs", "3CubsLogo.png"),
            entry("redsox", "4RedSox.png"),
            entry("bostonredsox", "4RedSox.png"),
            entry("boston", "4RedSox.png"),
            entry("braves", "5BravesLogo.png"),
            entry("atlantabraves", "5BravesLogo.png"),
            entry("phillies", "6PhilliesLogo.png"),
            entry("philadelphiaphillies", "6PhilliesLogo.png"),
            entry("astros", "7AstrosLogo.png"),
            entry("houstonastros", "7AstrosLogo.png"),
            entry("giants", "8GiantsLogo.png"),
            entry("sanfranciscogiants", "8GiantsLogo.png"),
            entry("rangers", "9RangersLogo.png"),
            entry("texasrangers", "9RangersLogo.png"),
            entry("mariners", "10MarinersLogo.png"),
            entry("seattlemariners", "10MarinersLogo.png"),
            entry("mets", "11MetsLogo.png"),
            entry("newyorkmets", "11MetsLogo.png"),
            entry("angels", "12AngelsLogo.png"),
            entry("losangelesangels", "12AngelsLogo.png"),
            entry("cardinals", "13CardinalsLogo.png"),
            entry("stlouiscardinals", "13CardinalsLogo.png"),
            entry("nationals", "14NationalsLogo.png"),
            entry("washingtonnationals", "14NationalsLogo.png"),
            entry("padres", "15PadresLogo.png"),
            entry("sandiegopadres", "15PadresLogo.png"),
            entry("twins", "16TwinsLogo.png"),
            entry("minnesotatwins", "16TwinsLogo.png"),
            entry("bluejays", "17BlueJaysLogo.png"),
            entry("torontobluejays", "17BlueJaysLogo.png"),
            entry("orioles", "18OriolesLogo.png"),
            entry("baltimoreorioles", "18OriolesLogo.png"),
            entry("royals", "19RoyalsLogo.webp"),
            entry("kansascityroyals", "19RoyalsLogo.webp"),
            entry("whitesox", "20WhiteSoxLogo.png"),
            entry("chicagowhitesox", "20WhiteSoxLogo.png"),
            entry("diamondbacks", "21DiamondbacksLogo.png"),
            entry("arizonadiamondbacks", "21DiamondbacksLogo.png"),
            entry("dbacks", "21DiamondbacksLogo.png"),
            entry("guardians", "22GuardiansLogo.webp"),
            entry("clevelandguardians", "22GuardiansLogo.webp"),
            entry("marlins", "23MarlinsLogo.png"),
            entry("miamimarlins", "23MarlinsLogo.png"),
            entry("reds", "24RedsLogo.png"),
            entry("cincinnatireds", "24RedsLogo.png"),
            entry("rockies", "25RockiesLogo.png"),
            entry("coloradorockies", "25RockiesLogo.png"),
            entry("tigers", "26TigerLogo.png"),
            entry("detroittigers", "26TigerLogo.png"),
            entry("athletics", "27AthleticsLogo.png"),
            entry("oaklandathletics", "27AthleticsLogo.png"),
            entry("sacramentoathletics", "27AthleticsLogo.png"),
            entry("brewers", "28BrewersLogo.png"),
            entry("milwaukeebrewers", "28BrewersLogo.png"),
            entry("rays", "29RaysLogo.png"),
            entry("tampabayrays", "29RaysLogo.png"),
            entry("pirates", "30PiratesLogo.webp"),
            entry("pittsburghpirates", "30PiratesLogo.webp")
    );

    public String logoForName(String teamName) {
        if (teamName == null || teamName.isBlank()) return FALLBACK_LOGO;
        return LOGO_DIR + LOGOS.getOrDefault(normalize(teamName), "4RedSox.png");
    }

    private static Map.Entry<String, String> entry(String key, String fileName) {
        return Map.entry(key, fileName);
    }

    private String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
