이번 장에서는 Querydsl의 Bulk 연산에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

### Update

이름이 "Roy"인 모든 선수들의 키를 10cm 증가시키는 방법이다.
여기서 주의할 점은 업데이트 이후 강제로 flush, clear를 해주었다는 점이다.
Querydsl은 JPQL로 변경될 뿐이다. JPQL의 경우 영속성 컨텍스트의 내용을 무시하고 실행되기 때문에
벌크 연산 이후 DB와의 싱크를 맞추기 위해서는 반드시 영속성 컨텍스트를 비워주어야한다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(35)
    @DisplayName("수정 벌크 연산 테스트")
    void bulkUpdateTest() {
        assertDoesNotThrow(() -> {
            query
                    .update(soccerPlayer)
                    .set(soccerPlayer.height, soccerPlayer.height.add(10))
                    .where(soccerPlayer.name.eq("Roy"))
                    .execute();
        });
        flushAndClear();
        SoccerPlayer roy = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchOne();

        assertNotNull(roy);
        assertEquals("Roy", roy.getName());
        assertEquals(183, roy.getHeight());
    }
}
```

### Delete

무명 선수(인기가 없는 것이 아니라 이름이 진짜로 없는 null인 선수)를 모두 삭제하는 방법은 아래와 같다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(36)
    @DisplayName("삭제 벌크 연산 테스트")
    void bulkDeleteTest() {
        assertDoesNotThrow(() -> {
            query
                    .delete(soccerPlayer)
                    .where(soccerPlayer.name.isNull())
                    .execute();
        });
    }
}
```

### SQL Function 사용하기

SQL Function의 경우 JPQL과 동일하게 Dialect에 등록되어 있는 함수만 사용이 가능하다.

"Roy"라는 문자열을 "HandsomeRoy"로 변경하는 방법은 아래와 같다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(37)
    @DisplayName("SQL Function Replace 테스트")
    void sqlFunctionReplaceTest() {
        String result = query
                .select(Expressions.stringTemplate("FUNCTION('replace', {0}, {1}, {2})",
                        soccerPlayer.name, "Roy", "HandsomeRoy"))
                .from(soccerPlayer)
                .where(soccerPlayer.name.eq("Roy"))
                .fetchFirst();

        assertEquals("HandsomeRoy", result);
    }
}
```

ansi 표준 함수들은 대부분 Querydsl에서 내장하고 있다.
굳이 stringTemplate를 사용하지 않고 아래와 같은 방법으로도 사용이 가능하다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Autowired
    private EntityManager entityManager;
    private JPAQueryFactory query;
    @Test
    @Order(38)
    @DisplayName("소문자 이름 비교 테스트")
    void sqlFunctionLowerTest() {
        SoccerPlayer roy = query
                .selectFrom(soccerPlayer)
                .where(soccerPlayer.name.lower().eq("roy"))
                .fetchFirst();

        assertEquals("Roy", roy.getName());
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