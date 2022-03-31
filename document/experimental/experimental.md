이번 장에서는 Spring Data JPA가 제공하는 Querydsl의 기능에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

제목이 Experimental Function인 이유는 기능의 제약이 커서 실무에서 사용하기에는 부족하기 때문이다.
물론 필자의 경험은 아니고 필자가 참고한 강의의 강사인 갓영한님의 말씀이다.

### QuerydslPredicateExecutor

리포지토리에서 Querydsl의 Predicate(검색 조건)을 받아서 별도의 코드 작성없이 바로 조회하는 기능이다.
[공식문서 (링크)](https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/ #core.extensions.querydsl)

기존 리포지토리가 QuerydslPredicateExecutor을 상속받도록 수정한다.

```java
public interface SoccerPlayerRepository extends
        JpaRepository<SoccerPlayer, Long>,
        SoccerPlayerQueryRepository,
        SoccerPlayerDslRepository,
        QuerydslPredicateExecutor<SoccerPlayer> {
}
```

QuerydslPredicateExecutor 인터페이스를 살펴보면 메서드들이 Predicate를 입력받는 것을 확인할 수 있다.

```java
public interface QuerydslPredicateExecutor<T> {
    Optional<T> findOne(Predicate predicate);
    Iterable<T> findAll(Predicate predicate);
    // 이하 생략
}
```

아래처럼 리포지토리 메서드를 호출하는 쪽에서 검색조건을 완성하여 넘기면 된다.

```java
@Transactional
@SpringBootTest
@TestMethodOrder(value = OrderAnnotation.class)
public class QuerydslBasicGrammarTest {
    @Test
    @Order(39)
    @DisplayName("QuerydslPredicateExecutor 테스트")
    void querydslPredicateExecutorTest() {
        Iterable<SoccerPlayer> result = soccerPlayerRepository.findAll(
                soccerPlayer.height.gt(170)
                        .and(soccerPlayer.weight.lt(70))
        );
    }
}
```

보이는 것처럼 리포지토리를 사용하는 클라이언트 입장에서 Querydsl의 구현체에 의존해야한다.
묵시적 조인은 가능하지만 Outer Join이 불가능하다.

---

### Querydsl Web

컨트롤러에서 입력받은 파라미터가 Querydsl의 검색조건이 되는 기능이다.
[공식문서 (링크)](https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe)

아래와 같은 방식으로 사용하면 클라이언트가 요청한 파라미터가 알아서 Querydsl의 Predicate로 변경된다.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/soccer-player")
public class SoccerPlayerController {
    private final SoccerPlayerRepository soccerPlayerRepository;

    @GetMapping("")
    public Iterable test(@QuerydslPredicate(root = SoccerPlayer.class) Predicate predicate,
                        Pageable pageable, @RequestParam MultiValueMap<String, String> params) {
        return soccerPlayerRepository.findAll(predicate, pageable);
    }
}
```

Querydsl의 구현체가 서비스를 넘어 클라이언트까지 넘어오게 된다.
파라미터가 Predicate로 변경되는 조건을 변경하는 경우 복잡하다.

---

### QuerydslRepositorySupport

Querydsl에서 Data JPA의 Pageable을 사용하는 경우 직접 offset과 limit를 입력해주어야 했다.
QuerydslRepositorySupport를 상속받는 경우 applyPagination을 사용하여 간편하게 페이징 처리가 가능하다.

사용법은 아래와 같다.

```java
public class SoccerPlayerDslRepositoryImpl extends QuerydslRepositorySupport
        implements SoccerPlayerDslRepository {

    private final JPAQueryFactory query;

    public SoccerPlayerDslRepositoryImpl(EntityManager entityManager) {
        super(SoccerPlayer.class);
        this.query = new JPAQueryFactory(entityManager);
    }
    public Page<SoccerPlayerTeamDTO> searchWithRepositorySupport(SoccerPlayerSearchDTO dto, Pageable pageable) {
        JPQLQuery<SoccerPlayerTeamDTO> searchQuery = from(soccerPlayer)
                .leftJoin(soccerPlayer.team, team)
                .where(
                        playerNameEq(dto.getPlayerName()),
                        teamNameEq(dto.getTeamName()),
                        heightGt(dto.getHeightGt()),
                        weightGt(dto.getWeightGt()))
                .select(new QSoccerPlayerTeamDTO(
                        soccerPlayer.id, soccerPlayer.name,
                        soccerPlayer.team.id, soccerPlayer.team.name));

        JPQLQuery<SoccerPlayerTeamDTO> jpqlQuery = getQuerydsl().applyPagination(pageable, searchQuery);

        QueryResults<SoccerPlayerTeamDTO> result = jpqlQuery.fetchResults();
        return new PageImpl(result.getResults(), pageable, result.getTotal());
    }
}
```

Querydsl 3.* 버전을 대상으로 만들어졌기 때문에 select절로 시작하는 것이 아니라 from절로 시작한다.
QueryFactory를 제공하지 않으며 Spring Data의 Sort 기능이 정상 동작하지 않는다.

---

**참고한 강의:**

- https://www.inflearn.com/course/Querydsl-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%8D%B0%EC%9D%B4%ED%84%B0-JPA-%EC%8B%A4%EC%A0%84
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-API%EA%B0%9C%EB%B0%9C-%EC%84%B1%EB%8A%A5%EC%B5%9C%EC%A0%81%ED%99%94
- https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-JPA-%ED%99%9C%EC%9A%A9-1
- https://www.inflearn.com/course/ORM-JPA-Basic

**JPA 공식 문서:** https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference

**위키백과:** https://ko.wikipedia.org/wiki/%EC%9E%90%EB%B0%94_%ED%8D%BC%EC%8B%9C%EC%8A%A4%ED%84%B4%EC%8A%A4_API