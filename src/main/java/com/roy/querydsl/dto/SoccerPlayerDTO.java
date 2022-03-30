package com.roy.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SoccerPlayerDTO {
    private String name;
    private int height;

    @QueryProjection
    public SoccerPlayerDTO(String name, int height) {
        this.name = name;
        this.height = height;
    }
}
