package com.roy.querydsl.repository.dsl.impl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.roy.querydsl.domain.QSoccerPlayer;
import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.dto.QSoccerPlayerTeamDTO;
import com.roy.querydsl.dto.SoccerPlayerSearchDTO;
import com.roy.querydsl.dto.SoccerPlayerTeamDTO;
import com.roy.querydsl.repository.dsl.SoccerPlayerDslRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

import static com.roy.querydsl.domain.QSoccerPlayer.*;
import static com.roy.querydsl.domain.QTeam.team;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SoccerPlayerDslRepositoryImpl extends QuerydslRepositorySupport
        implements SoccerPlayerDslRepository {

    private final JPAQueryFactory query;

    public SoccerPlayerDslRepositoryImpl(EntityManager entityManager) {
        super(SoccerPlayer.class);
        this.query = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<SoccerPlayerTeamDTO> searchSimplePage(SoccerPlayerSearchDTO dto, Pageable pageable) {
        QueryResults<SoccerPlayerTeamDTO> results = query
                .select(new QSoccerPlayerTeamDTO(
                        soccerPlayer.id, soccerPlayer.name,
                        soccerPlayer.team.id, soccerPlayer.team.name))
                .from(soccerPlayer)
                .leftJoin(soccerPlayer.team, team)
                .where(
                        playerNameEq(dto.getPlayerName()),
                        teamNameEq(dto.getTeamName()),
                        heightGt(dto.getHeightGt()),
                        weightGt(dto.getWeightGt()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<SoccerPlayerTeamDTO> content = results.getResults();
        return new PageImpl<>(content, pageable, results.getTotal());
    }

    @Override
    public Page<SoccerPlayerTeamDTO> searchComplexPage(SoccerPlayerSearchDTO dto, Pageable pageable) {
        List<SoccerPlayerTeamDTO> content = query
                .select(new QSoccerPlayerTeamDTO(
                        soccerPlayer.id, soccerPlayer.name,
                        soccerPlayer.team.id, soccerPlayer.team.name))
                .from(soccerPlayer)
                .leftJoin(soccerPlayer.team, team)
                .where(
                        playerNameEq(dto.getPlayerName()),
                        teamNameEq(dto.getTeamName()),
                        heightGt(dto.getHeightGt()),
                        weightGt(dto.getWeightGt()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = query
                .select(soccerPlayer.count())
                .from(soccerPlayer)
                .where(
                        playerNameEq(dto.getPlayerName()),
                        teamNameEq(dto.getTeamName()),
                        heightGt(dto.getHeightGt()),
                        weightGt(dto.getWeightGt()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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

    public Page<SoccerPlayerTeamDTO> searchPageWithSort(SoccerPlayerSearchDTO dto, Pageable pageable) {
        JPAQuery<SoccerPlayerTeamDTO> searchQuery = query
                .select(new QSoccerPlayerTeamDTO(
                        soccerPlayer.id, soccerPlayer.name,
                        soccerPlayer.team.id, soccerPlayer.team.name))
                .from(soccerPlayer)
                .leftJoin(soccerPlayer.team, team)
                .where(
                        playerNameEq(dto.getPlayerName()),
                        teamNameEq(dto.getTeamName()),
                        heightGt(dto.getHeightGt()),
                        weightGt(dto.getWeightGt()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        for (Sort.Order o : pageable.getSort()) {
            PathBuilder pathBuilder = new PathBuilder(soccerPlayer.getType(), soccerPlayer.getMetadata());
            searchQuery.orderBy(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
        }

        List<SoccerPlayerTeamDTO> content = searchQuery.fetch();

        JPAQuery<Long> countQuery = query
                .select(soccerPlayer.count())
                .from(soccerPlayer)
                .where(
                        playerNameEq(dto.getPlayerName()),
                        teamNameEq(dto.getTeamName()),
                        heightGt(dto.getHeightGt()),
                        weightGt(dto.getWeightGt()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
