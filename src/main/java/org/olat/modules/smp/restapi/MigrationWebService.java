package org.olat.modules.smp.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.id.Identity;
import org.olat.course.core.CourseElement;
import org.olat.course.core.CourseElementSearchParams;
import org.olat.course.core.manager.CourseElementDAO;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.manager.AssessmentEntryDAO;
import org.olat.modules.assessment.model.AssessmentEntryImpl;
import org.olat.modules.assessment.model.AssessmentRunStatus;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.manager.RepositoryEntryDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@Path("migration/assessment")
@Produces(MediaType.APPLICATION_JSON)
public class MigrationWebService {

    @Autowired
    private AssessmentEntryDAO aeDao;

    @Autowired
    private RepositoryEntryDAO rDao;

    @Autowired
    CourseElementDAO courseElementDao;

    @Autowired
    private BaseSecurity securityManager;

    @GET
    @Path("add")
    @Operation(summary = "Sets the assessment value for a user and course",
            description = "Sets the assessment value for a user and course")
    public Response setCourseAssessment(
            @QueryParam("cid") Long courseId,
            @QueryParam("login") String login,
            @QueryParam("score") Integer score,
            @QueryParam("ts") Long passTimestamp,
            @QueryParam("subid") String passedSubIdent,
            @QueryParam("max") Integer passedMaxScore,
            @QueryParam("threshold") Integer passedThreshold) {

        Identity userIdentity = securityManager.findIdentityByLogin(login);
        RepositoryEntry courseEntry = rDao.loadByKey(courseId);

        CourseElementSearchParams sp = new CourseElementSearchParams();
        sp.setCourseElementType(Arrays.asList("ms", "iqtest"));
        sp.setRepositoryEntries(Collections.singleton(courseEntry));
        List<CourseElement> assessmentElements = courseElementDao.load(sp);

        if (!assessmentElements.isEmpty()) {

            String subId = null;

            if (passedSubIdent != null) {
                boolean subIdentValid = assessmentElements.stream()
                        .anyMatch(e -> passedSubIdent.equals(e.getSubIdent()));

                if (!subIdentValid) {
                    return Response.serverError().entity("Passed subident not found: " + passedSubIdent).build();
                }

                subId = passedSubIdent;

            } else {
                subId = assessmentElements.iterator().next().getSubIdent();
            }

            AssessmentEntry assessmentEntry = aeDao.loadAssessmentEntry(userIdentity, courseEntry, subId);

            if (assessmentEntry == null) {
                assessmentEntry = aeDao.createAssessmentEntry(userIdentity, null, courseEntry, subId, false, null);
            }

            BigDecimal bigDecimalScore = new BigDecimal(score);
            BigDecimal maxScore = new BigDecimal(passedMaxScore != null ? passedMaxScore : 100);

            assessmentEntry.setScore(bigDecimalScore);
            assessmentEntry.setMaxScore(maxScore);
            assessmentEntry.setWeightedMaxScore(maxScore);
            assessmentEntry.setWeightedScore(bigDecimalScore);
            assessmentEntry.setUserVisibility(true);
            assessmentEntry.setFullyAssessed(true);
            assessmentEntry.setCurrentRunStatus(AssessmentRunStatus.done);

            if (assessmentEntry instanceof AssessmentEntryImpl assessmentEntryImpl) {
                assessmentEntryImpl.setPassedOverridable(null);

                if (passedThreshold != null) {
                    assessmentEntryImpl.setRawPassed(bigDecimalScore.compareTo(new BigDecimal(passedThreshold)) >= 0);
                } else {
                    assessmentEntryImpl.setRawPassed(bigDecimalScore.equals(maxScore));
                }

                assessmentEntryImpl.setPassedDate(new Date(passTimestamp));
            }

            aeDao.updateAssessmentEntry(assessmentEntry);

            return Response.ok().entity(assessmentEntry.getKey()).build();

        } else {
            return Response.serverError().entity("Not an OlatResourceImpl").build();
        }



    }

}
