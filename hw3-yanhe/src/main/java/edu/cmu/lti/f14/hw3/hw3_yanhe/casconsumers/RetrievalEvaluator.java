package edu.cmu.lti.f14.hw3.hw3_yanhe.casconsumers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_yanhe.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yanhe.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yanhe.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	//QID list
	public LinkedHashSet<Integer> QIDList;
	//QID to token in query
	public HashMap<Integer, Map<String, Integer>> QIDquery;
	//QID to tokens in document with rel=1
	public HashMap<Integer, Map<String, Integer>> QIDdocu1;
	//QID to list of tokens in document with rel=0
	public HashMap<Integer, ArrayList<Map<String, Integer>>>QIDdocu0;
	//QID to score with rel=1
	public HashMap<Integer, Double> QIDscore1;
	//QID to scorewith rel=0
	public HashMap<Integer, ArrayList<Double>> QIDscore0;
	//QID to rank
	public HashMap<Integer, Double> rank;
	//QID to text
	public HashMap<Integer, String> text;

	DecimalFormat ft = new DecimalFormat("#0.0000");

		
	public void initialize() throws ResourceInitializationException {

		QIDList = new LinkedHashSet<Integer>();
		QIDquery = new HashMap<Integer, Map<String, Integer>>();
		QIDdocu1 = new HashMap<Integer, Map<String, Integer>>();
		QIDdocu0 = new HashMap<Integer, ArrayList<Map<String, Integer>>>();
		QIDscore1 = new HashMap<Integer, Double>();
		QIDscore0 = new HashMap<Integer, ArrayList<Double>>();
		rank = new HashMap<Integer, Double>();
		text = new HashMap<Integer, String>();

	}

	/**
	 * TODO :: 1. construct the global word dictionary 
	 * 2. keep the word frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
		
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure the former annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			ArrayList<Token>tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

			int qid = doc.getQueryID();
			int rel = doc.getRelevanceValue();
			QIDList.add(qid);
			//relList.add(doc.getRelevanceValue());
						
			Map<String, Integer> tokens = new HashMap<String, Integer>();
			Iterator<Token> iter = tokenList.iterator();
			while(iter.hasNext()){
				Token t = (Token)iter.next();
				tokens.put(t.getText(), t.getFrequency());
			}
			if(rel == 99){
				QIDquery.put(qid, tokens);
			}
			else if (rel == 1){
				QIDdocu1.put(qid, tokens);
				text.put(qid, doc.getText());
			}
			else{
				if(!QIDdocu0.containsKey(qid)){
					QIDdocu0.put(qid, new ArrayList<Map<String, Integer>>());		
				}
				QIDdocu0.get(qid).add(tokens);	
			}
		}
	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 
	 * 2. Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);

		// TODO :: compute the cosine similarity measure
		Iterator it = QIDList.iterator();
		while(it.hasNext()){
			int qid = (Integer) it.next(); 
			Map<String, Integer> query = QIDquery.get(qid);
			
			//for rel=1
			Map<String, Integer> doc1 = QIDdocu1.get(qid);
			QIDscore1.put(qid, computeCosineSimilarity(query, doc1));	
			//QIDscore1.put(qid, jaccardSimilarity(query, doc1));	
			//QIDscore1.put(qid, diceSimilarity(query, doc1));
			//QIDscore1.put(qid, tfidfSimilarity(query, doc1));
			
			//for rel=0
			ArrayList<Map<String, Integer>> doc0list = QIDdocu0.get(qid);
			Iterator iter = doc0list.iterator();
			while(iter.hasNext()){
				Map<String, Integer> doc0 = (Map<String, Integer>) iter.next();
				if(!QIDscore0.containsKey(qid)){
					QIDscore0.put(qid, new ArrayList<Double>());
				}
				QIDscore0.get(qid).add(computeCosineSimilarity(query, doc0));
				//QIDscore0.get(qid).add(jaccardSimilarity(query, doc0));
				//QIDscore0.get(qid).add(diceSimilarity(query,doc0));
				//QIDscore0.get(qid).add(tfidfSimilarity(query, doc0));
			}	
		}	
		
		// TODO :: compute the rank of retrieved sentences
		for(Integer qid : QIDList){
			Collections.sort(QIDscore0.get(qid), new Comparator<Double>(){
				public int compare(Double d1, Double d2){
					if(d1 < d2)
						return 1;
					else
						return -1;
				}
			});
			
		    Double Score = QIDscore1.get(qid);
		    int i;
		    for(i = 0; i < QIDscore0.get(qid).size(); i++) {
		    	if (Score > QIDscore0.get(qid).get(i)) {
		    		//System.out.println("rev1  "+ Score);
		    		//System.out.println("rev0  "+ QIDscore0.get(qid).get(i));
		    		//System.out.println(QIDscore0.get(qid));
		    		break;
		        }
		      }
		    rank.put(qid, 1.0 + i);
		}

		for(Integer qid : QIDList){
		    System.out.println("cosine=" + ft.format(QIDscore1.get(qid)) + "\t" + "rank=" + 
		    		rank.get(qid).intValue() + "\t" + "qid=" + qid + "\t" + "rel=1" +"\t" + text.get(qid));
		}

		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr();
		System.out.println("MRR=" + ft.format(metric_mrr));
		
		write();
	}
	
	/**
	 * Write the output in standard format into file.
	 * @throws IOException
	 */
	public void write() throws IOException{
		File out = new File("report.txt");
		BufferedWriter buf = null;
		try {
			buf = new BufferedWriter(new FileWriter(out));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Integer qid : QIDList){
			buf.write("cosine=" + ft.format(QIDscore1.get(qid)) + "\t" + "rank=" + 
		    		rank.get(qid).intValue() + "\t" + "qid=" + qid + "\t" + "rel=1" +"\t" + text.get(qid));
			buf.newLine();
		}
		buf.write("MRR=" + ft.format(compute_mrr()));;
		buf.flush();
	}
	/**
	 * Compute cosine similarity of two vectors.
	 * First compute then length(magnitude) of the two vectors, which presented in maps.
	 * Then compute the dot product of the two vectors.
	 * Finally compute the cosine similarity.
	 * @param queryVector
	 * @param docVector
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		
		double cosine_similarity = 0.0;
		
		// TODO :: compute cosine similarity between two sentences
		
		double multisum = 0.0;
		double x = 0.0;
		double y = 0.0;
		double xSquare = 0.0;
		double ySquare = 0.0;
		Set<Map.Entry<String, Integer>> querySet = queryVector.entrySet();
		Iterator<Map.Entry<String, Integer>> querySetIter = querySet.iterator();
		while(querySetIter.hasNext()) {
			String token = querySetIter.next().getKey();
			x = queryVector.get(token);
			xSquare = xSquare+x*x;
			if (docVector.containsKey(token)){
				y = docVector.get(token);
				multisum = multisum+x*y;
			}
		}
		Set<Map.Entry<String, Integer>> docSet = docVector.entrySet();
		Iterator<Map.Entry<String, Integer>> docSetIter = docSet.iterator();
		while(docSetIter.hasNext()) {
			String token = docSetIter.next().getKey();
			y = docVector.get(token);
			ySquare = ySquare+y*y;
		}
		cosine_similarity = multisum/Math.sqrt(xSquare)/Math.sqrt(ySquare);

		return cosine_similarity;
	}
	/**
	 * Compute the magnitude of the vector.
	 * @param map
	 * @return len
	 */
	private double getlen(Map<String, Integer> map){
		double len = 0.0;
		Set<String> s = map.keySet();
		Iterator<String> it= s.iterator();
		while(it.hasNext()){
			String t = it.next();
			len += map.get(t) * map.get(t);
		}
		len = Math.sqrt(len);
		return len;
	}
	
	/**
	 * Compute jaccard similarity of two vectors
	 * Compute the intersection and union set of two vectors to get jaccard Similarity.
	 * @param queryVector
	 * @param docVector
	 * @return jaccardSimilarity 
	 */
	private double jaccardSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector){
		double jaccardSimilarity = 0.0;
		Set<String> union = new HashSet<String>();
		Set<String> sq = queryVector.keySet();
		Iterator<String> it = sq.iterator();
		int a = 0, b = 0;
		while(it.hasNext()){
			String t = it.next();
			if(docVector.containsKey(t)){
				if(docVector.get(t) == queryVector.get(t))
					a++;
			}
			else
				b++;
		}
		jaccardSimilarity = (double) a / (double) (a+b);
		return jaccardSimilarity;
	}
	/**
	 * Compute dice similarity of two vectors
	 * @param queryVector
	 * @param docVector
	 * @return
	 */
	private double diceSimilarity(Map<String, Integer> queryVector, 
			Map<String, Integer> docVector){
		double diceSimilarity = 0.0;
		Set<String> union = new HashSet<String>();
		Set<String> sq = queryVector.keySet();
		Iterator<String> it = sq.iterator();
		int a = 0, b = 0;
		while(it.hasNext()){
			String t = it.next();
			if(docVector.containsKey(t)){
				if(docVector.get(t) == queryVector.get(t))
					a++;
			}
			else
				b++;
		}
		diceSimilarity= (double)(2*a) / (double) (a+b);
		return diceSimilarity;                                                                                                                 
	}
	/**
	 * Compute tfidf similarity of two vectors
	 * tf(t,q): raw frequency used 
	 * @param queryVector
	 * @param docVector
	 * @return
	 */
	private double tfidfSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector){
		double tfidf = 0.0;
		double tf = 0.0;
		double idf = 0.0;
		int corpus = docVector.size();
		Set<String> sd = docVector.keySet();
		Iterator<String> iter = sd.iterator();
		while(iter.hasNext()){
			String temp = iter.next();
			if(queryVector.containsKey(temp)){
				idf += 1;
				tf += docVector.get(temp);
			}
		}
		idf = Math.log(corpus/(1 + idf));  
		tfidf = tf * idf; 
		return tfidf;
	}

	/**
	 * Compute the mean reciprocal rank of the text collection. 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr = 0.0;

		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		
		Iterator it = QIDList.iterator();
		int count = 0;
		while(it.hasNext()){
			metric_mrr += 1 / rank.get(it.next());
			count++;
		}
		return metric_mrr/count;
	}

}