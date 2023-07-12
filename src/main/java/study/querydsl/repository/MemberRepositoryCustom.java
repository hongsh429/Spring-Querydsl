package study.querydsl.repository;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {
    // 여기에 내가 원하는 메서드를 정의한다.
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
