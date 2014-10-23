package edu.cmu.lti.f14.hw3.hw3_yanhe.annotators;

import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_yanhe.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yanhe.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yanhe.utils.Utils;

/**
 * Document Vector Annotator is to creates sparse term vectors. It 
 * construct a vector of tokens and update the tokenList in the CAS
 * 
 * @author yanhe
 * 
 */
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	/**
	 * createTermFreqVector calculate the frequency of each vector and 
	 * save the information in doc.
	 * 
	 * @author yanhe
	 * 
	 */
	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		//TO DO: use tokenize0 from above
		List<String> tokenizationList = tokenize0(doc.getText());
		
		//Create a new HashMap and store each word of the text inside it
		HashMap<String, Integer> tokenHM = new HashMap<String, Integer>();
		Iterator<String> tokenIter = tokenizationList.iterator();
		
		while(tokenIter.hasNext()) {
			String tokenNext = tokenIter.next();
			if(tokenHM.containsKey(tokenNext) == false) {
				tokenHM.put(tokenNext, 1);
			}
			else {
				tokenHM.put(tokenNext, tokenHM.get(tokenNext) + 1);
			}
			
		}
		
		//Create a map entry and set the type value for each token
		Set<Map.Entry<String, Integer>> tokenSet = tokenHM.entrySet();
		Iterator<Map.Entry<String, Integer>> tokenSetIter = tokenSet.iterator();
		List<Token> tokenList = new LinkedList<Token>();
		while(tokenSetIter.hasNext()) {
			Map.Entry<String, Integer> tokenEntry = tokenSetIter.next();
			Token token = new Token(jcas);
			token.setFrequency(tokenEntry.getValue());
			token.setText(tokenEntry.getKey());
			tokenList.add(token);
			
		}
		FSList tokenFSList = Utils.fromCollectionToFSList(jcas, tokenList);
		doc.setTokenList(tokenFSList);
		//tokenFSList.addToIndexes(jcas);

	}

}