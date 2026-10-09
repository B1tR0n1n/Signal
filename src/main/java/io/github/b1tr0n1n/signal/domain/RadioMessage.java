package io.github.b1tr0n1n.signal.domain;

/**
 * An authored radio line. Text lives behind a translation key; ids are stable persistence keys.
 *
 * @param id         stable id, e.g. {@code bellwether/local_restored}
 * @param channel    stable channel id, e.g. {@code bellwether_maintenance}
 * @param speaker    stable character id, e.g. {@code nia}
 * @param textKey    translation key for the line
 */
public record RadioMessage(String id, String channel, String speaker, String textKey) {
}
