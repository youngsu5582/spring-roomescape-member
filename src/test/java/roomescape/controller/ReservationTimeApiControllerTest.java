package roomescape.controller;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import roomescape.fixture.ThemeFixture;
import roomescape.service.ReservationService;
import roomescape.service.ReservationTimeService;
import roomescape.service.ThemeService;
import roomescape.service.dto.input.ReservationInput;
import roomescape.service.dto.input.ReservationTimeInput;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ReservationTimeApiControllerTest {

    @Autowired
    ReservationService reservationService;

    @Autowired
    ReservationTimeService reservationTimeService;
    @Autowired
    ThemeService themeService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update("TRUNCATE TABLE reservation");
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("TRUNCATE TABLE reservation_time");
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    @DisplayName("시간 생성에 성공하면, 201을 반환한다")
    void return_201_when_reservationTime_create_success() {
        Map<String, String> params = new HashMap<>();
        params.put("startAt", "10:00");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/times")
                .then()
                .statusCode(201);

        RestAssured.given()
                .when().get("/times")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @DisplayName("시간 생성 시 시작 시간에 유효하지 않은 값이 입력되었을 때 400을 반환한다.")
    void return_400_when_reservationTime_create_input_is_invalid() {
        Map<String, String> params = new HashMap<>();
        params.put("startAt", "");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/times")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("특정 시간이 존재하지 않는데, 그 시간을 삭제하려 할 때 404을 반환한다.")
    void return_404_when_not_exist_id() {
        RestAssured.given()
                .delete("/times/-1")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("특정 시간에 대한 예약이 존재하는데, 그 시간을 삭제하려 할 때 409를 반환한다.")
    void return_409_when_delete_id_that_exist_reservation() {
        long timeId = reservationTimeService.createReservationTime(new ReservationTimeInput("09:00")).id();
        long themeId = themeService.createTheme(ThemeFixture.getInput()).id();
        reservationService.createReservation(new ReservationInput("제리", "2025-04-30", timeId, themeId));

        RestAssured.given()
                .delete("/times/" + timeId)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("중복된 예약 시간을 생성하려 할 때 409를 반환한다.")
    void return_409_when_duplicate_reservationTime() {
        reservationTimeService.createReservationTime(new ReservationTimeInput("10:00"));

        Map<String, String> params = new HashMap<>();
        params.put("startAt", "10:00");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/times")
                .then()
                .statusCode(409);
    }
}