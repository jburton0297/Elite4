import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * The purpose of this class is to merge various tokens in a given set of pre-tokenized documents
 * in order to both reduce the amount of unique tokens and to appropriately reference multiple tokens as a single, whole token.  
 * @author Jacob Burton
 *
 */
public class TokenMerger {

	// NOTE: Set window size very small for syntactic similarity rather than semantic similarity
	
	private Set<String> tokens;
	private float similarityThreshold;
	
	public TokenMerger(Set<String> tokens, float similarityThreshold) {
		this.tokens = tokens;
		this.similarityThreshold = similarityThreshold;
	}
	
	public HashSet<String> aggregate(TermContextMatrix matrix) {
		
		// Initialize hash set for final tokens
		HashSet<String> tokenSet = new HashSet<>();
		
		String previousToken = null;
		float similarity = 0f;
		for(String token : tokens) {
			
			// Add current token to hash set
			tokenSet.add(token);
			
			if(previousToken != null) {
				
				// If previous token is multi word, analyze each individual token
				if(StringUtils.contains(previousToken, " ")) {
					
					String[] t = StringUtils.split(previousToken, " ");
					float sum = 0f;
					for(String s : t) {
						
						// Calculate similarity between s and token
						sum += matrix.calcSimilarity(s, token);
					}
					
					// Calculate average similarity between all words
					similarity = sum / (float)t.length;

				} else {

					// Calculate similarity between token and previous token
					similarity = matrix.calcSimilarity(token, previousToken);
				}

				// If similarity is strong enough, add conjoined tokens to the set
				String newToken;
				if(similarity >= this.similarityThreshold) {
					newToken = previousToken + " " + token;
					tokenSet.add(newToken);
					
					previousToken = newToken;
				} else {
					previousToken = token;
				}						
			}
		}
		
		return tokenSet;	
	}
	
}
