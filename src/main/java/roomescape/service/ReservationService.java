package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.dao.ReservationDao;
import roomescape.dao.ReservationTimeDao;
import roomescape.dao.ThemeDao;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.exception.NotExistReservationException;
import roomescape.exception.NotExistReservationTimeException;
import roomescape.exception.NotExistThemeException;
import roomescape.exception.PastTimeReservationException;
import roomescape.exception.ReservationAlreadyExistsException;
import roomescape.service.dto.input.ReservationInput;
import roomescape.service.dto.output.ReservationOutput;
import roomescape.service.util.DateTimeFormatter;

@Service
public class ReservationService {

    private final ReservationDao reservationDao;
    private final ReservationTimeDao reservationTimeDao;
    private final ThemeDao themeDao;
    private final DateTimeFormatter dateTimeFormatter;

    public ReservationService(ReservationDao reservationDao, ReservationTimeDao reservationTimeDao, ThemeDao themeDao,
                              DateTimeFormatter dateTimeFormatter) {
        this.reservationDao = reservationDao;
        this.reservationTimeDao = reservationTimeDao;
        this.themeDao = themeDao;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public ReservationOutput createReservation(ReservationInput input) {
        //TODO : Controller 가 아닌 ControllerAdvice 가 catch 해주게 변경
        //TODO : LocalTime, LocalDate 를 외부에서 주입할지 고민, 10줄 분리
        ReservationTime time = reservationTimeDao.find(input.timeId())
                .orElseThrow(
                        () -> new NotExistReservationTimeException(String.format("%d는 없는 id 입니다.", input.timeId())));
        Theme theme = themeDao.find(input.themeId())
                .orElseThrow(
                        () -> new NotExistThemeException(String.format("%d는 없는 id 입니다.", input.themeId())));

        Reservation reservation = input.toReservation(time, theme);
        if (reservationDao.isExistByReservationAndTime(reservation.getDate(), time.getId())) {
            throw new ReservationAlreadyExistsException(
                    String.format("%s 에 해당하는 예약이 있습니다.", reservation.getDateAndTimeFormat()));
        }
        if (reservation.isBefore(dateTimeFormatter.getDate(), dateTimeFormatter.getTime())) {
            throw new PastTimeReservationException(String.format("%s는 지난 시간입니다.", reservation.getDateAndTimeFormat()));
        }
        Reservation savedReservation = reservationDao.create(reservation);
        return ReservationOutput.toOutput(savedReservation);
    }

    public List<ReservationOutput> getAllReservations() {
        List<Reservation> reservations = reservationDao.getAll();
        return ReservationOutput.toOutputs(reservations);
    }

    public void deleteReservation(long id) {
        if (!reservationDao.isExistById(id)) {
            throw new NotExistReservationException(String.format(String.format("%d는 없는 id 입니다.", id)));
        }
        reservationDao.delete(id);
    }
}
