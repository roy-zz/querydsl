package com.roy.querydsl;

import com.querydsl.core.types.Predicate;
import com.roy.querydsl.domain.SoccerPlayer;
import com.roy.querydsl.repository.SoccerPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/soccer-player")
public class SoccerPlayerController {

    private final SoccerPlayerRepository soccerPlayerRepository;

    @GetMapping("")
    public Iterable test(@QuerydslPredicate(root = SoccerPlayer.class) Predicate predicate,
                        Pageable pageable, @RequestParam MultiValueMap<String, String> params) {
        return soccerPlayerRepository.findAll(predicate, pageable);
    }

}
