package HBaseIA.TwitBase.filters;

import HBaseIA.TwitBase.hbase.UsersDAO;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import java.io.IOException;

import static org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import static org.apache.hadoop.hbase.filter.RegexStringComparator.EngineType;


public class PasswordStrengthFilterExample {

    private static final int MIN_LENGTH = 3;
    private static final String MIN_LENGTH_REGEX = String.format(".{%s,}", MIN_LENGTH);

    public static void main(String[] args) {

        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.property.clientPort", "5181");

        try {
            Connection connection = ConnectionFactory.createConnection(conf);
            Table t = connection.getTable(UsersDAO.TABLE_NAME);
            Scan scan = new Scan();
            scan.addColumn(UsersDAO.INFO_FAM, UsersDAO.PASS_COL);
            scan.addColumn(UsersDAO.INFO_FAM, UsersDAO.NAME_COL);
            scan.addColumn(UsersDAO.INFO_FAM, UsersDAO.EMAIL_COL);

            SingleColumnValueFilter filter = new SingleColumnValueFilter(UsersDAO.INFO_FAM, UsersDAO.PASS_COL,
                    CompareOp.EQUAL, new RegexStringComparator(MIN_LENGTH_REGEX, EngineType.JAVA));

            scan.setFilter(filter);

            ResultScanner rs = t.getScanner(scan);
            for (Result r : rs) {
                System.out.println(r);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
