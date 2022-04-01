package com.roy.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.dto.SoccerPlayerSearchDTO;
import com.roy.querydsl.repository.support.CustomQuerydslRepositorySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static com.roy.querydsl.domain.QSoccerPlayer.soccerPlayer;
import static com.roy.querydsl.domain.QTeam.team;

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
