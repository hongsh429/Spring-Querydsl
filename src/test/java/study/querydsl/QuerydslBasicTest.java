package study.querydsl;


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


}
