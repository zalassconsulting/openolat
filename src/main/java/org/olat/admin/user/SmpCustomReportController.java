package org.olat.admin.user;

import org.apache.http.client.utils.URIBuilder;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.manager.OrganisationDAO;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.panel.EmptyPanelItem;
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
    MultipleSelectionElement apPositionSelector;
    private FormLink incomeButton;
    private FormLink dealerBudgetButton;
    private FormLink trainerReportButton;
    private FormLink planningReportButton;
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
        FormLayoutContainer emptyPanel = uifactory.addHorizontalFormLayout("menu.smp.button.ap", translate("menu.smp.button.ap"), formLayout);

        apButton = uifactory.addFormLink(translate("menu.smp.button.ap"), emptyPanel, Link.BUTTON);
        apPositionSelector = uifactory.addCheckboxesHorizontal("menu.smp.button.ap.allpositions", null, emptyPanel, new String[]{"alllpositions"}, new String[]{translate("menu.smp.button.ap.allpositions")});
        apButton.addActionListener(FormEvent.ONCLICK);

        rsaButton = uifactory.addFormLink(translate("menu.smp.button.rsa"), formLayout, Link.BUTTON);
        rsaButton.addActionListener(FormEvent.ONCLICK);

        incomeButton = uifactory.addFormLink(translate("menu.smp.button.income"), formLayout, Link.BUTTON);
        incomeButton.addActionListener(FormEvent.ONCLICK);


        dealerBudgetButton = uifactory.addFormLink(translate("menu.smp.button.dealerbudget"), formLayout, Link.BUTTON);
        dealerBudgetButton.addActionListener(FormEvent.ONCLICK);

        trainerReportButton = uifactory.addFormLink(translate("menu.smp.button.trainerreport"), formLayout, Link.BUTTON);
        trainerReportButton.addActionListener(FormEvent.ONCLICK);

        planningReportButton = uifactory.addFormLink(translate("menu.smp.button.planningreport"), formLayout, Link.BUTTON);
        planningReportButton.addActionListener(FormEvent.ONCLICK);

    }

    @Override
    protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {

        Date from = dateChooser.getDate();
        Date to = dateChooser.getSecondDate();

        if (rsaButton == source) {
            // launch the RSA report
            requestSendingReport(ReportType.RSA);
            fireEvent(ureq, Event.DONE_EVENT);
        } else if (apButton == source) {
            // launch the AP report
            logInfo("Sending action plan to " + targetEmail + " for dates " + from + " to " + to);
            requestSendingReport(ReportType.AP, apPositionSelector.isSelected(0));
            fireEvent(ureq, Event.DONE_EVENT);
        } else if (incomeButton == source) {
            logInfo("Sending income report to " + targetEmail + " for dates " + from + " to " + to);
            requestSendingReport(ReportType.INCOME);
            fireEvent(ureq, Event.DONE_EVENT);
        } else if (dealerBudgetButton == source) {
            logInfo("Sending dealer budget report to " + targetEmail + " for dates " + from + " to " + to);
            requestSendingReport(ReportType.DEALER_BUDGET);
            fireEvent(ureq, Event.DONE_EVENT);
        } else if (trainerReportButton == source) {
            logInfo("Sending trainer report to " + targetEmail + " for dates " + from + " to " + to);
            requestSendingReport(ReportType.TRAINERS);
            fireEvent(ureq, Event.DONE_EVENT);
        } else if (planningReportButton == source) {
            logInfo("Sending planning report to " + targetEmail + " for dates " + from + " to " + to);
            requestSendingReport(ReportType.PLANNING);
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
        AP("action-plan/excel"),
        INCOME("income/excel"),
        DEALER_BUDGET("dealer-budget/excel"),
        TRAINERS("trainers/excel"),
        PLANNING("planning/excel");

        private final String path;

        ReportType(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    private void requestSendingReport(ReportType reportType) {
        requestSendingReport(reportType, false);
    }


        private void requestSendingReport(ReportType reportType, boolean... featureFlags) {

        try {
            URIBuilder uriBuilder = new URIBuilder().setScheme("http").setHost("localhost").setPort(8080).setPath(reportType.getPath())
                    .addParameter("email", targetEmail)
                    .addParameter("from", "" + dateChooser.getDate().getTime())
                    .addParameter("to", "" + dateChooser.getSecondDate().getTime())
                    .addParameter("ctx", contextSelection.getSelectedKey());

            if (featureFlags != null) {
                for (int i=0; i < featureFlags.length; i++) {
                    uriBuilder.addParameter("ff" + i, featureFlags[i] ? "true" : "false");
                }
            }

            URI uri = uriBuilder.build();


            logInfo("Invoking report URI: " + uri);

            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", EXCEL_MIME_TYPE)
                .GET().build();

            client.sendAsync(
                            request, HttpResponse.BodyHandlers.discarding()
                    ).thenApply(HttpResponse::statusCode)
                    .thenAccept(statusCode -> {
                        if (statusCode == HttpURLConnection.HTTP_NO_CONTENT) {
                            logInfo("Report successfully sent to " + targetEmail);
                        } else {
                            logWarn("Report could not be sent to " + targetEmail + ", status code: " + statusCode, null);
                        }
                    });
        } catch (URISyntaxException e) {
            logError("Error while sending action plan to " + targetEmail, e);
        }
    }
}
