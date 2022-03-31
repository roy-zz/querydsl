package com.roy.querydsl.repository;

import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.repository.dsl.SoccerPlayerDslRepository;
import com.roy.querydsl.repository.query.SoccerPlayerQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface SoccerPlayerRepository extends
        JpaRepository<SoccerPlayer, Long>,
        SoccerPlayerQueryRepository,
        SoccerPlayerDslRepository,
        QuerydslPredicateExecutor<SoccerPlayer> {
}
