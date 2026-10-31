package com.candidatae.integration.imap;

import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Factory for creating Jakarta Mail {@link Store} instances.
 * Extracted to allow clean unit testing without opening real network sockets.
 */
@Component
public class ImapStoreFactory {

    public Store createStore(Properties properties, String protocol) throws NoSuchProviderException {
        Session session = Session.getInstance(properties);
        return session.getStore(protocol);
    }
}

