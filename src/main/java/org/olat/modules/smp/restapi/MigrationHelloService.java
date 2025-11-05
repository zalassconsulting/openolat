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
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.manager.AssessmentEntryDAO;
import org.olat.modules.assessment.model.AssessmentEntryImpl;
import org.olat.modules.assessment.model.AssessmentRunStatus;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.manager.RepositoryEntryDAO;
import org.olat.resource.OLATResourceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
@Path("migration/hello")
@Produces(MediaType.APPLICATION_JSON)
public class MigrationHelloService {

    @GET
    public Response sayHello(@QueryParam("name") String name) {
        return Response.ok("Hello " + name).build();
    }
}
