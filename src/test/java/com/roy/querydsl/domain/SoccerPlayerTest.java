package com.roy.querydsl.domain;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
class SoccerPlayerTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Querydsl 정상 작동 테스트")
    void querydslSettingTest() {
        SoccerPlayer newSoccerPlayer = new SoccerPlayer();
        entityManager.persist(newSoccerPlayer);

        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        
        QSoccerPlayer soccerPlayer = QSoccerPlayer.soccerPlayer;

        SoccerPlayer storedPlayer = query
                .selectFrom(soccerPlayer)
                .fetchOne();

        assertEquals(newSoccerPlayer, storedPlayer);
        assertEquals(newSoccerPlayer.getId(), storedPlayer.getId());
    }

}