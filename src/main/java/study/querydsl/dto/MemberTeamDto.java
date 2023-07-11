package study.querydsl.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;


    /* 좋을 수도 있으나,,, 어쩔 수 없이 해당 dto는 querydsl에 의존적일 수 밖에 없다.. */
    @QueryProjection    // 이렇게되면 Q객체가 만들어져야하는데, 보통은 compile을 한번 해주어야 한다.
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
