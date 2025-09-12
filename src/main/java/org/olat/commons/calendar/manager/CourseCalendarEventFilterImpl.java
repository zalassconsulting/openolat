package org.olat.commons.calendar.manager;

import org.apache.logging.log4j.Logger;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.core.id.Identity;
import org.olat.core.logging.Tracing;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.manager.LectureBlockDAO;
import org.olat.modules.lecture.manager.LectureBlockToGroupDAO;
import org.olat.modules.lecture.manager.LectureParticipantSummaryDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourseCalendarEventFilterImpl implements CalendarEventFilter {

    private final Logger log = Tracing.createLoggerFor(CourseCalendarEventFilterImpl.class);

    private final Pattern EXTERNAL_ID_PATTERN = Pattern.compile("^lecture-block-(\\d+)-(\\d+)$");

    @Autowired
    LectureBlockDAO lectureBlockDao;

    @Autowired
    LectureParticipantSummaryDAO lectureParticipantSummaryDao;

    @Override
    public List<KalendarEvent> filterEvents(Identity identity, List<KalendarEvent> calendarEvents) {

        ArrayList<KalendarEvent> result = new ArrayList<>();

        for (KalendarEvent event : calendarEvents) {

            String externalId = event.getExternalId();

            if (externalId != null) {

                Matcher matcher = EXTERNAL_ID_PATTERN.matcher(externalId);

                if (matcher.matches()) {

                    String courseId = matcher.group(1);
                    String lectureBlockId = matcher.group(2);

                    log.info("Course ID: {}, Lecture Block ID: {}", courseId, lectureBlockId);

                    LectureBlock lb = lectureBlockDao.loadByKey(Long.valueOf(lectureBlockId));

                    Date enrollmentDate = lectureParticipantSummaryDao.getEnrollmentDate(lb, identity);

                    log.info("Enrollment date for lecture block {}: {}", lb, enrollmentDate);

                    if (enrollmentDate == null) {
                        // not a participant in this lecture block
                        continue;
                    }

                }
            }

            result.add(event);
        }

        return result;
    }
}
