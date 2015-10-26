import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCountPerMapTally {
	public static class TokenizerMapper
    extends Mapper<Object, Text, Text, IntWritable>{

	 private final static IntWritable one = new IntWritable(1);
	 private Text word = new Text();
	 private String tempword;
	/*The key is the offset to the line in the file and value is line itself in the document*/
	 public void map(Object key, Text value, Context context
	                 ) throws IOException, InterruptedException {
	   StringTokenizer itr = new StringTokenizer(value.toString());
	   /*In this approach we define a HashMap to store the count of each word instead 
	   of emitting (word, one) each time*/
	   HashMap<String, Integer> dict = new HashMap<String, Integer>();
	   while (itr.hasMoreTokens()) {
	     word.set(itr.nextToken());
	     tempword = word.toString();
	     tempword = tempword.toLowerCase();
	     if(tempword.startsWith("p") || tempword.startsWith("q") ||
	     		tempword.startsWith("m") || tempword.startsWith("n") || tempword.startsWith("o"))
	     {
	    	if(!dict.containsKey(tempword))
	    	    dict.put(tempword, 1);
	    	else
	    		dict.put(tempword, dict.get(tempword) + 1);
	     }
	   }
	   for(String tempword : dict.keySet())
	   {
	       /*Instead of emitting one we emit the word with the total count 
		   */
		   Text wordToWrite = new Text(tempword); 
		   context.write(wordToWrite, new IntWritable(dict.get(tempword)));
	   }
	 }
	}
    /*
	Implemented a custom partitioner to determine which intermediate keys are assigned to which reducer task.
	In this scenario we assign words starting with m to M to Reducer 0.
	Taking mod helps avoid the divide by zero error when lesser no. of reducers are available.
	*/
	public static class WCPartitioner extends Partitioner<Text, IntWritable>{

		@Override
		public int getPartition(Text key, IntWritable value, int numPartitions) {
			String tempWord = key.toString();
			char letter = tempWord.toLowerCase().charAt(0);
			int partitionNumber = 0;
			switch(letter){
				case 'm': partitionNumber = 0 % numPartitions; break;
				case 'n': partitionNumber = 1 % numPartitions; break;
				case 'o': partitionNumber = 2 % numPartitions; break;
				case 'p': partitionNumber = 3 % numPartitions; break;
				case 'q': partitionNumber = 4 % numPartitions; break;
			}
			return partitionNumber;
		}
	}
	public static class IntSumReducer
	    extends Reducer<Text,IntWritable,Text,IntWritable> {
	 private IntWritable result = new IntWritable();
	
	 public void reduce(Text key, Iterable<IntWritable> values,
	                    Context context
	                    ) throws IOException, InterruptedException {
	   int sum = 0;
	   for (IntWritable val : values) {
	     sum += val.get();
	   }
	   result.set(sum);
	   context.write(key, result);
	 }
	}
	
	public static void main(String[] args) throws Exception {
	 Configuration conf = new Configuration();
	 Job job = Job.getInstance(conf, "word count");
	 job.setJarByClass(WordCountPerMapTally.class);
	 job.setNumReduceTasks(5);
	 job.setMapperClass(TokenizerMapper.class);
	 job.setPartitionerClass(WCPartitioner.class);
	 /* We do not set the combiner*/
	 //job.setCombinerClass(IntSumReducer.class);
	 job.setReducerClass(IntSumReducer.class);
	 job.setOutputKeyClass(Text.class);
	 job.setOutputValueClass(IntWritable.class);
	 FileInputFormat.addInputPath(job, new Path(args[0]));
	 FileOutputFormat.setOutputPath(job, new Path(args[1]));
	 System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
