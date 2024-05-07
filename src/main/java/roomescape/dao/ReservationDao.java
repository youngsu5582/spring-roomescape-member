package roomescape.dao;

import java.util.List;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.dao.mapper.ReservationRowMapper;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationDate;

@Repository
public class ReservationDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    private final ReservationRowMapper rowMapper;

    public ReservationDao(final JdbcTemplate jdbcTemplate, final DataSource dataSource, final ReservationRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("reservation")
                .usingGeneratedKeyColumns("id");
        this.rowMapper = rowMapper;
    }

    public Reservation create(final Reservation reservation) {
        final SqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", reservation.getNameAsString())
                .addValue("date", reservation.getDate().asString())
                .addValue("time_id", reservation.getTime().getId())
                .addValue("theme_id", reservation.getTheme().getId());
        long id = jdbcInsert.executeAndReturnKey(params).longValue();
        return new Reservation(id, reservation.getName(), reservation.getDate(), reservation.getTime(),
                reservation.getTheme());
    }

    public boolean isExistByReservationAndTime(final ReservationDate date, final long timeId) {
        final String sql = "SELECT EXISTS (SELECT 1 FROM reservation WHERE date = ? AND time_id = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, date.asString(), timeId));
    }

    public boolean isExistByTimeId(final long timeId) {
        return isExistByCondition("time_id", timeId);
    }

    public boolean isExistByThemeId(final long themeId) {
        return isExistByCondition("theme_id", themeId);
    }

    public boolean isExistById(final long id) {
        return isExistByCondition("id", id);
    }

    private boolean isExistByCondition(final String conditionColumn, final Object conditionValue) {
        final String sql = "SELECT EXISTS (SELECT 1 FROM reservation WHERE " + conditionColumn + " = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, conditionValue));
    }

    public List<Reservation> getAll() {
        final String sql = """
                SELECT
                r.id AS reservation_id,
                r.name,
                r.date,
                t.id AS time_id,
                t.start_at AS time_value,
                th.id AS theme_id,
                th.name AS theme_name,
                th.description AS theme_description,
                th.thumbnail AS theme_thumbnail
                FROM reservation AS r
                INNER JOIN reservation_time AS t 
                ON r.time_id = t.id 
                INNER JOIN theme AS th 
                ON r.theme_id = th.id 
                """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void delete(final long id) {
        final String sql = "DELETE FROM reservation WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
