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






---

**참고한 강의:**

- https://www.inflearn.com/course/Querydsl-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%8D%B0%EC%9D%B4%ED%84%B0-JPA-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-API%EA%B0%9C%EB%B0%9C-%EC%84%B1%EB%8A%A5%EC%B5%9C%EC%A0%81%ED%99%94
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-%ED%99%9C%EC%9A%A9-1
- https://www.inflearn.com/course/ORM-JPA-Basic

**JPA 공식 문서:** https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference

**위키백과:** https://ko.wikipedia.org/wiki/%EC%9E%90%EB%B0%94_%ED%8D%BC%EC%8B%9C%EC%8A%A4%ED%84%B4%EC%8A%A4_API