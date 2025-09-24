package org.olat.modules.reminder.rule;

import org.apache.logging.log4j.Logger;
import org.olat.core.id.Identity;
import org.olat.core.logging.Tracing;
import org.olat.modules.lecture.LectureBlock;
import org.olat.modules.lecture.manager.LectureBlockDAO;
import org.olat.modules.reminder.FilterRuleSPI;
import org.olat.modules.reminder.ReminderRule;
import org.olat.modules.reminder.RepositoryEntryRuleSPI;
import org.olat.modules.reminder.model.ReminderIdentity;
import org.olat.modules.reminder.model.ReminderRuleImpl;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CourseEventReminderRuleSPI extends AbstractBeforeAfterRuleSPI implements FilterRuleSPI {

    protected final Logger log = Tracing.createLoggerFor(getClass());

    @Autowired
    LectureBlockDAO lectureBlockDao;

    @Override
    public String getLabelI18nKey() {
        return "rule.smp.course.event";
    }

    protected LocalDate getReferenceDate(ReminderRule rule) {
        if (rule instanceof ReminderRuleImpl ruleImpl) {
            LocalDate now = LocalDate.now();
            int distance = -Integer.parseInt(ruleImpl.getRightOperand()); // minus, because olat says so
            LaunchUnit unit = LaunchUnit.valueOf(ruleImpl.getRightUnit());

            return switch (unit) {
                case day -> now.plusDays(distance);
                case week -> now.plusWeeks(distance);
                case month -> now.plusMonths(distance);
                case year -> now.plusYears(distance);
            };
        } else {
            return null;
        }

    }

    private static boolean isBlockBeginningOnReferenceDate(LectureBlock block, LocalDate referenceDate) {
        return referenceDate.equals(LocalDate.ofInstant(block.getStartDate().toInstant(), ZoneId.systemDefault()));
    }

    @Override
    public void filter(RepositoryEntry entry, List<Identity> identities, ReminderRule rule) {

        LocalDate referenceDate = getReferenceDate(rule);

        if (referenceDate == null) {
            log.debug("Reference date is null, skipping course event rule filter");
            return;
        }

        List<LectureBlock> lectureBlocks = lectureBlockDao.getLectureBlocks(entry);


        for (LectureBlock lectureBlock : lectureBlocks) {
            if (isBlockBeginningOnReferenceDate(lectureBlock, referenceDate)) {
                List<Identity> participants = lectureBlockDao.getParticipants(lectureBlock);
                for (ListIterator<Identity> identityIt = identities.listIterator(); identityIt.hasNext(); ) {
                    Identity identity = identityIt.next();
                    if (participants.contains(identity)) {
                        identityIt.set(new ReminderIdentity(identity, ReminderIdentity.LECTURE_BLOCK_PROPERTY_KEY, lectureBlock));
                    }
                }
            }
        }

        identities.removeIf(identity -> !(identity instanceof ReminderIdentity));
        log.debug("Filter in course event rule: {}", rule);
    }
}
