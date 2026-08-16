package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveScoreboardServiceTest {

    @Test
    void completedPlayIncludesNotificationDetailsAndBattingSide() throws Exception {
        MlbApiService api = mock(MlbApiService.class);
        JsonNode feed = new ObjectMapper().readTree("""
                {
                  "gameData": {
                    "status": {
                      "detailedState": "In Progress",
                      "abstractGameState": "Live"
                    }
                  },
                  "liveData": {
                    "linescore": {
                      "teams": {
                        "away": {"runs": 2, "hits": 4, "errors": 0},
                        "home": {"runs": 1, "hits": 3, "errors": 0}
                      },
                      "innings": [],
                      "inningState": "Top",
                      "currentInningOrdinal": "3rd",
                      "inningHalf": "Top",
                      "currentInning": 3,
                      "balls": 0,
                      "strikes": 0,
                      "outs": 1,
                      "offense": {"batter": {"fullName": "Rafael Devers"}},
                      "defense": {"pitcher": {"fullName": "Opponent Pitcher"}}
                    },
                    "plays": {
                      "allPlays": [{
                        "about": {
                          "isComplete": true,
                          "atBatIndex": 17,
                          "halfInning": "top"
                        },
                        "result": {
                          "eventType": "home_run",
                          "event": "Home Run",
                          "description": "Rafael Devers homers to right field."
                        },
                        "matchup": {
                          "batter": {"fullName": "Rafael Devers"}
                        }
                      }]
                    }
                  }
                }
                """);
        when(api.fetchGameFeed(123)).thenReturn(feed);

        Game game = new Game();
        game.setMlbGameId(123);
        game.setOpponent("Pirates");
        game.setHomeAway("Away");
        game.setStatus("In Progress");

        Optional<ScoreboardView> result = new LiveScoreboardService(api).buildForGame(game);

        assertTrue(result.isPresent());
        ScoreboardView board = result.orElseThrow();
        assertEquals("home_run", board.getLastPlayEvent());
        assertEquals("Home Run", board.getLastPlayLabel());
        assertEquals("Rafael Devers homers to right field.", board.getLastPlayDescription());
        assertEquals("Rafael Devers", board.getLastPlayBatter());
        assertEquals(17, board.getLastPlayIndex());
        assertTrue(board.isLastPlayByRedSox());
    }

    @Test
    void ordinaryPitchesAreFormattedAndTerminalPitchIsExcluded() throws Exception {
        MlbApiService api = mock(MlbApiService.class);
        JsonNode feed = new ObjectMapper().readTree("""
                {
                  "gameData": {"status": {"detailedState": "In Progress", "abstractGameState": "Live"}},
                  "liveData": {
                    "linescore": {
                      "teams": {"away": {"runs": 0}, "home": {"runs": 0}},
                      "innings": [], "inningState": "Top", "currentInningOrdinal": "1st",
                      "inningHalf": "Top", "currentInning": 1, "balls": 1, "strikes": 2,
                      "outs": 0, "offense": {"batter": {"fullName": "Test Batter"}},
                      "defense": {"pitcher": {"fullName": "Test Pitcher"}}
                    },
                    "plays": {
                      "allPlays": [],
                      "currentPlay": {
                        "about": {"atBatIndex": 4},
                        "playEvents": [
                          {
                            "isPitch": true, "playId": "pitch-a", "index": 0,
                            "details": {"isInPlay": false, "call": {"description": "Called Strike"},
                                        "type": {"description": "Four-Seam Fastball"}},
                            "count": {"balls": 0, "strikes": 1}, "pitchData": {"startSpeed": 98.9}
                          },
                          {
                            "isPitch": true, "playId": "pitch-b", "index": 1,
                            "details": {"isInPlay": false, "call": {"description": "Foul"},
                                        "type": {"description": "Splitter"}},
                            "count": {"balls": 0, "strikes": 2}, "pitchData": {"startSpeed": 91.7}
                          },
                          {
                            "isPitch": true, "playId": "pitch-c", "index": 2,
                            "details": {"isInPlay": false, "call": {"description": "Ball"},
                                        "type": {"description": "Slider"}},
                            "count": {"balls": 1, "strikes": 2}, "pitchData": {"startSpeed": 84.6}
                          },
                          {
                            "isPitch": true, "playId": "terminal-strike", "index": 3,
                            "details": {"isInPlay": false, "call": {"description": "Called Strike"},
                                        "type": {"description": "Four-Seam Fastball"}},
                            "count": {"balls": 1, "strikes": 3}, "pitchData": {"startSpeed": 97.1}
                          },
                          {
                            "isPitch": true, "playId": "in-play", "index": 4,
                            "details": {"isInPlay": true, "call": {"description": "In play, out(s)"},
                                        "type": {"description": "Sinker"}},
                            "count": {"balls": 1, "strikes": 2}, "pitchData": {"startSpeed": 95.2}
                          }
                        ]
                      }
                    }
                  }
                }
                """);
        when(api.fetchGameFeed(456)).thenReturn(feed);

        Game game = game(456, "In Progress");
        ScoreboardView board = new LiveScoreboardService(api).buildForGame(game).orElseThrow();

        assertEquals(3, board.getRecentPitches().size());
        assertEquals("pitch-a", board.getRecentPitches().get(0).getId());
        assertEquals("0 - 1", board.getRecentPitches().get(0).getCount());
        assertEquals("98.9 mph", board.getRecentPitches().get(0).getSpeed());
        assertEquals("Four-Seam Fastball", board.getRecentPitches().get(0).getType());
        assertEquals("Called Strike", board.getRecentPitches().get(0).getCall());
        assertEquals("pitch-c", board.getRecentPitches().get(2).getId());
    }

    @Test
    void finalFeedOverridesNextBatterAndNormalizesStatus() throws Exception {
        MlbApiService api = mock(MlbApiService.class);
        JsonNode feed = new ObjectMapper().readTree("""
                {
                  "gameData": {"status": {"detailedState": "Completed Early: Rain", "abstractGameState": "Final"}},
                  "liveData": {
                    "linescore": {
                      "teams": {"away": {"runs": 4}, "home": {"runs": 2}},
                      "innings": [], "inningState": "End", "currentInningOrdinal": "9th",
                      "inningHalf": "Bottom", "currentInning": 9, "balls": 0, "strikes": 0,
                      "outs": 3, "offense": {"batter": {"fullName": "Next Batter"}},
                      "defense": {"pitcher": {"fullName": "Winning Pitcher"}}
                    },
                    "plays": {"allPlays": []}
                  }
                }
                """);
        when(api.fetchGameFeed(789)).thenReturn(feed);

        ScoreboardView board = new LiveScoreboardService(api)
                .buildForGame(game(789, "In Progress")).orElseThrow();

        assertTrue(board.isFinalGame());
        assertFalse(board.isLive());
        assertEquals("Final", board.getStatus());
        assertEquals("Final", board.getAtBat());
        assertEquals("Final", board.getPitching());
        assertEquals("Final", board.getInningState());
        assertEquals("END", board.getBreakCaption());
    }

    private Game game(int gameId, String status) {
        Game game = new Game();
        game.setMlbGameId(gameId);
        game.setOpponent("Pirates");
        game.setHomeAway("Away");
        game.setStatus(status);
        return game;
    }
}
