package com.roy.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.roy.querydsl.domain.QSoccerPlayer;
import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.domain.Team;
import com.roy.querydsl.dto.QSoccerPlayerDTO;
import com.roy.querydsl.dto.SoccerPlayerDTO;
import com.roy.querydsl.dto.StrangeSoccerPlayerDTO;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.roy.querydsl.domain.QSoccerPlayer.soccerPlayer;
import static com.roy.querydsl.domain.QTeam.team;
import static java.util.Objects.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.MethodOrderer.*;
import static org.junit.platform.commons.util.StringUtils.*;

@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {

    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

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
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
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
                    .selectFrom(soccerPlayer)
                    .where(soccerPlayer.name.eq("Roy")                      // name == "Roy"
                            .and(soccerPlayer.name.ne("Perry"))             // name != "Perry"
                            .and(soccerPlayer.name.eq("Perry").not())       // name != "Perry"
                            .and(soccerPlayer.name.isNotNull())                   // name IS NOT NULL
                            .and(soccerPlayer.name.in("Roy", "Perry"))     // name IN ("Roy", "Perry")
                            .and(soccerPlayer.name.notIn("Roy", "Perry"))  // name NOT IN ("Roy", "Perry")
                            .and(soccerPlayer.height.goe(180))              // height >= 180 (Greater Or Equal)
                            .and(soccerPlayer.height.gt(180))               // height > 180 (Greater Than)
                            .and(soccerPlayer.height.loe(190))              // height <= 190 (Less Or Equal)
                            .and(soccerPlayer.height.lt(190))               // height < 190 (Less Than)
                            .and(soccerPlayer.name.like("Ro%"))               // Like Ro%
                            .and(soccerPlayer.name.contains("Roy"))               // Like %Roy%
                            .and(soccerPlayer.name.startsWith("Ro")))             // Like Ro%
                    .fetchOne();
        });
    }

    @Test
    @Order(4)
    @DisplayName("검색 조건 And Parameter Test")
    void searchConditionAndParameterTest() {
        assertDoesNotThrow(() -> {
            SoccerPlayer storedPlayer = query
                    .selectFrom(soccerPlayer)
                    .where(soccerPlayer.name.eq("Roy"),
                            soccerPlayer.name.ne("Perry"),
                            soccerPlayer.name.eq("Perry").not(),
                            soccerPlayer.name.isNotNull(),
                            soccerPlayer.name.in("Roy", "Perry"),
                            soccerPlayer.name.notIn("Roy", "Perry"),
                            soccerPlayer.height.goe(180),
                            soccerPlayer.height.gt(180),
                            soccerPlayer.height.loe(190),
                            soccerPlayer.height.lt(190),
                            soccerPlayer.name.like("Ro%"),
                            soccerPlayer.name.contains("Roy"),
                            soccerPlayer.name.startsWith("Ro"))
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
                    .selectFrom(soccerPlayer)
                    .fetch();

            // FetchOne
            // 단 건 조회. 결과가 없으면 null, 결과가 둘 이상이면 com.querydsl.core.NonUniqueResultException
            SoccerPlayer resultUsingFetchOne = query
                    .selectFrom(soccerPlayer)
                    .where(soccerPlayer.name.eq("Roy"))
                    .fetchOne();

            // FetchFirst
            // limit(1).fetchOne();
            // 단 건을 조회해야하는데 여러 개의 결과가 나올 수 있을 때 사용.
            SoccerPlayer resultUsingFetchFirst = query
                    .selectFrom(soccerPlayer)
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

    @Test
    @Order(9)
    @DisplayName("Group 함수 사용 테스트")
    void groupUsingFunctionTest() {
        // heights = {173, 175, 160, 183}
        // weights = {73, 75, 60, 83}
        List<Tuple> tuples = query
                .select(soccerPlayer.count(),
                        soccerPlayer.height.sum(),
                        soccerPlayer.height.avg(),
                        soccerPlayer.height.max(),
                        soccerPlayer.height.min(),
                        soccerPlayer.weight.sum(),
                        soccerPlayer.weight.avg(),
                        soccerPlayer.weight.max(),
                        soccerPlayer.weight.min()
                ).from(soccerPlayer)
                .fetch();

        Tuple tuple = tuples.get(0);
        assertEquals(4, tuple.get(soccerPlayer.count()));
        assertEquals(691, tuple.get(soccerPlayer.height.sum()));
        assertEquals(172.75, tuple.get(soccerPlayer.height.avg()));
        assertEquals(183, tuple.get(soccerPlayer.height.max()));
        assertEquals(160, tuple.get(soccerPlayer.height.min()));
        assertEquals(291, tuple.get(soccerPlayer.weight.sum()));
        assertEquals(72.75, tuple.get(soccerPlayer.weight.avg()));
        assertEquals(83, tuple.get(soccerPlayer.weight.max()));
        assertEquals(60, tuple.get(soccerPlayer.weight.min()));
    }

    @Test
    @Order(10)
    @DisplayName("GroupBy 사용 테스트")
    void groupUsingGroupByTest() {
        // TeamA heights = {173, 175}, TeamB heights = {160, 183}
        // TeamA weights = {73, 75}, TeamB weights = {60, 83}
        List<Tuple> tuples = query
                .select(team.name,
                        soccerPlayer.height.avg(),
                        soccerPlayer.weight.avg()
                ).from(soccerPlayer)
                .join(soccerPlayer.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = tuples.get(0);
        Tuple teamB = tuples.get(1);

        assertEquals("TeamA", teamA.get(team.name));
        assertEquals((float) (173 + 175) / 2, teamA.get(soccerPlayer.height.avg()));
        assertEquals((float) (73 + 75) / 2, teamA.get(soccerPlayer.weight.avg()));

        assertEquals("TeamB", teamB.get(team.name));
        assertEquals((float) (160 + 183) / 2, teamB.get(soccerPlayer.height.avg()));
        assertEquals((float) (60 + 83) / 2, teamB.get(soccerPlayer.weight.avg()));
    }

    @Test
    @Order(11)
    @DisplayName("단순한 조건 Case 테스트")
    void simpleCaseTest() {
        List<String> result = query
                .select(soccerPlayer.team.name
                        .when("TeamA").then("TeamA에 속한 선수")
                        .when("TeamB").then("TeamB에 속한 선수")
                        .otherwise("다른 팀에 속한 선수"))
                .from(soccerPlayer)
                .fetch();
        result.forEach(i -> System.out.println("result = " + i));
    }

    @Test
    @Order(12)
    @DisplayName("복잡한 조건 Case 테스트")
    void complexCaseTest() {
        List<String> result = query
                .select(new CaseBuilder()
                        .when(soccerPlayer.height.between(0, 160)).then("0cm ~ 160cm")
                        .when(soccerPlayer.height.between(160, 170)).then("160cm ~ 170cm")
                        .when(soccerPlayer.height.between(170, 180)).then("170cm ~ 180cm")
                        .when(soccerPlayer.height.between(180, 190)).then("180cm ~ 190cm")
                        .otherwise("진격의 거인"))
                .from(soccerPlayer)
                .fetch();
    }

    @Test
    @Order(13)
    @DisplayName("OrderBy 내부에 Case 사용 테스트")
    void caseInOrderByTest() {
        // 180cm가 넘는 선수 -> 170cm ~ 180cm -> 160cm ~ 170cm -> 0cm ~ 160cm 순으로 출력
        NumberExpression<Integer> rank = new CaseBuilder()
                .when(soccerPlayer.height.between(0, 160)).then(1)
                .when(soccerPlayer.height.between(160, 170)).then(2)
                .when(soccerPlayer.height.between(170, 180)).then(3)
                .otherwise(4);

        List<Tuple> tuples = query
                .select(soccerPlayer.name,
                        soccerPlayer.height,
                        rank)
                .from(soccerPlayer)
                .orderBy(rank.desc())
                .fetch();

        tuples.forEach(tuple -> System.out.printf("name: %s, height: %s, rank: %s%n",
                tuple.get(soccerPlayer.name),
                tuple.get(soccerPlayer.height),
                tuple.get(rank)));
    }

    @Test
    @Order(14)
    @DisplayName("상수 최적화 테스트")
    void optimizingConstantTest() {
        Tuple result = query
                .select(
                        soccerPlayer.name,
                        Expressions.constant("선수"))
                .from(soccerPlayer)
                .fetchFirst();

        System.out.println("result: " + result);
    }

    @Test
    @Order(15)
    @DisplayName("문자 더하기 (Concat)")
    void concatTest() {
        List<String> result = query
                .select(soccerPlayer.name
                        .concat("의 키는 ")
                        .concat(soccerPlayer.height.stringValue())
                        .concat("입니다."))
                .from(soccerPlayer)
                .fetch();
        result.forEach(System.out::println);
    }

    @Test
    @Order(16)
    @DisplayName("기본 조인 테스트")
    void defaultJoinTest() {
        // Inner Join
        List<SoccerPlayer> storedPlayers = query
                .selectFrom(soccerPlayer)
                .join(soccerPlayer.team, team)
                .where(team.name.eq("TeamA"))
                .fetch();

        storedPlayers.forEach(player ->
                assertTrue(player.getName().equals("Roy")
                        || player.getName().equals("Perry"))
        );

        assertDoesNotThrow(() -> {
            // Inner Join
            query
                    .selectFrom(soccerPlayer)
                    .innerJoin(soccerPlayer.team, team)
                    .fetch();

            // Left Outer Join
            query
                    .selectFrom(soccerPlayer)
                    .leftJoin(soccerPlayer.team, team)
                    .fetch();

            // Right Outer Join
            query
                    .selectFrom(soccerPlayer)
                    .rightJoin(soccerPlayer.team, team)
                    .fetch();
        });
    }

    @Test
    @Order(17)
    @DisplayName("세타 조인 테스트")
    void thetaJoinTest() {
        entityManager.persist(new SoccerPlayer("TeamA"));

        List<SoccerPlayer> storedPlayers = query
                .select(soccerPlayer)
                .from(soccerPlayer, team)
                .where(soccerPlayer.name.eq(team.name))
                .fetch();

        storedPlayers.forEach(player -> assertEquals(player.getName(), "TeamA"));
    }

    @Test
    @Order(18)
    @DisplayName("조인 ON 필터링 테스트")
    void joinOnFilteringTest() {
        assertDoesNotThrow(() -> {
            List<Tuple> result = query
                    .select(soccerPlayer, team)
                    .from(soccerPlayer)
                    .leftJoin(soccerPlayer.team, team).on(team.name.eq("TeamA"))
                    .fetch();

            result.forEach(tuple -> System.out.println("tuple = " + tuple));
        });
    }

    @Test
    @Order(19)
    @DisplayName("조인 ON 조건 테스트")
    void joinOnConditionTest() {
        entityManager.persist(new SoccerPlayer("TeamA"));
        assertDoesNotThrow(() -> {
            List<Tuple> result = query
                    .select(soccerPlayer, team)
                    .from(soccerPlayer)
                    .leftJoin(team).on(soccerPlayer.name.eq(team.name))
                    .fetch();
            result.forEach(tuple -> System.out.println("tuple = " + tuple));
        });
    }

    @Test
    @Order(20)
    @DisplayName("페치 조인 미적용 테스트")
    void notAppliedFetchJoinTest() {
        flushAndClear();
        SoccerPlayer storedPlayer = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertNotNull(storedPlayer.getTeam());
        assertFalse(Hibernate.isInitialized(storedPlayer.getTeam()));
    }

    @Test
    @Order(21)
    @DisplayName("페치 조인 적용 테스트")
    void appliedFetchJoinTest() {
        flushAndClear();
        SoccerPlayer storedPlayer = query
                .selectFrom(soccerPlayer)
                .join(soccerPlayer.team, team).fetchJoin()
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertNotNull(storedPlayer.getTeam());
        assertTrue(Hibernate.isInitialized(storedPlayer.getTeam()));
    }

    @Test
    @Order(22)
    @DisplayName("서브 쿼리 EQ 테스트")
    void subQueryEqTest() {
        QSoccerPlayer subQPlayer = new QSoccerPlayer("subQPlayer");
        SoccerPlayer tallestPlayer = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.height.eq(
                        select(subQPlayer.height.max())
                                .from(subQPlayer)))
                .fetchOne();

        assertNotNull(tallestPlayer);
        assertEquals("Dice", tallestPlayer.getName());
    }

    @Test
    @Order(23)
    @DisplayName("서브 쿼리 GOE 테스트")
    void subQueryGoeTest() {
        QSoccerPlayer subQPlayer = new QSoccerPlayer("subQPlayer");
        Double averageHeight = query
                .select(soccerPlayer.height.avg())
                .from(soccerPlayer)
                .fetchOne();

        List<SoccerPlayer> tallerThanAvgPlayers = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.height.goe(
                        select(subQPlayer.height.avg())
                                .from(subQPlayer)))
                .fetch();

        assertTrue(tallerThanAvgPlayers.size() > 0);
        tallerThanAvgPlayers.forEach(player -> {
            assertTrue(player.getHeight() > averageHeight);
        });
    }

    @Test
    @Order(24)
    @DisplayName("서브 쿼리 IN 테스트")
    void subQueryInTest() {
        QSoccerPlayer subQPlayer = new QSoccerPlayer("subQPlayer");
        Double averageHeight = query
                .select(soccerPlayer.height.avg())
                .from(soccerPlayer)
                .fetchOne();

        List<SoccerPlayer> minMaxPlayers = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.name.in(
                        select(subQPlayer.name)
                                .from(subQPlayer)
                                .where(soccerPlayer.height.gt(averageHeight))
                )).fetch();

        minMaxPlayers.forEach(player -> {
            assertTrue(player.getHeight() > averageHeight);
        });
    }

    @Test
    @Order(25)
    @DisplayName("SELECT에 서브 쿼리 사용 테스트")
    void subQueryInSelectTest() {
        QSoccerPlayer subQPlayer = new QSoccerPlayer("subQPlayer");
        List<Tuple> tuples = query
                .select(soccerPlayer.name,
                        soccerPlayer.height,
                        select(subQPlayer.height.avg())
                                .from(subQPlayer))
                .from(soccerPlayer)
                .fetch();

        tuples.forEach(tuple -> {
            String name = tuple.get(soccerPlayer.name);
            Integer height = tuple.get(soccerPlayer.height);
            Double avgHeight = tuple.get(select(subQPlayer.height.avg()).from(subQPlayer));
            System.out.printf("%s의 키는 %s입니다. 모든 선수의 평균 키는 %s입니다.%n", name, height, avgHeight);
        });
    }

    @Test
    @Order(26)
    @DisplayName("Tuple을 사용한 프로젝션 테스트")
    void projectionUsingTupleTest() {
        List<Tuple> tuples = query
                .select(
                        soccerPlayer.name,
                        soccerPlayer.height)
                .from(soccerPlayer)
                .fetch();

        tuples.forEach(tuple -> {
            System.out.println("선수의 이름: " + tuple.get(soccerPlayer.name));
            System.out.println("선수의 키: " + tuple.get(soccerPlayer.height));
        });
    }

    @Test
    @Order(27)
    @DisplayName("순수 JPA DTO 조회 테스트")
    void pureJpaFindDTOTest() {
        assertDoesNotThrow(() -> {
            entityManager.createQuery(
                            "SELECT new com.roy.querydsl.dto.SoccerPlayerDTO(SP.name, SP.height) " +
                                    "FROM SoccerPlayer SP " +
                                    "WHERE SP.name = :name ", SoccerPlayerDTO.class)
                    .setParameter("name", "Roy")
                    .getSingleResult();
        });
    }

    @Test
    @Order(28)
    @DisplayName("DTO 조회 프로퍼티 접근 테스트")
    void createDTOPropertyAccess() {
        SoccerPlayerDTO pureJpaDTO = entityManager.createQuery(
                        "SELECT new com.roy.querydsl.dto.SoccerPlayerDTO(SP.name, SP.height) " +
                                "FROM SoccerPlayer SP " +
                                "WHERE SP.name = :name ", SoccerPlayerDTO.class)
                .setParameter("name", "Roy")
                .getSingleResult();

        SoccerPlayerDTO dslDTO = query
                .select(Projections.bean(SoccerPlayerDTO.class,
                        soccerPlayer.name, soccerPlayer.height))
                .from(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertEquals(dslDTO, pureJpaDTO);
    }

    @Test
    @Order(29)
    @DisplayName("DTO 조회 필드 직접 접근 테스트")
    void createDTOFieldAccess() {
        SoccerPlayerDTO pureJpaDTO = entityManager.createQuery(
                        "SELECT new com.roy.querydsl.dto.SoccerPlayerDTO(SP.name, SP.height) " +
                                "FROM SoccerPlayer SP " +
                                "WHERE SP.name = :name ", SoccerPlayerDTO.class)
                .setParameter("name", "Roy")
                .getSingleResult();

        SoccerPlayerDTO dslDTO = query
                .select(Projections.fields(SoccerPlayerDTO.class,
                        soccerPlayer.name, soccerPlayer.height))
                .from(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertEquals(dslDTO, pureJpaDTO);
    }

    @Test
    @Order(30)
    @DisplayName("DTO 조회 생성자 사용 테스트")
    void createDTOUsingConstructorTest() {
        SoccerPlayerDTO pureJpaDTO = entityManager.createQuery(
                        "SELECT new com.roy.querydsl.dto.SoccerPlayerDTO(SP.name, SP.height) " +
                                "FROM SoccerPlayer SP " +
                                "WHERE SP.name = :name ", SoccerPlayerDTO.class)
                .setParameter("name", "Roy")
                .getSingleResult();

        SoccerPlayerDTO dslDTO = query
                .select(Projections.constructor(SoccerPlayerDTO.class,
                        soccerPlayer.name, soccerPlayer.height))
                .from(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertEquals(dslDTO, pureJpaDTO);
    }

    @Test
    @Order(31)
    @DisplayName("필드명이 다른 DTO 조회 테스트")
    void notEqualFieldNameTest() {
        StrangeSoccerPlayerDTO strangeDto = query
                .select(Projections.fields(StrangeSoccerPlayerDTO.class,
                        soccerPlayer.name.as("whatYourName"),
                        ExpressionUtils.as(
                                soccerPlayer.height, "howTallAreYou")))
                .from(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertNotNull(strangeDto.getWhatYourName());
        assertNotNull(strangeDto.getHowTallAreYou());
        assertEquals("Roy", strangeDto.getWhatYourName());
        assertEquals(173, strangeDto.getHowTallAreYou());
    }

    @Test
    @Order(32)
    @DisplayName("DTO 조회 @QueryProjection 테스트")
    void createDTOQueryProjection() {
        SoccerPlayerDTO pureJpaDTO = entityManager.createQuery(
                        "SELECT new com.roy.querydsl.dto.SoccerPlayerDTO(SP.name, SP.height) " +
                                "FROM SoccerPlayer SP " +
                                "WHERE SP.name = :name ", SoccerPlayerDTO.class)
                .setParameter("name", "Roy")
                .getSingleResult();

        SoccerPlayerDTO dslDTO = query
                .select(new QSoccerPlayerDTO(soccerPlayer.name, soccerPlayer.height))
                .from(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertEquals(dslDTO, pureJpaDTO);
    }

    @Test
    @Order(33)
    @DisplayName("BooleanBuilder를 사용한 동적 쿼리 테스트")
    void dynamicQueryUsingBooleanBuilderTest() {
        String nameInParam = "Roy";
        Integer heightInParam = 170;
        List<SoccerPlayer> storedPlayers = searchPlayerByBooleanBuilder(nameInParam, heightInParam);

        assertEquals(1, storedPlayers.size());
        storedPlayers.forEach(player -> {
            assertEquals("Roy", player.getName());
        });
    }

    private List<SoccerPlayer> searchPlayerByBooleanBuilder(String targetName, Integer targetHeight) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (nonNull(targetName)) {
            booleanBuilder.and(soccerPlayer.name.eq(targetName));
        }
        if (nonNull(targetHeight)) {
            booleanBuilder.and(soccerPlayer.height.gt(targetHeight));
        }
        return query
                .selectFrom(soccerPlayer)
                .where(booleanBuilder)
                .fetch();
    }

    @Test
    @Order(34)
    @DisplayName("Where절의 다중 파라미터를 사용한 동적 쿼리 테스트")
    void dynamicQueryUsingWhereStateTest() {
        String nameInParam = "Roy";
        Integer heightInParam = 170;

        List<SoccerPlayer> storedPlayers = searchPlayerByWhereState(nameInParam, heightInParam);
        assertEquals(1, storedPlayers.size());
        storedPlayers.forEach(player -> {
            assertEquals("Roy", player.getName());
        });
    }

    private List<SoccerPlayer> searchPlayerByWhereState(String targetName, Integer targetHeight) {
        return query
                .selectFrom(soccerPlayer)
                .where(
                        nameEq(targetName),
                        heightGt(targetHeight))
                .fetch();
    }

    private BooleanExpression nameEq(String targetName) {
        return isNotBlank(targetName) ? soccerPlayer.name.eq(targetName) : null;
    }

    private BooleanExpression heightGt(Integer targetHeight) {
        return nonNull(targetHeight) ? soccerPlayer.height.gt(targetHeight) : null;
    }

    private BooleanExpression weightGt(Integer targetWeight) {
        return nonNull(targetWeight) ? soccerPlayer.weight.gt(targetWeight) : null;
    }

}
