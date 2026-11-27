package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__UseSequenceForBidderIds extends BaseJavaMigration {
    private static final long ALLOCATION_SIZE = 50;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SEQUENCE bidder_id_seq START WITH 50 INCREMENT BY 50");
            statement.execute("ALTER TABLE bidder ALTER COLUMN id DROP IDENTITY");
            statement.execute("ALTER TABLE bidder ALTER COLUMN id SET DEFAULT nextval('bidder_id_seq')");
        }

        long highestBidderId;
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(id), 0) FROM bidder")) {
            resultSet.next();
            highestBidderId = resultSet.getLong(1);
        }

        long nextSequenceValue = Math.max(
                ALLOCATION_SIZE,
                ((highestBidderId + (2 * ALLOCATION_SIZE) - 1) / ALLOCATION_SIZE) * ALLOCATION_SIZE);
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER SEQUENCE bidder_id_seq RESTART WITH " + nextSequenceValue);
        }
    }
}
