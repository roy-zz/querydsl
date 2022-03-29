package com.roy.querydsl.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

import java.util.Objects;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter @Setter
@ToString(of = {"id", "name", "height", "weight"})
@NoArgsConstructor(access = PROTECTED)
public class SoccerPlayer {

    @Id @GeneratedValue
    @Column(name = "soccer_player_id")
    private Long id;
    private String name;
    private int height;
    private int weight;

    @ManyToOne(fetch = LAZY, cascade = ALL)
    @JoinColumn(name = "team_id")
    private Team team;

    public SoccerPlayer(String name) {
        this(name, 0);
    }

    public SoccerPlayer(String name, int height) {
        this(name, height, 0);
    }

    public SoccerPlayer(String name, int height, int weight) {
        this(name, height, weight, null);
    }

    public SoccerPlayer(String name, int height, int weight, Team team) {
        this.name = name;
        this.height = height;
        this.weight = weight;
        if (Objects.nonNull(team)) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getSoccerPlayers().add(this);
    }

}
