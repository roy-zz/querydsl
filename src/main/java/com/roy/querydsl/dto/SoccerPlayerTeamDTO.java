package com.roy.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class SoccerPlayerTeamDTO {
    private Long playerId;
    private String playerName;
    private Long teamId;
    private String teamName;

    @QueryProjection
    public SoccerPlayerTeamDTO(Long playerId, String playerName, Long teamId, String teamName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamId = teamId;
        this.teamName = teamName;
    }

}
