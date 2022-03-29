package com.roy.querydsl;


import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@Transactional
@SpringBootTest
public class QuerydslBasicGrammarTest {

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void before() {
        Team teamA = new Team("TeamA");
        Team teamB = new Team("TeamB");
        List<SoccerPlayer> players = List.of(
                new SoccerPlayer("Roy", 173, 73, teamA),
                new SoccerPlayer("Perry", 175, 75, teamA),
                new SoccerPlayer("Sally", 160, 60, teamB),
                new SoccerPlayer("Dice", 183, 83, teamB)
        );
        players.forEach(i -> entityManager.persist(i));
    }

}
