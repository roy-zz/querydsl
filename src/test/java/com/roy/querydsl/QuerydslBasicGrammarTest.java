package com.roy.querydsl;


import com.querydsl.jpa.impl.JPAQueryFactory;
import com.roy.querydsl.domain.QSoccerPlayer;
import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.domain.Team;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static com.roy.querydsl.domain.QSoccerPlayer.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.MethodOrderer.*;

@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {

    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    private final QSoccerPlayer qSoccerPlayer = soccerPlayer;

    @BeforeEach
    void before() {
        query = new JPAQueryFactory(entityManager);
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

    @Test
    @Order(1)
    @DisplayName("JPQL을 사용한 파라미터 바인딩 테스트")
    void parameterBindingUsingJpqlTest() {
        String query = "SELECT SP FROM SoccerPlayer SP WHERE SP.name = :name";
        SoccerPlayer storedPlayer = entityManager.createQuery(query, SoccerPlayer.class)
                .setParameter("name", "Roy")
                .getSingleResult();

        assertEquals("Roy", storedPlayer.getName());
    }

    @Test
    @Order(2)
    @DisplayName("Querydsl을 사용한 파라미터 바인딩 테스트")
    void parameterBindingUsingQuerydsl() {
        SoccerPlayer storedPlayer = query
                .selectFrom(qSoccerPlayer)
                .where(qSoccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertNotNull(storedPlayer);
        assertEquals("Roy", storedPlayer.getName());
    }

    @Test
    @Order(3)
    @DisplayName("검색 조건 And Chaining Test")
    void searchConditionAndChainingTest() {
        assertDoesNotThrow(() -> {
            SoccerPlayer storedPlayer = query
                    .selectFrom(qSoccerPlayer)
                    .where(qSoccerPlayer.name.eq("Roy")                      // name == "Roy"
                            .and(qSoccerPlayer.name.ne("Perry"))             // name != "Perry"
                            .and(qSoccerPlayer.name.eq("Perry").not())       // name != "Perry"
                            .and(qSoccerPlayer.name.isNotNull())                   // name IS NOT NULL
                            .and(qSoccerPlayer.name.in("Roy", "Perry"))     // name IN ("Roy", "Perry")
                            .and(qSoccerPlayer.name.notIn("Roy", "Perry"))  // name NOT IN ("Roy", "Perry")
                            .and(qSoccerPlayer.height.goe(180))              // height >= 180 (Greater Or Equal)
                            .and(qSoccerPlayer.height.gt(180))               // height > 180 (Greater Than)
                            .and(qSoccerPlayer.height.loe(190))              // height <= 190 (Less Or Equal)
                            .and(qSoccerPlayer.height.lt(190))               // height < 190 (Less Than)
                            .and(qSoccerPlayer.name.like("Ro%"))               // Like Ro%
                            .and(qSoccerPlayer.name.contains("Roy"))               // Like %Roy%
                            .and(qSoccerPlayer.name.startsWith("Ro")))             // Like Ro%
                    .fetchOne();
        });
    }

    @Test
    @Order(4)
    @DisplayName("검색 조건 And Parameter Test")
    void searchConditionAndParameterTest() {
        assertDoesNotThrow(() -> {
            SoccerPlayer storedPlayer = query
                    .selectFrom(qSoccerPlayer)
                    .where(qSoccerPlayer.name.eq("Roy"),
                            qSoccerPlayer.name.ne("Perry"),
                            qSoccerPlayer.name.eq("Perry").not(),
                            qSoccerPlayer.name.isNotNull(),
                            qSoccerPlayer.name.in("Roy", "Perry"),
                            qSoccerPlayer.name.notIn("Roy", "Perry"),
                            qSoccerPlayer.height.goe(180),
                            qSoccerPlayer.height.gt(180),
                            qSoccerPlayer.height.loe(190),
                            qSoccerPlayer.height.lt(190),
                            qSoccerPlayer.name.like("Ro%"),
                            qSoccerPlayer.name.contains("Roy"),
                            qSoccerPlayer.name.startsWith("Ro"))
                    .fetchOne();
        });
    }

}
