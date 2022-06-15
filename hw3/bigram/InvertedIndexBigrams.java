import java.io.*;
import java.util.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InvertedIndexBigrams {
	
	public static class BMapper extends Mapper<Object, Text, Text, Text> {
		
		private Text documentID = new Text();
		private Text words = new Text();
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] document = value.toString().split("\t", 2);
			String id = document[0];
			String content = document[1];
			documentID.set(id);
			String normalizedContent = content.toLowerCase().replaceAll("[^a-z ]", "").replaceAll("[ ]+", " ");
			StringTokenizer tokenizer = new StringTokenizer(normalizedContent);
			String curr = "";
			if (tokenizer.hasMoreTokens()) {
				curr = tokenizer.nextToken();
			}
			while (tokenizer.hasMoreTokens()) {
				String next = tokenizer.nextToken();
				words.set(curr + " " + next);
				curr = next;
				context.write(words, documentID);
			}
		}
	}
	
	public static class BReducer extends Reducer<Text, Text, Text, Text> {
		
		private Text invertedIndex = new Text();
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			Map<String, Integer> map = new HashMap<>();
			for (Text value : values) {
				String temp = value.toString();
				map.put(temp, map.getOrDefault(temp, 0) + 1);
			}
			StringBuffer sb = new StringBuffer();
			for (String id : map.keySet()) {
				sb.append(id).append(":").append(map.get(id)).append(" ");
			}
			invertedIndex.set(sb.toString());
			context.write(key, invertedIndex);
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		if (args.length != 2) {
			System.err.println("Usage: Inverted Index Bigrams <input path> <output path>");
			System.exit(-1);
		}
		Job job = new Job();
		job.setJarByClass(InvertedIndexBigrams.class);
		job.setJobName("Inverted Index Bigrams");
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setMapperClass(BMapper.class);
		job.setReducerClass(BReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.waitForCompletion(true);
	}
}
