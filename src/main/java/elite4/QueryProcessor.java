import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;

//import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
//import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
//import edu.stanford.nlp.ling.CoreLabel;
//import edu.stanford.nlp.pipeline.Annotation;
//import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class QueryProcessor {

//	private StanfordCoreNLP pipeline;
	
	private File documentsDir;
	private HashSet<String> regions;
	
	private TermContextMatrix tcMatrix;
	private List<Bucket> buckets;
	
	public QueryProcessor(File documentsDir, HashSet<String> regions) {
		this.documentsDir = documentsDir;
		this.regions = regions;
		
		// Setup NLP library
//		Properties props = new Properties();
//		props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
//		pipeline = new StanfordCoreNLP(props);
		
		try {
			
			// Build matrix with small window
			TermContextMatrix matrix = new TermContextMatrix(2);
			matrix.Build(documentsDir.getAbsolutePath(), null);
			
			// Aggregate tokens
			TokenMerger merger = new TokenMerger(matrix.index.keySet(), 0.9f);
			HashSet<String> multiwordTokens = merger.aggregate(matrix);
			
			// Build new matrix with large window
			matrix = null;
			matrix = new TermContextMatrix(10);
			matrix.Build(documentsDir.getAbsolutePath(), multiwordTokens);
			this.tcMatrix = matrix;
			
			// Create buckets
			SkillBuckets skillBuckets = new SkillBuckets(multiwordTokens, 0.8f);
			this.buckets = skillBuckets.cluster(matrix);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public String[] RunQuery(String query) {
		
		// Preprocess
		String processedQuery = query.toLowerCase();//PreProcess(query);
		
		// Find any regions
//		String[] regions = FindRegions(processedQuery);
		
		// Remove region from query string
//		for(String region : regions)
//			processedQuery = processedQuery.replace(region, "").replaceAll("\\s++",  " ");
		
		// Index documents
		//File[] documents = IndexDocuments(processedQuery, new String[0], documentsDir.listFiles());
		
		// Extract top K skill words from matched documents
		String[] topSkills = ExtractSkills(processedQuery);
		
		return topSkills;
	}
	
	public String[] ExtractSkills(String processedQuery) {
		
		// Split query
		String[] s = this.tcMatrix.index.keySet().toArray(new String[this.tcMatrix.index.keySet().size()]);
		String[] split = TermContextMatrix.splitByMultiWordTokens(processedQuery.split(" "), s);
		
		// Calculate similarity of each bucket
		PriorityQueue<Map.Entry<Bucket, Float>> sims = new PriorityQueue<>(new Comparator<Map.Entry<Bucket, Float>>() {
			@Override
			public int compare(Map.Entry<Bucket, Float> arg0, Map.Entry<Bucket, Float> arg1) {
				return -arg0.getValue().compareTo(arg1.getValue());
			}
		});
		for(Bucket b : buckets) {
			float sum = 0f;
			int count = 0;
			for(String w : b.getMembers()) {
				for(String t : split) {
					sum += this.tcMatrix.calcSimilarity(w, t);
					count++;
				}
			}
			sims.offer(new AbstractMap.SimpleEntry<Bucket, Float>(b, (sum / count)));
		}
				
		// Get top bucket
		Bucket top = sims.poll().getKey();
		
		return top.getMembers().toArray(new String[top.getMembers().size()]);
	}
	
	public File[] IndexDocuments(String query, String[] regions, File[] documents) {
		
		// Cleanup query string
		String queryString = query.replaceAll("\\s++", " ").trim();
		
		HashSet<Integer> documentSet = new HashSet<>();
		
		// Index by region if necessary
		if(regions.length > 0) {
			
			try {

				// Read map file
				RandomAccessFile map = new RandomAccessFile("map.raf", "r");

				String record;
				int regionId = 0;
				for(String region : regions) {

					// Read first record
					map.seek(0L); // seek to (region position * record length)
					record = map.readLine().trim().replaceAll("\\s++", " ");
					regionId = Integer.parseInt(record.split(" ")[0]);
					
					int currentId = regionId;
					do {
						
						// Add document to list
						documentSet.add(Integer.parseInt(record.split(" ")[1]));
						
						// Get next record
						record = map.readLine().trim().replaceAll("\\s++", " ");
						regionId = Integer.parseInt(record.split(" ")[0]);
						
					} while(currentId == regionId);					
				}				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Index documents from query		
		try {
			
			BufferedReader br;
			String[] s = this.tcMatrix.index.keySet().toArray(new String[this.tcMatrix.index.keySet().size()]);
			int index = 0;
			
			for(File f : documents) {
				br = new BufferedReader(new FileReader(f));
				String line;
				String[] a;
				boolean continueFile = true;
				while((line = br.readLine()) != null && continueFile) {
					a = TermContextMatrix.splitByMultiWordTokens(line.split(" "), s);
					for(int i = 0; i < a.length; i++) {
						if(query.contains(a[i])) {
							documentSet.add(index);
							continueFile = false;
						}
					}
				}				
				index++;
			}
			
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// Get files from document id list
		List<File> matchedDocuments = new ArrayList<>();
		for(int i = 0; i < documents.length; i++) {
			if(documentSet.contains(i)) {
				matchedDocuments.add(documents[i]);
			}
		}
		
		return matchedDocuments.toArray(new File[matchedDocuments.size()]);
	}
	
	public String[] FindRegions(String query) {
		
		List<String> regions = new ArrayList<>();
		
		// Loop over query
		String[] tokens = query.split(" ");
		for(String t : tokens) {
			if(regions.contains(t)) {
				regions.add(t);
			}
		}
		
		return regions.toArray(new String[regions.size()]);		
	}
	
//	public String PreProcess(String query) {
//		
//		// Lemmatize
//		Annotation annotation = new Annotation(query.toLowerCase());
//		pipeline.annotate(annotation);
//		List<CoreLabel> tokenLabels = annotation.get(TokensAnnotation.class);
//		
//		StringBuilder processedQuery = new StringBuilder();
//		String lemma;
//		for(CoreLabel l : tokenLabels) {
//			lemma = l.get(LemmaAnnotation.class);
//			processedQuery.append(lemma + " ");
//		}
//		
//		return processedQuery.toString().trim();
//	}
	
}
