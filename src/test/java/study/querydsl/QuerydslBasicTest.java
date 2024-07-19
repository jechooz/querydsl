package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
            //�ʵ�� ���� �ڵ� ���̱�
            queryFactory = new JPAQueryFactory(em);

            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            Member member1 = new Member("member1",10,teamA);
            Member member2 = new Member("member2",20,teamA);

            Member member3 = new Member("member3",30,teamB);
            Member member4 = new Member("member4",40,teamB);

            em.persist(member1);
            em.persist(member2);
            em.persist(member3);
            em.persist(member4);

            //�ʱ�ȭ
            em.flush();
            em.clear();

            //Ȯ��
            List<Member> members = em.createQuery("select m from Member m", Member.class)
                    .getResultList();

            for (Member member : members) {
                System.out.println("member ="+ member);
                System.out.println("->member.team"+member.getTeam());
            }
    }


    @Test
    public void startJPQL(){

        String qlString =
                "select m from Member m "+
                " where m.username =:username";

        //member1�� ã�ƶ�
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){

        //1)�⺻ �ν��Ͻ� ���
        //QMember qMember = QMember.member;

        //2)��Ī ���� ���
        //QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchAndParam() {
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetch();
        assertThat(result1.size()).isEqualTo(1);
    }

    //�����ȸ
    @Test
    public void resultFetch(){
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        queryFactory
//                .selectFrom(member)
//                .fetchFirst();


        //***
//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * ȸ�����ļ���
     * 1. ȸ������ ��������(desc)
     * 2. ȸ������ �ø�����(asc)
     * �� 2���� ȸ�� �̸��� ������ �������� ���(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        //����
        Member member5=result.get(0);
        Member member6=result.get(1);
        Member memberNull=result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  //���°���� ��ŵ�Ұž�
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }


    //��ü��ȸ��
    //�ǹ������� ���ɾָ�
    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  //���°���� ��ŵ�Ұž�
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
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

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * ���� �̸��� �� ���� ��տ����� ���ض�.
     * */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * ��A�� �Ҽӵ� ��� ȸ��
     * */
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");

    }

    /**
     * ��Ÿ����
     * ȸ���� �̸��� ���̸��� ���� ȸ����ȸ
     * ��� ȸ��,��� ���� ���̺��� �����´����� �� �� ������ �ع����°�
     */
//    @Test
//    public void theta_join(){
//        List<Member> result = queryFactory
//                .select(member)
//                .from(member, team)
//                .where(member.username.eq(team.name))
//                .fetch();
//        assertThat(result)
//                .extracting("username")
//                .containsExactly("teamA", "teamB");
//    }

    /**
     * ��) ȸ���� ���� �����ϸ鼭, �� �̸��� teamA�� ���� ����, ȸ���� ��� ��ȸ
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. �������� ���� ��ƼƼ �ܺ� ����
     * ��) ȸ���� �̸��� ���� �̸��� ���� ��� �ܺ� ����
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    //��ġ���� ���� ��÷ε����� Member, Team SQL ���� �������� �ѹ��� ��ȸ
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //findMember.getTeam������ �ε��� ��ƼƼ����?���� �ε� �ʱ�ȭ�� �ȵ� ��ƼƼ���� �˷���
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("��ġ���ι�����").isTrue();
    }

    /**
     * ���� ����
     * ���̰� ���� ���� ȸ�� ��ȸ
     */
    @Test
    public void subQuery() throws Exception{

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * ���� ����
     * ���̰� ����� ȸ�� ��ȸ
     */
    @Test
    public void subQueryGoe() throws Exception{

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }


    /**
     * �������� ���� �� ó��, in ���
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * DB���� �߾���������
     * */
    @Test
    public void basicCase() throws Exception{

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("����")
                        .when(20).then("������")
                        .otherwise("��Ÿ"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s=" + s);
        }
    }

    @Test
    public void complexCase() throws Exception{

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20��")
                        .when(member.age.between(21, 30)).then("21~30��")
                        .otherwise("��Ÿ"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s=" + s);
        }
    }

    /**
     * ���� ���ϱ� concat
     * */
    @Test
    public void concat() throws Exception{

        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s=" + s);
        }
    }

    //================�߱޹���=========================

    @Test
    public void tupleProjection() throws Exception{
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }

    //JPQL���� �������ǰ� ��� ��ȯ - DTO ��ȸ
    @Test
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    //������Ƽ ���� - Setter
    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("member");
        }
    }


    //�ʵ� ���� ����
    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("member");
        }
    }

    //��Ī�� �ٸ� ��
    /**
     * ������Ƽ��, �ʵ� ���� ���� ��Ŀ��� �̸��� �ٸ� �� �ذ� ���
     * ExpressionUtils.as(source,alias) : �ʵ峪, ���� ������ ��Ī ����
     * username.as("memberName") : �ʵ忡 ��Ī ����
     * */
    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"),

                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age")
                        ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto ="+userDto);
        }
    }

    @Test
    public void findDtoConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto =" + memberDto);
        }
    }

    //�������ǰ� ��� ��ȯ - @QueryProjection
    //Dto�� Q���Ϸ� �������ؼ�Query�� ������ Ȱ���� �� ����
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+ memberDto);
        }
    }

    //���� ���� 1) - BooleanBuilder
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception{
        //���� �̸��� "member1"�̰� 10���� ����� ã��;�
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
        //�Ķ���� ���� null�̳� �ƴϳĿ� ���� Query�� �������� ���ؾߵ�

    }


    private List<Member> searchMember1(String usernameCond,Integer ageCond){

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //���� ���� 2) -Where ���� �Ķ���� ���
    @Test
    public void dynamicQuery_whereParam()  throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private Predicate ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

//    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
//        return usernameEq(usernameCond).and(ageEq(ageCond));
//    }


    /**
     * ����, ���� ��ũ ����
     * ���� �ѹ����� �뷮 ������ ����
     * */
    public void bulkUpdate() {
        //member1=10 ->DB ��ȸ��
        //member2=20 ->DB ��ȸ��
        //member3=30 ->DB member3
        //member4=40 ->DB member4

        long count = queryFactory
                .update(member)
                .set(member.username, "��ȸ��")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();
        //���Ӽ� ���ؽ�Ʈ�� �ִ� ��ƼƼ�� �����ϰ� ����Ǳ� ������
        // ��ġ ������ �����ϰ� ���� ���Ӽ� ���ؽ�Ʈ�� �ʱ�ȭ �ϴ� ���� �����ϴ�.
    }

    @Test
    public void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    //member M���� �����ϴ� replace �Լ� ���
    @Test
    public void sqlFunction(){
        String result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetchFirst();
    }

    @Test
    public void sqlFunction2(){
        queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
    }

}
