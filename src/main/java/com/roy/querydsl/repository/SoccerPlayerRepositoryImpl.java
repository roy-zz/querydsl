package com.roy.querydsl.repository;

import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.repository.dsl.SoccerPlayerDslRepository;
import com.roy.querydsl.repository.query.SoccerPlayerQueryRepository;
import com.roy.querydsl.repository.query.impl.SoccerPlayerQueryRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoccerPlayerRepositoryImpl extends
        JpaRepository<SoccerPlayer, Long>,
        SoccerPlayerQueryRepository,
        SoccerPlayerDslRepository {
}
