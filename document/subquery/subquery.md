이번 장에서는 Querydsl의 서브 쿼리에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

JPA에서 서브 쿼리를 사용하기 위해서는 **com.querydsl.jpa.JPAExpressions**를 사용해야한다.

EQ를 사용하여 키가 가장 큰 선수를 조회한다.
서브 쿼리에 사용될 선수(alias)가 필요하기 때문에 새로운 Q 객체를 생성해야 한다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(22)
    @DisplayName("서브 쿼리 EQ")
    void subQueryEqTest() {
        QSoccerPlayer subQPlayer = new QSoccerPlayer("subQPlayer");
        SoccerPlayer tallestPlayer = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.height.eq(
                        JPAExpressions
                                .select(subQPlayer.height.max())
                                .from(subQPlayer)))
                .fetchOne();

        assertNotNull(tallestPlayer);
        assertEquals("Dice", tallestPlayer.getName());
    }
}
```

---

키가 평균보다 큰 선수를 뽑는다.
이전 Group에서 사용해 본 AVG()를 사용하여 평균을 구하고 서브 쿼리로 조회된 선수들의 키가 모두 평균을 넘는지 확인해본다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(23)
    @DisplayName("서브 쿼리 GOE")
    void subQueryGoeTest() {
        QSoccerPlayer subQPlayer = new QSoccerPlayer("subQPlayer");
        Double averageHeight = query
                .select(soccerPlayer.height.avg())
                .from(soccerPlayer)
                .fetchOne();

        List<SoccerPlayer> tallerThanAvgPlayers = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.height.goe(
                        JPAExpressions
                                .select(subQPlayer.height.avg())
                                .from(subQPlayer)))
                .fetch();

        assertTrue(tallerThanAvgPlayers.size() > 0);
        tallerThanAvgPlayers.forEach(player -> {
            assertTrue(player.getHeight() > averageHeight);
        });
    }
}
```

---

IN 절에도 서브 쿼리가 사용 가능하다.
키가 평균을 넘는 선수들의 이름을 IN을 사용하여 조회하였다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
                        JPAExpressions
                                .select(subQPlayer.name)
                                .from(subQPlayer)
                                .where(soccerPlayer.height.gt(averageHeight))
                )).fetch();

        minMaxPlayers.forEach(player -> {
            assertTrue(player.getHeight() > averageHeight);
        });
    }
}
```

---

SELECT 절에도 서브 쿼리의 사용이 가능하다.
선수의 이름과 키, 그리고 모든 선수의 평균 키를 뽑는 방법이다.
이번 방식에는 JPAExpressions를 static import하여 조금이나마 코드를 깔끔하게 변경하였다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
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
}
```

출력 결과는 아래와 같다.

```bash
Roy의 키는 173입니다. 모든 선수의 평균 키는 172.75입니다.
Perry의 키는 175입니다. 모든 선수의 평균 키는 172.75입니다.
Sally의 키는 160입니다. 모든 선수의 평균 키는 172.75입니다.
Dice의 키는 183입니다. 모든 선수의 평균 키는 172.75입니다.
```

Querydsl의 Java 코드로 JPQL을 생성해주는 라이브러리다.
JPQL에서 From절에 서브 쿼리의 사용이 불가능하기 때문에 Querydsl에서도 당연히 사용이 불가능하다.

From절에 서브 쿼리가 필요한 경우 Join으로 해결 가능한지 검토해본다.
만약 불가능하다면 쿼리를 분리하여 실행하는 방법을 생각해본다.
모든 방법이 실패하고 From절에 서브 쿼리를 사용해야만 한다면 native 쿼리를 사용한다.

---

**참고한 강의:**

- https://www.inflearn.com/course/Querydsl-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%8D%B0%EC%9D%B4%ED%84%B0-JPA-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-API%EA%B0%9C%EB%B0%9C-%EC%84%B1%EB%8A%A5%EC%B5%9C%EC%A0%81%ED%99%94
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-%ED%99%9C%EC%9A%A9-1
- https://www.inflearn.com/course/ORM-JPA-Basic

**JPA 공식 문서:** https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference

**위키백과:** https://ko.wikipedia.org/wiki/%EC%9E%90%EB%B0%94_%ED%8D%BC%EC%8B%9C%EC%8A%A4%ED%84%B4%EC%8A%A4_API