이번 장에서는 Querydsl의 조인에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

### Default Join

첫 번째 파라미터로 조인 대상을 지정한다.
두 번째 파라미터로 별칭(alias)로 사용할 Q 타입을 지정한다.

이번에는 Q 클래스 파일에 기본으로 있는 static 인스턴스를 사용하였다.
만약 같은 테이블에 두번의 조인이 필요하다면 두 개의 별칭(alias)이 필요하게 되고 
new QSoccerPlayer("alias") 와 같이 새로운 별칭을 생성하여 사용해야한다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
}
```

---

### Theta Join

연관 관계가 없는 필드에 조인한다.
from 절에 여러 엔티티를 입력한다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
}
```

조인 조건을 Where절에 적는 경우 Outer Join이 불가능하다.
Outer Join을 하기위해서는 On을 사용해야한다.

---

### On

조인 대상을 필터링하며 연관관계가 없는 엔티티를 외부 조인할 때 사용된다.

**조인 대상을 필터링 하는 용도로 사용**

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(18)
    @DisplayName("조인 ON 테스트")
    void joinOnTest() {
        assertDoesNotThrow(() -> {
            List<Tuple> result = query
                    .select(soccerPlayer, team)
                    .from(soccerPlayer)
                    .leftJoin(soccerPlayer.team, team).on(team.name.eq("TeamA"))
                    .fetch();

            result.forEach(tuple -> System.out.println("tuple = " + tuple));
        });
    }
}
```

출력 결과는 아래와 같다.

```bash
tuple = [SoccerPlayer(id=1, name=Roy, height=173, weight=73), Team(id=2, name=TeamA)]
tuple = [SoccerPlayer(id=3, name=Perry, height=175, weight=75), Team(id=2, name=TeamA)]
tuple = [SoccerPlayer(id=4, name=Sally, height=160, weight=60), null]
tuple = [SoccerPlayer(id=6, name=Dice, height=183, weight=83), null]
```

Outer Join을 사용할 때 필터링을 사용하려면 On을 사용하면 된다.
Inner Join의 경우 On을 사용하는 것과 Where을 사용하는 것이 동일하게 작동한다.

**조인 조건을 주기위한 용도로 사용**

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
}
```

출력 결과는 아래와 같다

```bash
tuple = [SoccerPlayer(id=1, name=Roy, height=173, weight=73), null]
tuple = [SoccerPlayer(id=3, name=Perry, height=175, weight=75), null]
tuple = [SoccerPlayer(id=4, name=Sally, height=160, weight=60), null]
tuple = [SoccerPlayer(id=6, name=Dice, height=183, weight=83), null]
tuple = [SoccerPlayer(id=7, name=TeamA, height=0, weight=0), Team(id=2, name=TeamA)]
```

---

### Fetch Join

페치 조인은 지금까지 공부해왔던 것과 같이 DB에서 제공하는 기능이 아니다.
연관된 Entity를 한 번에 조회하는 JPA의 기능이다.

페치 조인을 사용하지 않는 경우 조회된 SoccerPlayer의 Team은 초기화 되지 않은 것을 알 수 있다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
}
```

페치 조인을 사용하는 경우 일반 조인 뒤에 fetchJoin()만 추가하면 된다.
페치 조인을 사용해서 조회하는 경우 SoccerPlayer의 Team이 초기화 되어 있는 것을 확인할 수 있다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
}
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