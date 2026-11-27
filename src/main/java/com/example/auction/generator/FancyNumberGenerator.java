package com.example.auction.generator;

import java.util.Locale;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGenerator;

public class FancyNumberGenerator implements IdentifierGenerator {
    private static final String FIND_MAXIMUM_ID_SQL =
            "SELECT MAX(CAST(id AS INTEGER)) FROM booking";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        Integer maximum = session.createNativeQuery(FIND_MAXIMUM_ID_SQL, Integer.class).getSingleResult();
        int next = maximum == null ? 1 : maximum + 1;
        if (next > 9_999) {
            throw new IdentifierGenerationException("Booking fancy IDs are limited to four digits");
        }
        return String.format(Locale.ROOT, "%04d", next);
    }
}
