import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The purpose of this class is to take a broad group of words related to jobs and skills cluster them into specific groups or fields.
 * @author jacob
 *
 */
public class SkillBuckets {
	
	// NOTE: Set window size very large for semantic similarity rather than syntactic similarity

	private HashSet<String> tokens;
	private float similarityThreshold;
	private List<Bucket> buckets;
	
	public SkillBuckets(HashSet<String> tokens, float similarityThreshold) {
		this.tokens = tokens;
		this.similarityThreshold = similarityThreshold;
		this.buckets = new ArrayList<>();
	}
	
	public List<Bucket> cluster(TermContextMatrix matrix) {
		
		// Loop through each token
		String rootToken = null;
		HashSet<String> otherTokens = tokens;
		Bucket currentBucket = new Bucket();
		while(otherTokens.size() > 0) {
			for(String token : otherTokens) {
				
				// Check if current bucket is empty
				if(currentBucket.getMembers().size() == 0) {
					
					// Set current token as root token of current bucket
					currentBucket.addMember(token);
					rootToken = token;
					
					// Remove token from otherTokens set
					otherTokens.remove(token);
					
				} else {
					
					// Calculate similarity between root token and token
					float similarity = matrix.calcSimilarity(rootToken, token);
					if(similarity >= this.similarityThreshold) {
						
						// Token is similar enough, add to current bucket
						currentBucket.addMember(token);
						
						// Remove token from otherTokens set
						otherTokens.remove(token);
					}				
				}
			}
			
			// Reset current bucket
			this.buckets.add(currentBucket);
			currentBucket = new Bucket();			
		}
		
		return buckets;		
	}
	
}