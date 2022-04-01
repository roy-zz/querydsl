이번 장에서는 Querydsl 지원 클래스를 직접 만드는 방법에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

지난 QuerydslRepositorySupport는 Querydsl 3.x 버전을 대상으로 만들어져서 사용에 제약이 많았다.
Spring Data에서 제공하는 QuerydslRepositorySupport의 단점을 보완하는 지원 클래스를 만들어본다.

공통적으로 사용되는 EntityManager와 QueryFactory를 지원한다.
Querydsl 3.x과 다르게 select(), selectFrom()으로 조회를 시작할 수 있다.
페이징 관련 기능이 추상화되어 편하게 사용가능하다.
오버로딩 되어 있는 페이징 메소드를 호출하여 원하는 경우 언제든지 컨텐츠 쿼리와 카운트 쿼리를 분리할 수 있다.

```java
@Repository
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class CustomQuerydslRepositorySupport {

    private final Class domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    public CustomQuerydslRepositorySupport(Class<?> domainClass) {
        notNull(domainClass, "Domain class must not be null");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        notNull(entityManager, "Entity Manager must not be null");

        JpaEntityInformation entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = INSTANCE;
        EntityPath path = resolver.createPath(entityInformation.getJavaType());
        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata()));
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        notNull(entityManager, "Entity Manager must not be null");
        notNull(querydsl, "Querydsl must not be null");
        notNull(queryFactory, "Query Factory must not be null");
    }

    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    protected Querydsl getQuerydsl() {
        return querydsl;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected <T> JPAQuery<T> select(Expression<T> expression) {
        return getQueryFactory().select(expression);
    }

    protected <T> JPAQuery<T> selectFrom(EntityPath<T> from) {
        return getQueryFactory().selectFrom(from);
    }

    protected <T> Page<T> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery> contentQuery) {
        JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();
        return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
    }

    protected <T> Page<T> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery> contentQuery, Function<JPAQueryFactory, JPAQuery> countQuery) {
        JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch();

        JPAQuery countResult = countQuery.apply(getQueryFactory());
        return PageableExecutionUtils.getPage(content, pageable, countResult::fetchCount);
    }

}
```

커스텀되어 있는 Querydsl Support 클래스를 사용하는 방법은 아래와 같다.

```java
@Repository
public class SoccerPlayerSupportedRepository extends CustomQuerydslRepositorySupport {

    public SoccerPlayerSupportedRepository(Class<?> domainClass) {
        super(domainClass);
    }

    public List<SoccerPlayer> selectAll() {
        return select(soccerPlayer)
                .from(soccerPlayer)
                .fetch();
    }

    public List<SoccerPlayer> selectFromAll() {
        return selectFrom(soccerPlayer)
                .fetch();
    }

    public Page<SoccerPlayer> findPageByApplyPage(SoccerPlayerSearchDTO searchDto, Pageable pageable) {
        JPAQuery<SoccerPlayer> query =
                selectFrom(soccerPlayer)
                        .leftJoin(soccerPlayer.team, team)
                        .where(complexConditions(searchDto));

        List<SoccerPlayer> content = getQuerydsl().applyPagination(pageable, query).fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<SoccerPlayer> applyPagination(SoccerPlayerSearchDTO searchDTO, Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery
                .selectFrom(soccerPlayer)
                .leftJoin(soccerPlayer.team, team)
                .where(complexConditions(searchDTO)));
    }

    public Page<SoccerPlayer> applyPaginationV2(SoccerPlayerSearchDTO searchDto, Pageable pageable) {
        return applyPagination(pageable,
                contentQuery -> contentQuery
                        .selectFrom(soccerPlayer)
                        .leftJoin(soccerPlayer.team, team)
                        .where(complexConditions(searchDto)),
                countQuery -> countQuery
                        .selectFrom(soccerPlayer)
                        .leftJoin(soccerPlayer.team, team)
                        .where(complexConditions(searchDto))
        );
    }

    private BooleanExpression[] complexConditions(SoccerPlayerSearchDTO dto) {
        return new BooleanExpression[]{
                playerNameEq(dto.getPlayerName()),
                teamNameEq(dto.getTeamName()),
                heightGt(dto.getHeightGt()),
                weightGt(dto.getWeightGt())
        };
    }

    private BooleanExpression playerNameEq(String playerName) {
        return Objects.nonNull(playerName) ? soccerPlayer.name.eq(playerName) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return Objects.nonNull(teamName) ? soccerPlayer.team.name.eq(teamName) : null;
    }

    private BooleanExpression heightGt(Integer height) {
        return Objects.nonNull(height) ? soccerPlayer.height.gt(height) : null;
    }

    private BooleanExpression weightGt(Integer weight) {
        return Objects.nonNull(weight) ? soccerPlayer.weight.gt(weight) : null;
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