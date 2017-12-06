package HBaseIA.TwitBase.mapreduce;

import HBaseIA.TwitBase.hbase.TwitsDAO;
import HBaseIA.TwitBase.hbase.UsersDAO;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;

import java.util.Iterator;
import java.util.Random;

public class HamletTagger {

    public static class Map
            extends TableMapper<ImmutableBytesWritable, Put> {

        public static enum Counters {HAMLET_TAGS}

        private Random rand;

        private boolean mentionsHamlet(String msg) {
            return rand.nextBoolean();
        }

        protected void setup(Context context) {
            rand = new Random(System.currentTimeMillis());
        }

        protected void map(
                ImmutableBytesWritable rowkey,
                Result result,
                Context context) {

            Cell cell = result.getColumnLatestCell(TwitsDAO.TWITS_FAM, TwitsDAO.TWIT_COL);
            byte[] b = CellUtil.cloneValue(cell);
            String msg = Bytes.toString(b);

            cell = result.getColumnLatestCell(TwitsDAO.TWITS_FAM, TwitsDAO.USER_COL);
            b = CellUtil.cloneValue(cell);
            String user = Bytes.toString(b);

            if (mentionsHamlet(msg)) {
                Put p = UsersDAO.mkPut(
                        user,
                        UsersDAO.INFO_FAM,
                        UsersDAO.HAMLET_COL,
                        Bytes.toBytes(true));
                ImmutableBytesWritable outkey =
                        new ImmutableBytesWritable(p.getRow());
                try {
                    context.write(outkey, p);
                    context.getCounter(Counters.HAMLET_TAGS).increment(1);
                } catch (Exception e) {
                    // gulp!
                }
            }
        }
    }

    public static class Reduce
            extends TableReducer<
            ImmutableBytesWritable,
            Put,
            ImmutableBytesWritable> {

        @Override
        protected void reduce(
                ImmutableBytesWritable rowkey,
                Iterable<Put> values,
                Context context) {
            Iterator<Put> i = values.iterator();
            if (i.hasNext()) {
                try {
                    context.write(rowkey, i.next());
                } catch (Exception e) {
                    // gulp!
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Job job = new Job(conf, "TwitBase Hamlet tagger");
        job.setJarByClass(HamletTagger.class);

        Scan scan = new Scan();
        scan.addColumn(TwitsDAO.TWITS_FAM, TwitsDAO.USER_COL);
        scan.addColumn(TwitsDAO.TWITS_FAM, TwitsDAO.TWIT_COL);
        TableMapReduceUtil.initTableMapperJob(
                Bytes.toString(TwitsDAO.TABLE_NAME_BYTES),
                scan,
                Map.class,
                ImmutableBytesWritable.class,
                Put.class,
                job);
        TableMapReduceUtil.initTableReducerJob(
                Bytes.toString(UsersDAO.TABLE_NAME_BYTES),
                IdentityTableReducer.class,
                job);

        job.setNumReduceTasks(0);
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
