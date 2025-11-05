package org.olat.course.reminder.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.*;

public class SmsEditStep extends BasicStep {

	public SmsEditStep(UserRequest ureq) {
		super(ureq);
		setI18nTitleAndDescr("edit.sms", null);
		setNextStep(Step.NOSTEP);
	}

	@Override
	public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
		return PrevNextFinishConfig.BACK_FINISH;
	}

	@Override
	public StepFormController getStepController(UserRequest ureq, WindowControl wControl,
			StepsRunContext stepsRunContext, Form form) {
		return new SmsEditController(ureq, wControl, form, stepsRunContext);
	}

}
