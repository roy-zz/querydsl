package com.roy.querydsl;


import com.querydsl.core.QueryResults;
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

    @Test
    @Order(5)
    @DisplayName("결과 조회 테스트")
    void searchResultTest() {
        assertDoesNotThrow(() -> {
            // Fetch
            // List를 조회하고 데이터가 없다면 Empty List를 반환한다.
            List<SoccerPlayer> resultUsingFetch = query
                    .selectFrom(qSoccerPlayer)
                    .fetch();

            // FetchOne
            // 단 건 조회. 결과가 없으면 null, 결과가 둘 이상이면 com.querydsl.core.NonUniqueResultException
            SoccerPlayer resultUsingFetchOne = query
                    .selectFrom(qSoccerPlayer)
                    .where(qSoccerPlayer.name.eq("Roy"))
                    .fetchOne();

            // FetchFirst
            // limit(1).fetchOne();
            // 단 건을 조회해야하는데 여러 개의 결과가 나올 수 있을 때 사용.
            SoccerPlayer resultUsingFetchFirst = query
                    .selectFrom(qSoccerPlayer)
                    .fetchFirst();

            // FetchResults
            // 페이징 정보를 포함, Total Count를 조회하는 쿼리가 추가로 실행된다.
            QueryResults<SoccerPlayer> resultUsingFetchResults = query
                    .selectFrom(soccerPlayer)
                    .fetchResults();

            // FetchCount
            // Count 쿼리로 변경해서 Count 수 조회
            long count = query
                    .selectFrom(soccerPlayer)
                    .fetchCount();
        });
    }

    @Test
    @Order(6)
    @DisplayName("정렬 테스트")
    void sortTest() {
        entityManager.persist(new SoccerPlayer("190H90WPlayer", 190, 90));
        entityManager.persist(new SoccerPlayer("190H85WPlayer", 190, 85));
        entityManager.persist(new SoccerPlayer("NullH100WPlayer", 190, null));
        List<SoccerPlayer> storedPlayers = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.height.goe(188))
                .orderBy(soccerPlayer.height.desc(), soccerPlayer.weight.asc().nullsFirst())
                .fetch();

        assertEquals("NullH100WPlayer", storedPlayers.get(0).getName());
        assertEquals("190H85WPlayer", storedPlayers.get(1).getName());
        assertEquals("190H90WPlayer", storedPlayers.get(2).getName());
    }

    @Test
    @Order(7)
    @DisplayName("페이징 컨텐츠 조회 테스트")
    void pagingOnlyContentTest() {
        List<SoccerPlayer> storedPlayers = query
                .selectFrom(soccerPlayer)
                .orderBy(soccerPlayer.height.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertEquals(2, storedPlayers.size());
    }

    @Test
    @Order(8)
    @DisplayName("페이징 데이터 및 컨텐츠 조회 테스트")
    void pagingDataAndContentTest() {
        QueryResults<SoccerPlayer> pageOfStoredPlayers = query
                .selectFrom(soccerPlayer)
                .orderBy(soccerPlayer.height.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertEquals(4, pageOfStoredPlayers.getTotal());
        assertEquals(2, pageOfStoredPlayers.getLimit());
        assertEquals(1, pageOfStoredPlayers.getOffset());
        assertEquals(2, pageOfStoredPlayers.getResults().size());
    }

}
