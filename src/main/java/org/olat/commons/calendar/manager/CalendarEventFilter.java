package org.olat.commons.calendar.manager;

import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.core.id.Identity;

import java.util.List;

public interface CalendarEventFilter {

    List<KalendarEvent> filterEvents(Identity identity, List<KalendarEvent> events);

}
