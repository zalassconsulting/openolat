package org.olat.course.reminder.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailHelper;
import org.olat.modules.reminder.Reminder;
import org.olat.modules.reminder.manager.CourseReminderTemplate;
import org.olat.modules.reminder.ui.ReminderAdminController;

public class SmsEditController extends StepFormBasicController {

	private TextElement contentEL;

	private final Reminder reminder;

	public SmsEditController(UserRequest ureq, WindowControl wControl, Form rootForm, StepsRunContext runContext) {
		super(ureq, wControl, rootForm, runContext, LAYOUT_VERTICAL, null);
		setTranslator(Util.createPackageTranslator(ReminderAdminController.class, getLocale(), getTranslator()));
		reminder = (Reminder)runContext.get(RulesEditStep.CONTEXT_KEY);
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		//sms content
		String smsContent = reminder.getSmsContent();
		contentEL = uifactory.addTextElement("reminder.sms.content", "reminder.sms.content", 128, smsContent, formLayout);
		contentEL.setMandatory(false);
		contentEL.setElementCssClass("o_sel_course_reminder_subject");
		contentEL.setHelpText(MailHelper.getVariableNamesHelp(CourseReminderTemplate.subjectVariableNames(), getLocale(), false));

	}

	@Override
	protected void formOK(UserRequest ureq) {
		String smsContent = contentEL.getValue();
		reminder.setSmsContent(smsContent);

		fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
	}
}
