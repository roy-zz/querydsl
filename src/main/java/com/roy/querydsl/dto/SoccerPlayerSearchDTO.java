package com.roy.querydsl.dto;

import lombok.Data;

@Data
public class SoccerPlayerSearchDTO {
    private String playerName;
    private String teamName;
    private Integer heightGt;
    private Integer weightGt;
}
