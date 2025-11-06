package org.olat.course.reminder.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormToggle;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailHelper;
import org.olat.modules.reminder.Reminder;
import org.olat.modules.reminder.manager.CourseReminderTemplate;
import org.olat.modules.reminder.ui.ReminderAdminController;

public class SmsEditController extends StepFormBasicController {

	private TextElement contentEL;
	private FormToggle smsEnabledEL;


	private final Reminder reminder;

	public SmsEditController(UserRequest ureq, WindowControl wControl, Form rootForm, StepsRunContext runContext) {
		super(ureq, wControl, rootForm, runContext, LAYOUT_VERTICAL, null);
		setTranslator(Util.createPackageTranslator(ReminderAdminController.class, getLocale(), getTranslator()));
		reminder = (Reminder)runContext.get(RulesEditStep.CONTEXT_KEY);
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		//sms toggle on/off
		boolean managedEff = reminder.getSmsEnabled();
		smsEnabledEL = uifactory.addToggleButton("smsIsOn", "reminder.sms.enabled.statement", translate("on"), translate("off"), formLayout);
		smsEnabledEL.addActionListener(FormEvent.ONCHANGE);
		smsEnabledEL.toggle(managedEff);
		smsEnabledEL.setEnabled(true);

		//sms content
		String smsContent = reminder.getSmsContent();
		if(!StringHelper.containsNonWhitespace(smsContent)) {
			smsContent = translate("reminder.sms.body");
		}
		contentEL = uifactory.addTextElement("reminder.sms.content", "reminder.sms.content", 320, smsContent, formLayout);
		contentEL.setMandatory(false);
		contentEL.setElementCssClass("o_sel_course_reminder_subject");
		contentEL.setHelpText(MailHelper.getVariableNamesHelp(CourseReminderTemplate.bodyVariableNames(), getLocale(), false));

	}

	@Override
	protected void formOK(UserRequest ureq) {
		String smsContent = contentEL.getValue();
		boolean smsEnabled = smsEnabledEL.isOn();
		reminder.setSmsContent(smsContent);
		reminder.setSmsEnabled(smsEnabled);

		fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
	}
}
