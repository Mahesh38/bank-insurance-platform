/// Opaque, bank-minted identifiers.
///
/// Implements ID-01 and ID-02 from `01-domain-model-and-invariants.md` §3.1:
/// every aggregate root carries a bank-minted, opaque identifier, and
/// cross-context references are by root identifier plus context — never by an
/// embedded object graph.
///
/// These are distinct types rather than bare `String`s so that passing a
/// `LeadId` where a `QuoteId` is expected is a compile error rather than a
/// runtime defect.
library;

abstract class EntityId {
  final String value;
  const EntityId(this.value);

  @override
  bool operator ==(Object other) =>
      other.runtimeType == runtimeType && other is EntityId && other.value == value;

  @override
  int get hashCode => Object.hash(runtimeType, value);

  @override
  String toString() => '$runtimeType($value)';
}

class CustomerId extends EntityId {
  const CustomerId(super.value);
}

class LeadId extends EntityId {
  const LeadId(super.value);
}

class JourneyId extends EntityId {
  const JourneyId(super.value);
}

class ConsentId extends EntityId {
  const ConsentId(super.value);
}

class SuitabilityId extends EntityId {
  const SuitabilityId(super.value);
}

class QuoteId extends EntityId {
  const QuoteId(super.value);
}

class OfferId extends EntityId {
  const OfferId(super.value);
}

class ProposalId extends EntityId {
  const ProposalId(super.value);
}

class PaymentId extends EntityId {
  const PaymentId(super.value);
}

class PolicyId extends EntityId {
  const PolicyId(super.value);
}

class ActorId extends EntityId {
  const ActorId(super.value);
}

/// A masked mobile number. The RM app never holds a full customer mobile —
/// it holds enough to say "ending 42" and nothing more.
///
/// Supports `AC-PAY-010-2` and the `WaitingPanel` component in S05 §4.6, which
/// must display the last two digits so the RM has something to say while the
/// customer completes an action on their own device.
class MaskedMobile {
  final String lastTwo;
  const MaskedMobile(this.lastTwo);

  @override
  String toString() => '••••••••$lastTwo';
}
