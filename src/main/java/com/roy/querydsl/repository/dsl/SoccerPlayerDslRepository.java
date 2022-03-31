package com.roy.querydsl.repository.dsl;

import com.roy.querydsl.dto.SoccerPlayerSearchDTO;
import com.roy.querydsl.dto.SoccerPlayerTeamDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SoccerPlayerDslRepository {

    Page<SoccerPlayerTeamDTO> searchSimplePage(SoccerPlayerSearchDTO dto, Pageable pageable);

    Page<SoccerPlayerTeamDTO> searchComplexPage(SoccerPlayerSearchDTO dto, Pageable pageable);

}
