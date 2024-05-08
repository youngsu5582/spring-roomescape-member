package roomescape.service.dto.output;

import roomescape.domain.user.Member;

public record MemberCreateOutput(long id, String name, String email) {
    public static MemberCreateOutput toOutput(final Member member) {
        return new MemberCreateOutput(member.getId(), member.getName(), member.getEmail());
    }
}