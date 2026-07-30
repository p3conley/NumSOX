package com.example.redsoxtracker.dto;

public class CategoryComparison {
    private String category;
    private String awayValue;
    private String homeValue;
    private String edge; // "Away", "Home", "Even"

    public CategoryComparison(String category, String awayValue, String homeValue, String edge) {
        this.category = category;
        this.awayValue = awayValue;
        this.homeValue = homeValue;
        this.edge = edge;
    }

    public String getCategory() { return category; }
    public String getAwayValue() { return awayValue; }
    public String getHomeValue() { return homeValue; }
    public String getEdge() { return edge; }

    public boolean isAwayEdge() { return "Away".equals(edge); }
    public boolean isHomeEdge() { return "Home".equals(edge); }
    public boolean isEven() { return "Even".equals(edge); }
}
