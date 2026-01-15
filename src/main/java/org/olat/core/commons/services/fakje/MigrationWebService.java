package org.olat.core.commons.services.fakje;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.olat.basesecurity.manager.OrganisationDAO;
import org.olat.core.id.Organisation;
import org.olat.core.id.Roles;
import org.olat.modules.assessment.MigrationService;
import org.olat.modules.assessment.model.CallReqDTO;
import org.olat.modules.assessment.model.MigrateReqDTO;
import org.olat.modules.assessment.model.Validatable;
import org.olat.restapi.security.RestSecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Path("migration")
public class MigrationWebService {

    @Autowired
    private MigrationService migrationService;

    @Autowired
    private OrganisationDAO odao;

    @POST
    @Path("callMe")
    public Response callMe(@Context HttpServletRequest request) {
        CallReqDTO body = getBody(request);
        Response response = checkOk(body, request);
        if(response != null) return response;
        migrationService.callMe(body.db, body.user, body.pass, body.org, body.tid, RestSecurityHelper.getIdentity(request));
        return Response.ok().build();
    }

    @POST
    @Path("migrateMe")
    public Response migrateMe(@Context HttpServletRequest request) {
        MigrateReqDTO body = getBodyM(request);
        Response response = checkOk(body, request);
        if(response != null) return response;
        migrationService.migrateMe(body.db, body.user, body.pass, body.uid, RestSecurityHelper.getIdentity(request));
        return Response.ok().build();
    }

    @POST
    @Path("callMeAllTheWay")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response callMeAll(@Context HttpServletRequest request) {
        CallReqDTO body = getBody(request);
        Response response = checkOk(body, request);
        if(response != null) return response;
        migrationService.callMe(body.db, body.user, body.pass, body.org, RestSecurityHelper.getIdentity(request));
        return Response.ok().build();
    }

    private boolean isAdmin(HttpServletRequest request) {
        try {
            Roles roles = RestSecurityHelper.getRoles(request);
            return roles.isAdministrator();
        } catch (Exception e) {
            return false;
        }
    }

    private Response checkOk(Validatable body, @Context HttpServletRequest request) {
        if(!isAdmin(request)) {
            return Response.serverError().status(Response.Status.FORBIDDEN).build();
        }
        try {
            body.validate();
        } catch (IllegalArgumentException e) {
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return null;
    }

    private CallReqDTO getBody(HttpServletRequest req) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(req.getInputStream(), CallReqDTO.class);
        } catch(IOException e) {
            return new CallReqDTO(null, null, null,null, null);
        }
    }

    private MigrateReqDTO getBodyM(HttpServletRequest req) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(req.getInputStream(), MigrateReqDTO.class);
        } catch(IOException e) {
            return new MigrateReqDTO(null, null, null,null);
        }
    }
}
