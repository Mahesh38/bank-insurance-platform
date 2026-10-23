package com.bank.common.events;

/**
 * Who caused the change and in what capacity ({@code INV-ACT-04}). {@code actorInsurerId} and
 * {@code assistedActorId} are mandatory for an {@code INSURER_PARTNER_REP}.
 */
public record Actor(
    String id, String type, String actingCapacity, String actorInsurerId, String assistedActorId) {

  public static final String INSURER_PARTNER_REP = "INSURER_PARTNER_REP";

  public Actor {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("actor.id is required");
    }
    if (type == null || !type.matches("^[A-Z][A-Z_]{1,39}$")) {
      throw new IllegalArgumentException("actor.type must be UPPER_SNAKE");
    }
    if (actingCapacity != null
        && !actingCapacity.equals("SP_ACCOUNTABLE")
        && !actingCapacity.equals("ASSIST_ONLY")) {
      throw new IllegalArgumentException("actor.actingCapacity must be SP_ACCOUNTABLE|ASSIST_ONLY");
    }
    if (INSURER_PARTNER_REP.equals(type) && (actorInsurerId == null || assistedActorId == null)) {
      throw new IllegalArgumentException(
          "INSURER_PARTNER_REP needs actorInsurerId and assistedActorId (INV-ACT-04)");
    }
  }

  public static Actor of(String id, String type, String actingCapacity) {
    return new Actor(id, type, actingCapacity, null, null);
  }

  /** Background work with no human principal (relay, sweeper, TAT clock). */
  public static Actor system(String serviceId) {
    return new Actor(serviceId, "SYSTEM", null, null, null);
  }
}
