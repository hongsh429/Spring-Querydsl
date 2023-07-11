package study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void setUp() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        // given
        // member1을 찾아라
        String qlString = "select m from Member m where m.username = :username";
        Member findMember = em
                .createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        // when

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        /*
        JPQL
            사용자가 이 메서드를 호출 했을 때!, 런타임에서 오류를 알 수 있다.
        */
    }

    @Test
    public void startQuerydsl() throws Exception {
        // given
        /* Muti-thread 환경에서도 무리 없이 동작하도록 설계되어있다. */
        queryFactory = new JPAQueryFactory(em); /* EntityManager를 넘겨주어야 한다. */

        QMember m = new QMember("m"); // 이건 굳이 쓰지 않는다
//        QMember m = member; > QMember 내부에 들어가면 된다. 그럼 static QMember가 존재.

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))    // 파라미터 바인딩 처리
                .fetchOne();

        // when

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        /*
        Querydsl
            컴파일 시점에 오류를 그냥 잡아준다. */
    }

    @Test
    public void startQuerydslCleanStyle() throws Exception {
        // given
        /* Muti-thread 환경에서도 무리 없이 동작하도록 설계되어있다. */
        queryFactory = new JPAQueryFactory(em); /* EntityManager를 넘겨주어야 한다. */

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))    // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        // when

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchParam() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        // when

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchBetween() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 40)))
                .fetch();

        // when

        // then
        assertThat(findMembers.size()).isEqualTo(1);
        assertThat(findMembers.get(0).getUsername()).isEqualTo("member1");
    }

    /* 결과 조회 fetchOne, fetch, fetchFirst, ... */
    @Test
    public void resultFetch() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member).where(member.username.eq("member1"))
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();// == limit(1).fetchOne(); 과 동일

        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .fetchResults();

        /* 아래부터 쿼리가 날라 간다. */
        System.out.println(result.getTotal());
        List<Member> contents = result.getResults();

        long total = queryFactory.selectFrom(member)
                .fetchCount();

        // when

        // then
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     *
     * @throws Exception
     */
    @Test
    public void sort() throws Exception {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member result1 = result.get(0);
        Member result2 = result.get(1);
        Member result3 = result.get(2);
        assertThat(result1.getUsername()).isEqualTo("member5");
        assertThat(result2.getUsername()).isEqualTo("member6");
        assertThat(result3.getUsername()).isNull();

    }

    @Test
    public void paging1() throws Exception {

        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() throws Exception {

        queryFactory = new JPAQueryFactory(em);
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        /*
        count query

        long total = queryFactory.selectFrom(member)
                .fetchCount();
        */

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
    }

    @Test
    public void aggregation() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        // when
        Tuple tuple = result.get(0);
        System.out.println("tuple = " + tuple);
        // then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                /*.having(member.age.avg().gt(20))*/
                .fetch();

        System.out.println("result = " + result);

        // when
        Tuple tuple1 = result.get(0);
        Tuple tuple2 = result.get(1);


        // then
        assertThat(tuple1.get(team.name)).isEqualTo("teamA");
        assertThat(tuple2.get(team.name)).isEqualTo("teamB");

        assertThat(tuple1.get(member.age.avg())).isEqualTo(15);
        assertThat(tuple2.get(member.age.avg())).isEqualTo(35);

    }


    /* 조인 */

    /**
     * teamA에 소속된 모든 회원을 찾아라
     *
     * @throws Exception
     */
    @Test
    public void join() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);
        List<Member> results = queryFactory
                .selectFrom(member)
                .join(member.team, team)    // (member의 team, QTeam) 이렇게 명시해준 것.
                /*.leftJoin() .innerJoin()도 다 있다. */
                /* .on() 생략 시, 기본 id로 알아서 되고, 조금 더 거르고 싶다면 명시해도 된다.*/
                .where(team.name.eq("teamA"))
                .fetch();

        System.out.println("results = " + results);
        // when
        assertThat(results)
                .extracting("username")
                .containsExactly("member1", "member2");
        // then
    }


    /**
     * 세타 조인 - 연관관계가 없는 가운데 하는 조인
     * <p>
     * 회원의 이름이 팀 이름과 같은 회원 조회
     *
     * @throws Exception
     */
    @Test
    public void theta_join() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        /* db 마다 성능 최적화 하는 방법은 다르지만, 어느 정도 한다. */
        List<Member> results = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        System.out.println("results = " + results);
        // when

        // then
        assertThat(results)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


    /*  on 절 */

    /**
     * 예) 회원과 팀을 조인하면스 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     *
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> results = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple result : results) {
            System.out.println("result = " + result);
        }
        // when

        // then
    }

    /**
     * 연관관계가 없는 언티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        /* db 마다 성능 최적화 하는 방법은 다르지만, 어느 정도 한다. */
        List<Tuple> results = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                // 원래는 member.team, team 이엇는데~ 막조인할거라, 그냥 team 이라고 한다. 왜? 연관관계가 없어서

                /* 관계가 없는 조인! on으로 연관관계를 맺어준다. */
                .on(member.username.eq(team.name))
                // 막조인과 기본 조인의 차이! -> team.id = member.team.id 이런게 잇어야 하는데 없다!

                .fetch();

        System.out.println("results = " + results);
        // when
    }


    /* 페치 조인 */
    @PersistenceUnit
    EntityManagerFactory emf;   // 지금 해당 Entity가 로딩되었는지 아닌지 확인하기 위해 사용

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();
        queryFactory = new JPAQueryFactory(em);
        // given
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("description: 페치 조인 미적용").isFalse();

        // then
    }


    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();
        queryFactory = new JPAQueryFactory(em);
        // given
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("description: 페치 조인 적용").isTrue();

        // then
    }

    /*서브 쿼리*/

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);
        // given
        Member result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetchOne();

        assertThat(result.getAge()).isEqualTo(40);
        // then
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQuery2() throws Exception {

        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);
        // given
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())    // 그냥 member Q 가지고 가도 되네?
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        // then
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        select(memberSub.age.avg())  /* JPAExpressions static import */
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }


    /* case 문 */
    @Test
    public void basicCase() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        // when

        // then
    }

    @Test
    public void complexCase() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 40)).then("21~40살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    /* 상수 */
    @Test
    public void constant() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("result = " + result);
    }


    /* 프로잭션 */
    @Test
    public void simpleProjection() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        System.out.println("result = " + result);

        for (Tuple tuple : result) {
            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
        }

        /*
        Tuple은 패키지가 package com.querydsl.core; 라는 건.. repo를 떠나서 사용하는 건 좋지 않음.
        왜? -> ✨✨ 그걸 넘김으로써 service가 repo에 종속적으로 변경되게됨...
        그래서 보통은 dto로 넘기는 게 좋다.

        */
    }

    // dto를 통한 프로잭션
    @Test
    public void findDtoByJPQL() throws Exception {
        // given
        /*new operation 활용법*/
        List<MemberDto> resultList = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                        " from Member m", MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /* 위의 문제를 querydsl은 3가지 방법으로 쉽게 지원한다. */
    @Test
    public void findDtoBySetter() throws Exception {
        // 프로퍼티 접근법

        queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByFields() throws Exception {
        // constructor 접근법

        queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByContructor() throws Exception {
        // 프로퍼티 접근법

        queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructorUserDto() throws Exception {
        // 프로퍼티 접근법 + 별칭사용
        queryFactory = new JPAQueryFactory(em);

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @Test
    public void findDtoByConstructorUserDto2() throws Exception {
        // 프로퍼티 접근법 + 별칭사용
        QMember memberSub = new QMember("memberSub");
        queryFactory = new JPAQueryFactory(em);

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        /*ExpressionUtils.as(member.username, "name") << 위의 코드와 동일 */
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @Test
    public void findDtoByConstructorUserDtoSpecial() throws Exception {
        // 생성자 접근법 -> 별칭사용 안해도 알아서 들어가네?
        QMember memberSub = new QMember("memberSub");
        queryFactory = new JPAQueryFactory(em);

        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        /*ExpressionUtils.as(member.username, "name") << 위의 코드와 동일 */
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }


    /*
    @QueryProjection
        위의 constructor로 설계하는 것과의 차이! 존재하지 않는 필드를 넣어주면,
         @QueryProjection은 compile 오류를 발생시킨다.

         but, Projections.constructor 는 함수가 호출되는 시점에서 오류를 발생시킨다.

    조금만 깊게 고민해보면,,, @QueryProjection을 넣은 dto가 querydsl에 의존적으로 변한다...
    쉽게 말해, 순수한 dto가 아니게 된다...
    */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        // given
        queryFactory = new JPAQueryFactory(em);

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
}
