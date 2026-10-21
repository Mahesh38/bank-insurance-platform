package com.bank.platform.lead.adapter.memory;

import com.bank.platform.lead.domain.LeadActivityPort;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Test and local-only companion of {@link InMemoryLeadRepository} ({@code lead.store=memory}). */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "memory")
public class InMemoryLeadActivityStore implements LeadActivityPort {

  private final List<Assignment> assignments = new CopyOnWriteArrayList<>();
  private final List<Meeting> meetings = new CopyOnWriteArrayList<>();
  private final List<FollowUp> followUps = new CopyOnWriteArrayList<>();
  private final List<LeadEvent> events = new CopyOnWriteArrayList<>();

  @Override
  public void recordAssignment(Assignment assignment) {
    assignments.add(assignment);
  }

  @Override
  public void recordMeeting(Meeting meeting) {
    meetings.add(meeting);
  }

  @Override
  public void recordFollowUp(FollowUp followUp) {
    followUps.add(followUp);
  }

  @Override
  public void append(LeadEvent event) {
    events.add(event);
  }

  @Override
  public List<Assignment> assignmentsOf(String leadId) {
    return assignments.stream().filter(a -> a.leadId().equals(leadId)).toList();
  }

  @Override
  public List<LeadEvent> eventsOf(String leadId) {
    return events.stream().filter(e -> e.leadId().equals(leadId)).toList();
  }

  public List<Meeting> meetings() {
    return List.copyOf(meetings);
  }

  public List<FollowUp> followUps() {
    return List.copyOf(followUps);
  }
}
