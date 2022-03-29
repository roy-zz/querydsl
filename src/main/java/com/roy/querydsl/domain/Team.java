package com.roy.querydsl.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter @Setter
@ToString(of = {"id", "name"})
@NoArgsConstructor(access = PROTECTED)
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team")
    private List<SoccerPlayer> soccerPlayers = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}