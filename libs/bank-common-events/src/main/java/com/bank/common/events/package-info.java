/**
 * ADR-025 core: envelope, codec, ports, topic naming, idempotent consumer. Plain Java + Jackson —
 * no Spring, no JDBC, no Kafka (adapters live in {@code jdbc}, {@code kafka}, {@code spring}).
 */
package com.bank.common.events;
