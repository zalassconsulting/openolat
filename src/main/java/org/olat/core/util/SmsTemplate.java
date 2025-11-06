package org.olat.core.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.olat.core.id.Identity;
import org.olat.core.util.mail.MailTemplate;

public class SmsTemplate {

    private static final VelocityEngine engine;

    static {
        Properties props = new Properties();
        props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        engine = new VelocityEngine(props);
        engine.init();
    }

    public static String renderSmsTemplate(Identity recipient, MailTemplate template) {

        VelocityContext context = template.getContext() != null
                ? new VelocityContext(template.getContext())
                : new VelocityContext();

        template.putVariablesInMailContext(recipient);
        template.putMissingVariablesToMailContext();

        String body = template.getBodyTemplate();
        if (body == null) {
            return "";
        }

        StringWriter writer = new StringWriter();
        engine.evaluate(context, writer, "SMS", new StringReader(body));

        return writer.toString();
    }

}
