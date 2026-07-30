package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.repository.BallparkFactorSnapshotRepository;
import com.example.redsoxtracker.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
public class BallparkFactorService {

    private final BallparkFactorSnapshotRepository parkRepo;
    private final TeamRepository teamRepository;

    public BallparkFactorService(BallparkFactorSnapshotRepository parkRepo, TeamRepository teamRepository) {
        this.parkRepo = parkRepo;
        this.teamRepository = teamRepository;
    }

    public Optional<BallparkFactorSnapshot> getFenwayFactors() {
        return teamRepository.findByTeamCode("BOS")
                .flatMap(parkRepo::findTopByTeamOrderBySnapshotDateDesc)
                .or(() -> fallbackParkByVenueName("Fenway Park"));
    }

    public Optional<BallparkFactorSnapshot> getParkForTeam(Team team) {
        return parkRepo.findTopByTeamOrderBySnapshotDateDesc(team)
                .or(() -> fallbackParkByVenueName(team != null ? team.getHomeVenue() : null));
    }

    public Optional<BallparkFactorSnapshot> getParkByVenueName(String venueName) {
        return parkRepo.findTopByVenueNameIgnoreCaseOrderBySnapshotDateDesc(venueName)
                .or(() -> fallbackParkByVenueName(venueName));
    }

    public List<BallparkFactorSnapshot> getAllParks() {
        List<BallparkFactorSnapshot> parks = parkRepo.findAllByOrderByVenueNameAsc();
        if (!parks.isEmpty()) return parks;
        return PARKS.values().stream()
                .sorted(Comparator.comparing(ParkProfile::name))
                .map(p -> buildFallback(p.name(), p))
                .toList();
    }

    public String describeFactorRelativeToAverage(Integer factor) {
        if (factor == null) return "No data";
        if (factor >= 115) return "Strongly favors offense";
        if (factor >= 107) return "Moderately favors offense";
        if (factor >= 103) return "Slightly above average";
        if (factor >= 97) return "Roughly league average";
        if (factor >= 93) return "Slightly below average";
        if (factor >= 85) return "Moderately suppresses";
        return "Strongly suppresses";
    }

    private Optional<BallparkFactorSnapshot> fallbackParkByVenueName(String venueName) {
        if (venueName == null || venueName.isBlank()) return Optional.empty();
        String key = venueName.toLowerCase();
        for (Map.Entry<String, ParkProfile> entry : PARKS.entrySet()) {
            if (key.contains(entry.getKey())) {
                return Optional.of(buildFallback(entry.getValue().name(), entry.getValue()));
            }
        }
        return Optional.of(buildFallback(venueName, NEUTRAL));
    }

    private BallparkFactorSnapshot buildFallback(String venueName, ParkProfile p) {
        BallparkFactorSnapshot park = new BallparkFactorSnapshot();
        park.setVenueName(venueName);
        park.setSeason(LocalDate.now().getYear());
        park.setSnapshotDate(LocalDate.now());
        park.setParkFactor(p.overall());
        park.setRunParkFactor(p.run());
        park.setHomeRunParkFactor(p.hr());
        park.setHitParkFactor(p.hit());
        park.setDoubleParkFactor(p.doubleF());
        park.setTripleParkFactor(p.tripleF());
        park.setLhbParkFactor(p.lhb());
        park.setRhbParkFactor(p.rhb());
        park.setSingleParkFactor(p.single());
        park.setWalkParkFactor(p.walk());
        park.setStrikeoutParkFactor(p.strikeout());
        park.setBabipParkFactor(p.babip());
        park.setLfHomeRunFactor(p.lfHr());
        park.setCfHomeRunFactor(p.cfHr());
        park.setRfHomeRunFactor(p.rfHr());
        park.setAltitudeFactor(p.altitudeFt());
        park.setMultiYearParkFactor(p.multiYear());
        park.setCity(p.city());
        park.setCapacity(p.capacity());
        park.setSurface(p.surface());
        park.setRoofType(p.roof());
        park.setNotableFeatures(p.notes());
        park.setSourceName("MLB park-factor baseline (multi-year estimate)");
        park.setSourceLastUpdated(LocalDate.now());
        return park;
    }

