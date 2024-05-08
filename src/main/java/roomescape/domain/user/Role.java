package roomescape.domain.user;

public enum Role {
    ADMIN("admin"), USER("user");
    private final String value;

    Role(final String value) {
        this.value = value;
    }
    public static Role from(final String value) {
        for (final Role role : Role.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException(String.format("%s 는 역활에 없습니다.",value));
    }

    public String getValue() {
        return value;
    }
}