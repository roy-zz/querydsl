이번 장에서는 Spring Data JPA와 Querydsl을 함께 사용하는 방법에 대해서 알아본다.
글의 하단부에 참고한 강의와 공식문서의 경로를 첨부하였으므로 자세한 사항은 강의나 공식문서에서 확인한다.
모든 코드는 [깃허브 (링크)](https://github.com/roy-zz/querydsl)에 올려두었다.

---

### 리포지토리 구조

Spring Data JPA의 리포지토리는 인터페이스로 이루어져 있으며 개발자가 직접 구현하지 않는다.
하지만 Querydsl을 사용하기 위해서는 조회하기 위한 코드를 직접 작성해야한다. 
Data JPA 리포지토리에서 Querydsl을 사용하는 것은 불가능하며 아래와 같이 리포지토리 구조를 잡아야한다.

![](image/repository-structure.png)

**SoccerPlayerQueryRepository**: 화면에 특화된 복잡한 Select 쿼리를 작성하는 구현체
**JpaRepository**: Spring Data JPA에서 기본 CRUD를 제공하는 인터페이스
**SoccerPlayerDslRepository**: Querydsl용 역할을 명시할 인터페이스
**SoccerPlayerDslRepositoryImpl**: 인터페이스에 명시되어 있는 기능을 구현할 구현체

이러한 구조로 리포지토리가 구성되어 있다면 클라이언트 입장에서는 SoccerPlayerRepository만 사용하면 되고
실제로 내가 사용하는 메서드의 구현체는 누구이며 어떻게 구현되어 있는지 알 필요가 없어진다.

패키지 구조는 아래의 이미지와 같다.

![](image/package-structure.png)

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