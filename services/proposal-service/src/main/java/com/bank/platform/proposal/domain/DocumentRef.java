package com.bank.platform.proposal.domain;

/** A reference to a document in S3 — bytes never enter the envelope (schemas/20 §2). */
public record DocumentRef(String documentId, String objectRef, String documentClass) {}
