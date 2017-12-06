package HBaseIA.TwitBase.hbase;

import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import utils.Md5Utils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class TwitsDAO {

    public static final byte[] TABLE_NAME_BYTES = Bytes.toBytes("/twits");
    public static final TableName TABLE_NAME = TableName.valueOf(TABLE_NAME_BYTES);
    public static final byte[] TWITS_FAM = Bytes.toBytes("twits");

    public static final byte[] USER_COL = Bytes.toBytes("user");
    public static final byte[] TWIT_COL = Bytes.toBytes("twit");
    private static final int longLength = 8; // bytes

    private Connection connection;

    private static final Logger log = Logger.getLogger(TwitsDAO.class);

    public TwitsDAO(Connection connection) {
        this.connection = connection;
    }

    private static byte[] mkRowKey(Twit t) {
        return mkRowKey(t.user, t.dt);
    }

    private static byte[] mkRowKey(String user, LocalDateTime dt) {
        byte[] userHash = Md5Utils.md5sum(user);
        byte[] timestamp = Bytes.toBytes(-1 * dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        byte[] rowKey = new byte[Md5Utils.MD5_LENGTH + longLength];

        int offset = 0;
        offset = Bytes.putBytes(rowKey, offset, userHash, 0, userHash.length);
        Bytes.putBytes(rowKey, offset, timestamp, 0, timestamp.length);
        return rowKey;
    }

    private static Put mkPut(Twit t) {
        Put p = new Put(mkRowKey(t));
        p.addColumn(TWITS_FAM, USER_COL, Bytes.toBytes(t.user));
        p.addColumn(TWITS_FAM, TWIT_COL, Bytes.toBytes(t.text));
        return p;
    }

    private static Get mkGet(String user, LocalDateTime dt) {
        Get g = new Get(mkRowKey(user, dt));
        g.addColumn(TWITS_FAM, USER_COL);
        g.addColumn(TWITS_FAM, TWIT_COL);
        return g;
    }

    private static String to_str(byte[] xs) {
        StringBuilder sb = new StringBuilder(xs.length * 2);
        for (byte b : xs) {
            sb.append(b).append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static Scan mkScan(String user) {
        byte[] userHash = Md5Utils.md5sum(user);
        byte[] startRow = Bytes.padTail(userHash, longLength); // 212d...866f00...
        byte[] stopRow = Bytes.padTail(userHash, longLength);
        stopRow[Md5Utils.MD5_LENGTH - 1]++;                      // 212d...867000...

        log.debug("Scan starting at: '" + to_str(startRow) + "'");
        log.debug("Scan stopping at: '" + to_str(stopRow) + "'");

        Scan s = new Scan(startRow, stopRow);
        s.addColumn(TWITS_FAM, USER_COL);
        s.addColumn(TWITS_FAM, TWIT_COL);
        return s;
    }

    public void postTwit(String user, LocalDateTime dt, String text) throws IOException {

        Table twits = connection.getTable(TABLE_NAME);

        Put p = mkPut(new Twit(user, dt, text));
        twits.put(p);

        twits.close();
    }

    public HBaseIA.TwitBase.model.Twit getTwit(String user, LocalDateTime dt) throws IOException {

        Table twits = connection.getTable(TABLE_NAME);

        Get g = mkGet(user, dt);
        Result result = twits.get(g);
        if (result.isEmpty())
            return null;

        Twit t = new Twit(result);
        twits.close();
        return t;
    }

    public List<HBaseIA.TwitBase.model.Twit> list(String user) throws IOException {

        Table twits = connection.getTable(TABLE_NAME);

        ResultScanner results = twits.getScanner(mkScan(user));
        List<HBaseIA.TwitBase.model.Twit> ret = new ArrayList<HBaseIA.TwitBase.model.Twit>();
        for (Result r : results) {
            ret.add(new Twit(r));
        }

        twits.close();
        return ret;
    }

    private static class Twit extends HBaseIA.TwitBase.model.Twit {

        private Twit(Result r) {
            this(CellUtil.cloneValue(r.getColumnLatestCell(TWITS_FAM, USER_COL)),
                    Arrays.copyOfRange(r.getRow(), Md5Utils.MD5_LENGTH, Md5Utils.MD5_LENGTH + longLength),
                    CellUtil.cloneValue(r.getColumnLatestCell(TWITS_FAM, TWIT_COL)));
        }

        private Twit(byte[] user, byte[] dt, byte[] text) {
            this(
                    Bytes.toString(user),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(-1 * Bytes.toLong(dt)), TimeZone.getDefault().toZoneId()),
                    Bytes.toString(text));
        }

        private Twit(String user, LocalDateTime dt, String text) {
            this.user = user;
            this.dt = dt;
            this.text = text;
        }
    }
}
