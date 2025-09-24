package org.olat.modules.reminder.rule;

import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.modules.reminder.ReminderRule;
import org.olat.modules.reminder.RuleEditorFragment;
import org.olat.modules.reminder.RuleSPI;
import org.olat.modules.reminder.model.ReminderRuleImpl;
import org.olat.modules.reminder.ui.RepositoryEntryLifecycleAfterValidRuleEditor;
import org.olat.repository.RepositoryEntry;

import java.util.Locale;

abstract class AbstractBeforeAfterRuleSPI implements RuleSPI {
    @Override
    public String getLabelI18nKey() {
        return "rule.lifecycle.valid.to";
    }

    @Override
    public int getSortValue() {
        return 4;
    }

    @Override
    public String getStaticText(ReminderRule rule, RepositoryEntry entry, Locale locale) {
        if (rule instanceof ReminderRuleImpl) {
            ReminderRuleImpl r = (ReminderRuleImpl)rule;
            Translator translator = Util.createPackageTranslator(RepositoryEntryLifecycleAfterValidRuleEditor.class, locale);
            String currentUnit = r.getRightUnit();
            String currentValue = r.getRightOperand();

            if (currentValue == null) {
                return null;
            }
            String i18nBeforeAfter = "after.";
            if (currentValue.startsWith("-")) {
                i18nBeforeAfter = "before.";
                currentValue = currentValue.substring(1);
            }

            try {
                LaunchUnit.valueOf(currentUnit);
            } catch (Exception e) {
                return null;
            }

            String[] args = new String[] { currentValue };
            return translator.translate(getLabelI18nKey() + "." + i18nBeforeAfter + currentUnit, args);
        }
        return null;
    }

    @Override
    public ReminderRule clone(ReminderRule rule, CourseEnvironmentMapper envMapper) {
        return rule.clone();
    }

    @Override
    public RuleEditorFragment getEditorFragment(ReminderRule rule, RepositoryEntry entry) {
        return new RepositoryEntryLifecycleAfterValidRuleEditor(rule, this.getClass().getSimpleName());
    }
}
