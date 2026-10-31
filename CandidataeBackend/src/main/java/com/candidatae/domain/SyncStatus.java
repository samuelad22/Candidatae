package com.candidatae.domain;

/**
 * Current synchronization state of an email account.
 */
public enum SyncStatus {

    /**
     * Waiting for the next scheduled or manual synchronization.
     */
    IDLE,

    /**
     * Synchronization is currently in progress.
     */
    SYNCING,

    /**
     * The last synchronization completed successfully.
     */
    SUCCESS,

    /**
     * The last synchronization failed due to authentication or network errors.
     */
    ERROR
}

