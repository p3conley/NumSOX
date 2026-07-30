# NumSOX

A Boston Red Sox analytics dashboard built with Java and Spring Boot. NumSOX pulls live schedule, roster, and stat data from the MLB Stats API and Baseball Savant, then turns it into a guided, story-first view of each game: who's playing, who's pitching, who has the edge, and why — with deeper sabermetrics available on demand rather than dumped on the page.

## Features

- **Dashboard** — featured game with a live scoreboard, win probability, ballpark factor summary, and Red Sox season snapshot
- **League standings** — full AL/NL breakdown by division, with games-back, wild-card position, run differential, and home/away splits
- **Matchup Center** — side-by-side team comparison, probable starters, category-by-category breakdown, strengths/weaknesses, ballpark fit, and a historical calibration backtest for the win probability model
- **Games** — full season schedule and results, with per-game notes
- **Team stats** — every MLB team's offense, pitching, bullpen, and defensive metrics
- **Players** — roster, hitters, starters, and bullpen views with expandable advanced-stat and Statcast sections
- **Ballpark factors** — park-adjusted run/HR/hit factors for all 30 current MLB venues
- **Sync** — a status page showing when each data source last refreshed, with manual refresh controls per data type

## Data sources

| Data | Source |
|---|---|
| Schedule, results, rosters, box scores | [MLB Stats API](https://statsapi.mlb.com) |
| Team/player WAR, wRC+, xFIP, FIP-, ERA- | MLB Stats API sabermetrics feed |
| Exit velocity, hard-hit%, barrel%, whiff%, pitch velocity, Outs Above Average | [Baseball Savant](https://baseballsavant.mlb.com) public leaderboard CSV export |
| Ballpark factors | Built-in multi-year static reference data |

**Not available:** DRS (Baseball Info Solutions) and UZR (FanGraphs) are both proprietary datasets with no free public API, so NumSOX intentionally omits them rather than fabricate or estimate them.

Both live data sources are first-party MLB properties. Data is fetched read-only, on-demand, via each site's own public export endpoints — this project does not scrape rendered pages or bypass any access controls.

## Tech stack

- Java 21
- Spring Boot 3.5 (Web, Thymeleaf, Spring Data JPA)
- H2 (file-based, default) or PostgreSQL
- Jackson (JSON + CSV)

## Getting started

### Prerequisites

- JDK 21
- Apache Maven (this project does not bundle the Maven wrapper — install Maven separately if `mvn` isn't recognized)

### Run it

```bash
mvn spring-boot:run
```

On Windows, you can also double-click `run-app.bat`, or run `.\run-app.ps1` from PowerShell.

Then open:

```
http://localhost:8080
```

On first launch, the app seeds static reference data and pulls the current season's schedule, standings, team stats, rosters, player stats, and Statcast metrics automatically. This takes a few seconds.

### Refreshing data

There is no scheduled background job — data updates on startup and whenever you click a **Refresh** button on the [Sync](http://localhost:8080/sync) page (or "Refresh score" on the dashboard for the featured game). Restart the app or hit Refresh to pull the latest scores and stats.

## Database

By default the app uses a file-based H2 database at `./data/redsox-tracker`, so fetched data survives restarts. Browse it at `http://localhost:8080/h2-console` using the JDBC URL from `application.properties`.

To use PostgreSQL instead:

1. Create a database named `redsox_tracker`.
2. Set your password in `src/main/resources/application-postgres.properties`.
3. Run with the `postgres` profile:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

## Project structure

```
src/main/java/com/example/redsoxtracker/
  controller/   Thymeleaf page controllers
  service/      Data import, standings, matchup, win-probability, ballpark-factor logic
  domain/       JPA entities
  dto/          View-model objects for templates
  repository/   Spring Data repositories
src/main/resources/
  templates/    Thymeleaf pages
  static/       CSS, images, team logos
```
