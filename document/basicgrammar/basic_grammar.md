이번 장에서는 Querydsl의 기본 문법에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

두 개의 Entity인 축구선수(SoccerPlayer)와 선수들이 속해있는 팀(Team)을 가지고 예를 들어본다.

![](image/entity-diagram.png)

**SoccerPlayer**

```java
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
```

**Team**

```java
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
```

모든 테스트를 진행하기 이전에 테스트 데이터를 넣기 위해 아래와 같이 @BeforeEach가 붙은 메서드를 만든다.
각각의 테스트가 실행되기 이전에 먼저 실행되어 DB에 데이터를 넣어줄 것이다.
SoccerPlayer과 Team은 연결할 때 cascade = ALL로 하였기 때문에 SoccerPlayer만 저장하여도 팀까지 같이 저장된다.

```java
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
```

### 파라미터 바인딩

JPQL을 사용하여 파라미터를 바인딩하는 경우 문자열이기 때문에 컴파일 시점에 오류를 파악할 수 없다.
실제로 쿼리가 날아가는 시점에 파악이 가능(런타임 오류)하다.

테스트 코드의 경우 JPAQueryFactory가 필드 멤버로 빠져있다.
동시성 문제는 JPAQueryFactory를 생성할 때 사용되는 EntityManager에 의해 발생한다.
하지만 스프링에서 다중 쓰레드 환경에서 EntityManager에 접근하더라도 Thread-Safe하도록 설계를 하였다.
트랜잭션 마다 별도의 영속성 컨텍스트를 생성하기 때문에 동시성 문제는 발생하지 않는다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {

    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;

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
}
```

Querydsl의 경우 오타가 있으면 컴파일 시점에 오류가 발생하기 때문에 컴파일러와 IDE의 지원을 받아 배포 이전에 최대한 많은 오류를 발견할 수 있다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {

    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;

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
    @Order(2)
    @DisplayName("Querydsl을 사용한 파라미터 바인딩 테스트")
    void parameterBindingUsingQuerydsl() {
        QSoccerPlayer qSoccerPlayer = new QSoccerPlayer("qPlayer1");
        SoccerPlayer storedPlayer = query
                .selectFrom(qSoccerPlayer)
                .where(qSoccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertNotNull(storedPlayer);
        assertEquals("Roy", storedPlayer.getName());
    }
}
```

### Q-Type

Q클래스의 인스턴스를 사용하는 방법으로는 2가지가 있다.
아래처럼 별칭을 직접 지정하여 사용할 수 있다.

```java
QSoccerPlayer qSoccerPlayerA = new QSoccerPlayer("A");
QSoccerPlayer qSoccerPlayerB = new QSoccerPlayer("B");
```
Q클래스 파일 내부에 있는 기본 static 인스턴스를 사용할 수 있다.

```java
import static com.roy.querydsl.domain.QSoccerPlayer.*;
QSoccerPlayer qSoccerPlayer = soccerPlayer;
```

각각의 Q클래스 파일에는 기본 static 인스턴스가 존재한다.

```java
/**
 * QSoccerPlayer is a Querydsl query type for SoccerPlayer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSoccerPlayer extends EntityPathBase<SoccerPlayer> {

    private static final long serialVersionUID = 1280744666L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSoccerPlayer soccerPlayer = new QSoccerPlayer("soccerPlayer");
    
    //이하 생략
}
```

일반적으로 Q파일의 static 인스턴스를 사용하고 같은 테이블에 조인을 해야하는 경우에만 생성해서 사용한다.

---

### 검색 조건 쿼리

검색 조건은 and, or로 체이닝할 수 있으며 JPQL에서 제공하는 모든 검색 조건을 제공한다.

아래는 여러 조건을 체이닝한 테스트 코드이다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Test
    @Order(3)
    @DisplayName("검색 조건 And Chaining Test")
    void searchConditionTest() {
        assertDoesNotThrow(() -> {
            SoccerPlayer storedPlayer = query
                    .selectFrom(qSoccerPlayer)
                    .where(qSoccerPlayer.name.eq("Roy")                      // name == "Roy"
                            .and(qSoccerPlayer.name.ne("Perry"))             // name != "Perry"
                            .and(qSoccerPlayer.name.eq("Perry").not())       // name != "Perry"
                            .and(qSoccerPlayer.name.isNotNull())             // name IS NOT NULL
                            .and(qSoccerPlayer.name.in("Roy", "Perry"))      // name IN ("Roy", "Perry")
                            .and(qSoccerPlayer.name.notIn("Roy", "Perry"))   // name NOT IN ("Roy", "Perry")
                            .and(qSoccerPlayer.height.goe(180))              // height >= 180 (Greater Or Equal)
                            .and(qSoccerPlayer.height.gt(180))               // height > 180 (Greater Than)
                            .and(qSoccerPlayer.height.loe(190))              // height <= 190 (Less Or Equal)
                            .and(qSoccerPlayer.height.lt(190))               // height < 190 (Less Than)
                            .and(qSoccerPlayer.name.like("Ro%"))             // Like Ro%
                            .and(qSoccerPlayer.name.contains("Roy"))         // Like %Roy%
                            .and(qSoccerPlayer.name.startsWith("Ro")))       // Like Ro%
                    .fetchOne();
        });
    }
}
```

이 때 발생한 쿼리는 아래와 같다.

```sql
select
    soccerPlayer 
