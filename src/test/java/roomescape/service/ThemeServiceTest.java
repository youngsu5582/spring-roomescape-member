package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.dao.ReservationDao;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.exception.ExistReservationInThemeException;
import roomescape.exception.NotExistThemeException;
import roomescape.fixture.ThemeFixture;
import roomescape.service.dto.input.ReservationTimeInput;
import roomescape.service.dto.input.ThemeInput;
import roomescape.service.dto.output.ReservationTimeOutput;
import roomescape.service.dto.output.ThemeOutput;

@SpringBootTest
class ThemeServiceTest {

    @Autowired
    ThemeService themeService;
    @Autowired
    ReservationTimeService reservationTimeService;
    @Autowired
    ReservationDao reservationDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("TRUNCATE TABLE reservation");
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("TRUNCATE TABLE theme");
        jdbcTemplate.update("TRUNCATE TABLE reservation_time");
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    @DisplayName("유효한 값을 입력하면 예외를 발생하지 않는다.")
    void create_reservationTime() {
        ThemeInput input = new ThemeInput(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg"
        );
        assertThatCode(() -> themeService.createTheme(input))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("유효하지 않은 값을 입력하면 예외를 발생한다.")
    void throw_exception_when_input_is_invalid() {
        ThemeInput input = new ThemeInput(
                "",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg"
        );
        assertThatThrownBy(() -> themeService.createTheme(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("모든 요소를 받아온다.")
    void get_all_themes() {
        ThemeInput input = new ThemeInput(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg"
        );
        themeService.createTheme(input);

        var result = themeService.getAllThemes();
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 테마 ID 를 삭제하려 하면 에외를 발생한다.")
    void throw_exception_when_not_exist_id() {
        assertThatThrownBy(() -> themeService.deleteTheme(-1))
                .isInstanceOf(NotExistThemeException.class);
    }

    @Test
    @DisplayName("특정 테마에 대한 예약이 존재하면 예외를 발생한다.")
    void throw_exception_when_delete_id_that_exist_reservation() {
        ThemeOutput themeOutput = themeService.createTheme(
                ThemeFixture.getInput());

        ReservationTimeOutput timeOutput = reservationTimeService.createReservationTime(
                new ReservationTimeInput("10:00"));

        reservationDao.create(Reservation.from(
                null,
                "제리",
                "2024-04-30",
                ReservationTime.from(timeOutput.id(), timeOutput.startAt()),
                Theme.of(themeOutput.id(), themeOutput.name(), themeOutput.description(), themeOutput.thumbnail())
        ));

        assertThatThrownBy(() -> themeService.deleteTheme(themeOutput.id()))
                .isInstanceOf(ExistReservationInThemeException.class);
    }

    @Test
    @DisplayName("예약이 많은 테마 순으로 조회한다.")
    void get_popular_themes() {
        ThemeOutput themeOutput1 = themeService.createTheme(ThemeFixture.getInput());
        final ThemeOutput themeOutput2 = themeService.createTheme(new ThemeInput(
                "레벨3 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://i.pinimg.com/236x/6e/bc/46/6ebc461a94a49f9ea3b8bbe2204145d4.jpg"
        ));
        final ReservationTimeOutput timeOutput = reservationTimeService.createReservationTime(
                new ReservationTimeInput("10:00"));

        reservationDao.create(Reservation.from(
                null,
                "제리",
                "2024-06-01",
                ReservationTime.from(timeOutput.id(), timeOutput.startAt()),
                Theme.of(themeOutput1.id(), themeOutput1.name(), themeOutput1.description(), themeOutput1.thumbnail())
        ));
        reservationDao.create(Reservation.from(
                null,
                "조이썬",
                "2024-06-02",
                ReservationTime.from(timeOutput.id(), timeOutput.startAt()),
                Theme.of(themeOutput1.id(), themeOutput1.name(), themeOutput1.description(), themeOutput1.thumbnail())
        ));
        reservationDao.create(Reservation.from(
                null,
                "제리",
                "2024-06-03",
                ReservationTime.from(timeOutput.id(), timeOutput.startAt()),
                Theme.of(themeOutput2.id(), themeOutput2.name(), themeOutput2.description(), themeOutput2.thumbnail())
        ));

        final List<ThemeOutput> popularThemes = themeService.getPopularThemes("2024-06-04");

        assertThat(popularThemes).containsExactly(
                new ThemeOutput(themeOutput1.id(), themeOutput1.name(), themeOutput1.description(),
                        themeOutput1.thumbnail()),
                new ThemeOutput(themeOutput2.id(), themeOutput2.name(), themeOutput2.description(),
                        themeOutput2.thumbnail())
        );
    }
}