    /** 100 = league average for every *Factor field. Altitude is feet above sea level, not indexed. */
    private record ParkProfile(
            String name,
            int overall, int run, int hr, int hit, int doubleF, int tripleF,
            int lhb, int rhb, int single, int walk, int strikeout, int babip,
            int lfHr, int cfHr, int rfHr, int altitudeFt, int multiYear,
            String city, Integer capacity, String surface, String roof, String notes) {}

    private static final ParkProfile NEUTRAL = new ParkProfile(
            "Unknown Venue", 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 100,
            null, null, null, null, "Fallback estimate used when detailed park-factor data is not stored locally.");

    private static final Map<String, ParkProfile> PARKS = Map.ofEntries(
            Map.entry("fenway", new ParkProfile("Fenway Park",
                    103, 104, 95, 104, 117, 90, 101, 100, 101, 100, 99, 102, 108, 84, 92, 20, 103,
                    "Boston, MA", 37755, "Grass", "Open",
                    "The 37-foot Green Monster in left field turns long fly balls into doubles, suppressing home runs while producing one of the highest double factors in MLB.")),
            Map.entry("yankee", new ParkProfile("Yankee Stadium",
                    102, 103, 114, 100, 96, 75, 111, 101, 97, 101, 99, 98, 95, 105, 126, 55, 102,
                    "Bronx, NY", 46537, "Grass", "Open",
                    "A short right-field porch (314 ft) fuels one of the league's highest home-run factors, especially for left-handed pull power.")),
            Map.entry("progressive", new ParkProfile("Progressive Field",
                    97, 96, 92, 97, 101, 95, 97, 96, 98, 99, 101, 99, 93, 96, 94, 660, 97,
                    "Cleveland, OH", 34830, "Grass", "Open",
                    "Deep gaps and a below-average home-run factor make it a modest pitcher's park overall.")),
            Map.entry("camden", new ParkProfile("Oriole Park at Camden Yards",
                    99, 98, 95, 98, 101, 85, 97, 99, 99, 100, 100, 99, 92, 99, 97, 35, 99,
                    "Baltimore, MD", 40000, "Grass", "Open",
                    "Bullpen fences were pushed back in 2022, notably suppressing left-handed home runs to left-center.")),
            Map.entry("rogers", new ParkProfile("Rogers Centre",
                    101, 101, 104, 100, 98, 90, 102, 101, 99, 101, 100, 99, 103, 101, 105, 250, 101,
                    "Toronto, ON", 41300, "Artificial Turf", "Retractable",
                    "Artificial turf and a symmetrical outfield produce a fairly neutral, slightly hitter-friendly environment.")),
            Map.entry("tropicana", new ParkProfile("Tropicana Field",
                    96, 95, 92, 95, 97, 80, 95, 96, 96, 98, 102, 96, 91, 93, 92, 50, 96,
                    "St. Petersburg, FL", 25000, "Artificial Turf", "Fixed Dome",
                    "A fixed dome with catwalks and consistent indoor air makes it one of the more pitcher-friendly parks in the league.")),
            Map.entry("minute maid", new ParkProfile("Minute Maid Park",
                    101, 101, 105, 101, 96, 88, 102, 101, 100, 101, 100, 100, 110, 96, 98, 50, 101,
                    "Houston, TX", 41168, "Grass", "Retractable",
                    "The short left-field 'Crawford Boxes' (315 ft) significantly inflate home runs for right-handed pull hitters.")),
            Map.entry("comerica", new ParkProfile("Comerica Park",
                    99, 98, 89, 99, 108, 110, 98, 99, 100, 100, 99, 101, 92, 88, 90, 600, 99,
                    "Detroit, MI", 41083, "Grass", "Open",
                    "One of the deepest outfields in MLB suppresses home runs but produces plenty of doubles and triples in the gaps.")),
            Map.entry("kauffman", new ParkProfile("Kauffman Stadium",
                    101, 101, 86, 100, 108, 115, 100, 101, 101, 99, 99, 101, 88, 85, 88, 750, 101,
                    "Kansas City, MO", 37903, "Grass", "Open",
                    "Spacious outfield territory produces a low home-run factor but well above-average doubles and triples.")),
            Map.entry("target field", new ParkProfile("Target Field",
                    99, 98, 96, 98, 99, 90, 98, 99, 99, 100, 100, 99, 96, 95, 97, 830, 98,
                    "Minneapolis, MN", 38544, "Grass", "Open",
                    "A fairly balanced park with no strong tendencies in either direction.")),
            Map.entry("guaranteed rate", new ParkProfile("Guaranteed Rate Field",
                    102, 102, 108, 101, 98, 85, 103, 102, 100, 101, 99, 100, 106, 104, 109, 594, 102,
                    "Chicago, IL", 40615, "Grass", "Open",
                    "Consistently one of the more homer-friendly parks in the American League.")),
            Map.entry("angel", new ParkProfile("Angel Stadium",
                    99, 98, 97, 99, 98, 92, 98, 99, 99, 100, 100, 99, 96, 97, 98, 150, 99,
                    "Anaheim, CA", 45050, "Grass", "Open",
                    "A well-balanced park close to league average across most categories.")),
            Map.entry("t-mobile", new ParkProfile("T-Mobile Park",
                    94, 93, 91, 93, 95, 95, 93, 94, 94, 99, 102, 97, 91, 90, 92, 350, 94,
                    "Seattle, WA", 47929, "Grass", "Retractable",
                    "Marine air and deep power alleys make it one of the toughest parks in MLB for hitters.")),
            Map.entry("globe life", new ParkProfile("Globe Life Field",
                    101, 101, 103, 100, 99, 90, 101, 101, 100, 100, 99, 99, 102, 102, 103, 550, 101,
                    "Arlington, TX", 40300, "Grass", "Retractable",
                    "Climate-controlled air and hitter-friendly dimensions have made it more offense-friendly than its predecessor.")),
            Map.entry("sutter", new ParkProfile("Sutter Health Park",
                    104, 105, 108, 103, 102, 95, 104, 103, 102, 100, 99, 101, 106, 100, 110, 30, 104,
                    "West Sacramento, CA", 14014, "Grass", "Open",
                    "A minor-league park pressed into temporary MLB service; smaller dimensions and hot Central Valley summer air have made it modestly hitter-friendly.")),
            Map.entry("dodger", new ParkProfile("Dodger Stadium",
                    99, 98, 101, 98, 97, 88, 99, 98, 98, 100, 100, 98, 99, 101, 102, 500, 99,
                    "Los Angeles, CA", 56000, "Grass", "Open",
                    "Cool marine-influenced night air keeps it a modest pitcher's park overall, despite a slightly above-average home-run factor.")),
            Map.entry("coors", new ParkProfile("Coors Field",
                    115, 118, 112, 112, 120, 165, 116, 115, 108, 102, 94, 106, 111, 113, 112, 5280, 116,
                    "Denver, CO", 50398, "Grass", "Open",
                    "The mile-high altitude thins the air, carrying fly balls farther and making it the most extreme hitter's park in MLB, especially for triples in the spacious outfield.")),
            Map.entry("oracle", new ParkProfile("Oracle Park",
                    94, 93, 84, 93, 98, 100, 93, 94, 95, 99, 101, 97, 83, 84, 86, 10, 93,
                    "San Francisco, CA", 41915, "Grass", "Open",
                    "A deep right-center 'Triples Alley' and cold bay air combine for one of the lowest home-run factors in the majors.")),
            Map.entry("petco", new ParkProfile("Petco Park",
                    95, 94, 91, 94, 96, 95, 94, 95, 95, 99, 101, 98, 90, 92, 92, 62, 95,
                    "San Diego, CA", 40209, "Grass", "Open",
                    "Heavy marine air and generous outfield dimensions have long made it a pitcher-friendly environment.")),
            Map.entry("citizens bank", new ParkProfile("Citizens Bank Park",
                    102, 102, 109, 101, 98, 80, 103, 102, 100, 101, 99, 100, 107, 106, 110, 40, 102,
                    "Philadelphia, PA", 42792, "Grass", "Open",
                    "Modest outfield dimensions make it one of the more consistent home-run-friendly parks in the National League.")),
            Map.entry("citi field", new ParkProfile("Citi Field",
                    98, 97, 96, 98, 98, 95, 97, 98, 99, 100, 100, 99, 95, 96, 97, 20, 98,
                    "Queens, NY", 41922, "Grass", "Open",
                    "Fences were moved in during 2012, but it still plays as a slight pitcher's park overall.")),
            Map.entry("truist", new ParkProfile("Truist Park",
                    101, 101, 103, 100, 99, 95, 101, 101, 100, 100, 100, 100, 102, 102, 104, 1050, 101,
                    "Atlanta, GA", 41084, "Grass", "Open",
                    "A fairly balanced modern park with a slight lean toward offense.")),
            Map.entry("loandepot", new ParkProfile("loanDepot Park",
                    96, 95, 90, 95, 98, 100, 95, 96, 96, 99, 101, 98, 90, 91, 91, 10, 96,
                    "Miami, FL", 36742, "Grass", "Retractable",
                    "Deep power alleys and a retractable roof that's often closed keep it one of the tougher parks for home runs.")),
            Map.entry("nationals", new ParkProfile("Nationals Park",
                    100, 100, 99, 100, 101, 95, 100, 100, 100, 100, 100, 100, 98, 99, 99, 10, 100,
                    "Washington, DC", 41339, "Grass", "Open",
                    "One of the most neutral parks in MLB, close to league average in nearly every category.")),
            Map.entry("american family", new ParkProfile("American Family Field",
                    102, 102, 104, 100, 99, 90, 102, 101, 100, 100, 99, 100, 103, 101, 105, 600, 102,
                    "Milwaukee, WI", 41900, "Grass", "Retractable",
                    "A retractable roof and modest fences make it a slightly above-average home-run park.")),
            Map.entry("pnc", new ParkProfile("PNC Park",
                    97, 96, 93, 98, 102, 100, 103, 94, 99, 100, 100, 100, 90, 88, 108, 730, 97,
                    "Pittsburgh, PA", 38747, "Grass", "Open",
                    "Deep left-center field suppresses right-handed power, while a short right-field porch (320 ft) boosts homers for lefties.")),
            Map.entry("busch", new ParkProfile("Busch Stadium",
                    96, 95, 89, 97, 99, 105, 95, 96, 98, 100, 100, 100, 90, 87, 91, 455, 96,
                    "St. Louis, MO", 44494, "Grass", "Open",
                    "Generous outfield dimensions make it a consistent pitcher's park, particularly for home runs.")),
            Map.entry("great american", new ParkProfile("Great American Ball Park",
                    106, 107, 116, 103, 100, 85, 106, 107, 101, 101, 98, 101, 118, 100, 112, 500, 106,
                    "Cincinnati, OH", 42319, "Grass", "Open",
                    "Short outfield porches, especially in left field, make it one of the most home-run-friendly parks in baseball.")),
            Map.entry("chase field", new ParkProfile("Chase Field",
                    100, 100, 102, 99, 99, 95, 100, 100, 100, 100, 100, 99, 101, 99, 103, 1100, 100,
                    "Phoenix, AZ", 48405, "Artificial Turf", "Retractable",
                    "A humidor and switch to artificial turf have brought it close to a neutral, league-average park."))
    );
}