from
    SoccerPlayer soccerPlayer 
where
    soccerPlayer.name = ?1 
    and soccerPlayer.name <> ?2 
    and not soccerPlayer.name = ?3 
    and soccerPlayer.name is not null 
    and soccerPlayer.name in ?4 
    and soccerPlayer.name not in ?5 
    and soccerPlayer.height >= ?6 
    and soccerPlayer.height > ?7 
    and soccerPlayer.height <= ?8 
    and soccerPlayer.height < ?9 
    and soccerPlayer.name like ?10 escape '!' 
    and soccerPlayer.name like ?11 escape '!' 
    and soccerPlayer.name like ?12 escape '!'
```

또한 AND 조건을 파라미터로 처리 가능하다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
```

이렇게 작성하는 경우 null 값은 무시되기 때문에 동적 쿼리를 생성에 유리하다.

---

### 결과 조회

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

---

### 정렬

Q 인스턴스의 필드에 desc(), asc(), nullsLast(), nullsFirst()를 사용하여 정렬을 사용할 수 있다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

---

### 페이징

#### 조회된 내용만 필요한 경우

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

#### 페이징에 대한 정보도 필요한 경우

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
```

이렇게 조회하는 경우 카운트 쿼리도 같이 발생한다.
JOIN이 많이 걸려있는 경우 성능상의 문제가 발생할 수 있다.
성능 테스트를 진행해보고 카운트 쿼리에서 많은 시간이 소요된다면 카운트 쿼리를 직접 작성해서 성능을 개선해야한다.

---

### 집합

#### 집합 함수를 사용하는 경우

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

#### GroupBy를 사용하는 경우

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

---

### Case

#### 단순한 조건의 테스트

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

#### 복잡한 조건의 테스트

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

#### OrderBy에 Case 사용

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

출력 결과는 아래와 같다.

```bash
name: Dice, height: 183, rank: 4
name: Roy, height: 173, rank: 3
name: Perry, height: 175, rank: 3
name: Sally, height: 160, rank: 1
```

---

### Constant & Character

상수가 필요한 경우에는 Expressions.constant(${Char})을 사용한다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

출력은 아래와 같다.
```bash
result: [Roy, 선수]
```

하지만 발생한 쿼리에는 상수에 대한 언급이 없다.
Querydsl에서 자체적으로 최적화를 하였기 때문이다.

```sql
select
    soccerplay0_.name as col_0_0_ 
from
    soccer_player soccerplay0_ limit ?
```

#### 문자 더하기 (Concat)

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Test
    @Order(15)
    @DisplayName("문자 더하기 (Concat)")
    void concatTest() {
        List<String> result = query
                .select(soccerPlayer.name
                        .concat("의 팀은 ")
                        .concat(soccerPlayer.team.name)
                        .concat("입니다."))
                .from(soccerPlayer)
                .fetch();
        result.forEach(System.out::println);
    }
}
```

출력 결과는 아래와 같다.

```bash
Roy의 팀은 TeamA입니다.
Perry의 팀은 TeamA입니다.
Sally의 팀은 TeamB입니다.
Dice의 팀은 TeamB입니다.
```

사실 아래와 같이 키 정보를 출력하려 하였으나 예상치 못한 결과가 출력되었고 원인을 파악하지 못하였다.
원인은 추후에 찾고 **문자열이 아닌 타입을 Concat할 때는 .stringValue()**를 붙여주어야 한다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
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
}
```

실패한 출력 결과는 아래와 같다.

```bash
Roy의 키는 1입니다.
Perry의 키는 1입니다.
Sally의 키는 1입니다.
Dice의 키는 1입니다.
```

---

**참고한 강의:**

- https://www.inflearn.com/course/Querydsl-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%8D%B0%EC%9D%B4%ED%84%B0-JPA-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-API%EA%B0%9C%EB%B0%9C-%EC%84%B1%EB%8A%A5%EC%B5%9C%EC%A0%81%ED%99%94
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-%ED%99%9C%EC%9A%A9-1
- https://www.inflearn.com/course/ORM-JPA-Basic

**JPA 공식 문서:** https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference

**위키백과:** https://ko.wikipedia.org/wiki/%EC%9E%90%EB%B0%94_%ED%8D%BC%EC%8B%9C%EC%8A%A4%ED%84%B4%EC%8A%A4_API