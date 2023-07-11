package study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

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
                .selectFrom(member)
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
     * @throws Exception
     */
    @Test
    public void sort() throws Exception {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        // when

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

        // then
    }
}
