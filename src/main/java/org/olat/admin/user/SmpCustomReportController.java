package org.olat.admin.user;

import org.apache.http.client.utils.URIBuilder;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.manager.OrganisationDAO;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.id.Organisation;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SmpCustomReportController extends FormBasicController {

    public static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private FormLink rsaButton;
    private FormLink apButton;
    private DateChooser dateChooser;
    private SingleSelection contextSelection;


    private final String targetEmail;

    private final HttpClient client = HttpClient.newHttpClient();

    @Autowired
    private BaseSecurity securityManager;

    @Autowired
    private OrganisationDAO organisationDAO;

    private final List<Organisation> contextOrganisations = new ArrayList<>();

    public SmpCustomReportController(UserRequest ureq, WindowControl wControl) {
        super(ureq, wControl);
        targetEmail = ureq.getIdentity().getUser().getEmail();
        initForm(ureq);
    }

    @Override
    protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {

        contextOrganisations.addAll(organisationDAO.loadByType("CTX"));

        uifactory.addStaticTextElement(translate("menu.smp.label"), targetEmail, formLayout);


        LocalDate ld = LocalDate.now().withDayOfMonth(1);

        Date startDate = Date.from(ld.minusMonths(3).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(ld.atStartOfDay().minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());

        dateChooser = uifactory.addDateChooser("menu.smp.dates", "menu.smp.dates", startDate, formLayout);
        dateChooser.setSecondDate(true);
        dateChooser.set2DigitsYearFormat(false);
        dateChooser.setSecondDate(endDate);

        String[] contextKeys = contextOrganisations.stream().map(Organisation::getKey).map(String::valueOf).toArray(String[]::new);
        String[] contextValues = contextOrganisations.stream().map(Organisation::getDisplayName).toArray(String[]::new);

        contextSelection = uifactory.addDropdownSingleselect("menu.smp.context", formLayout, contextKeys, contextValues);

        rsaButton = uifactory.addFormLink(translate("menu.smp.button.rsa"), formLayout, Link.BUTTON);
        rsaButton.addActionListener(FormEvent.ONCLICK);

        apButton = uifactory.addFormLink(translate("menu.smp.button.ap"), formLayout, Link.BUTTON);
        apButton.addActionListener(FormEvent.ONCLICK);



    }

    @Override
    protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
        if (rsaButton == source) {
            // launch the RSA report
            requestSendingReport(ReportType.RSA);
            fireEvent(ureq, Event.DONE_EVENT);
        } else if (apButton == source) {
            // launch the AP report

            Date from = dateChooser.getDate();
            Date to = dateChooser.getSecondDate();

            logInfo("Sending action plan to " + targetEmail + " for dates " + from + " to " + to);

            requestSendingReport(ReportType.AP);
            fireEvent(ureq, Event.DONE_EVENT);
        }
        super.formInnerEvent(ureq, source, event);
    }

    @Override
    protected void formOK(UserRequest ureq) {
        fireEvent(ureq, Event.DONE_EVENT);
    }

    private enum ReportType {
        RSA("rsa-monthly/excel"),
        AP("action-plan/excel");

        private final String path;

        ReportType(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    private void requestSendingReport(ReportType reportType) {

        try {
            URI uri = new URIBuilder().setScheme("http").setHost("localhost").setPort(8080).setPath(reportType.getPath())
                    .addParameter("email", targetEmail)
                    .addParameter("from", "" + dateChooser.getDate().getTime())
                    .addParameter("to", "" + dateChooser.getSecondDate().getTime())
                    .addParameter("ctx", contextSelection.getSelectedKey())
                    .build();

            logInfo("Invoking report URI: " + uri);

            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", EXCEL_MIME_TYPE)
                .GET().build();

            client.sendAsync(
                            request, HttpResponse.BodyHandlers.discarding()
                    ).thenApply(HttpResponse::statusCode)
                    .thenAccept(statusCode -> {
                        if (statusCode == HttpURLConnection.HTTP_OK) {
                            logInfo("Action plan successfully sent to " + targetEmail);
                        } else {
                            logWarn("Action plan could not be sent to " + targetEmail, null);
                        }
                    });
        } catch (URISyntaxException e) {
            logError("Error while sending action plan to " + targetEmail, e);
        }
    }
}
