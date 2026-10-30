package com.candidatae.scheduler;

/**
 * Domain event published when a user successfully logs in.
 */
public record UserLoggedInEvent(String email) {
}

