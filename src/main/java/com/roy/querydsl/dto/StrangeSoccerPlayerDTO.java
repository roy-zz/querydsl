package com.roy.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class StrangeSoccerPlayerDTO {
    private String whatYourName;
    private int howTallAreYou;

    public StrangeSoccerPlayerDTO(String whatYourName, int howTallAreYou) {
        this.whatYourName = whatYourName;
        this.howTallAreYou = howTallAreYou;
    }
}
