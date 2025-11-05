package org.olat.course.reminder.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.modules.reminder.Reminder;

public class SmsViewController extends FormBasicController {

	private final Reminder reminder;

	public SmsViewController(UserRequest ureq, WindowControl wControl, Reminder reminder) {
		super(ureq, wControl, LAYOUT_VERTICAL);
		this.reminder = reminder;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		uifactory.addStaticTextElement("reminder.sms.content", reminder.getSmsContent(), formLayout);
//		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
//		buttonsCont.setRootForm(mainForm);
//		formLayout.add(buttonsCont);
//		buttonsCont.setElementCssClass("o_button_group o_button_group_top o_button_group_bottom");
//		uifactory.addFormSubmitButton("close", buttonsCont);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent(ureq, FormEvent.CLOSE_EVENT);
	}
}
