import java.awt.Color;
import java.awt.Label;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
/**
 * 
 */

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Khalilur
 *
 */
public class Search {

	/**
	 * 
	 */
	public String indexFile = "";
	public String postinglistFile = "";
	public String docmapFile = "";	
	public String termdocindexfile ="";
	public TreeMap<Integer, String> docmapindex = null;
	public TreeMap<String, Integer> postinglist = null;	
	public TreeMap<Integer, List<Integer>> documentlist = null;
	public TreeMap<Integer, TreeMap<Integer, List<Integer>>> termdocposindex = null;
	public TreeMap<Integer, List<Integer>> termdocindex = null;
	public int hits = 0;
	public String myQuery = "";
	public boolean relFlag=false;
	public boolean irrelFlag = false;
	public String relevance = "";
    public String irrelevance = "";
		
	public Search() {
		postinglistFile = "d:/search/postinglist.txt";
		docmapFile = "d:/search/docmap.txt";
		indexFile = "d:/search/PositionalIndex.zip";
		termdocindexfile = "d:/search/term_doc_index.txt";
		documentlist = new TreeMap<Integer, List<Integer>>();
		termdocposindex = new TreeMap<Integer, TreeMap<Integer,List<Integer>>>();
		termdocindex = new TreeMap<Integer, List<Integer>>();
		//getTermDocIndex(termdocindexfile);
		try {
			docmapindex = getDocsMap();
			postinglist = getPostingList();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		Search search = new Search();
		String query = "";
		System.out.println();		
		System.err.println("Options: ");
		System.err.println("1) Always begin with 'search'");
		System.err.println("2) Hit option to fetch Top K-N. e.g.: search [--hits=10] [query]");
		System.err.println("3) Relevance feedback,  e.g.: search [--relevant=(relevance document)] [query]");
		System.err.println("4) Irrelevance feedback ,  e.g.: search [--irrelevant=(relevance document)] [query]");
		System.err.println("5) OR Query option, e.g.: search [--OR] [query]");
		System.err.println("6) AND Query option, e.g.: search [--AND] [query]");
		System.out.println("Type your query 'search [your query]' below or type 'quit' to exit :");		
		System.out.println();
		
		InputStreamReader is = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(is);		
		while (!(query.equals("quit"))){
			query = br.readLine();
			if (!(query.equals("quit"))){
				search.checkArgument(query);				
			}						
		}
	}	
	
	public void checkArgument(String inputQuery) throws Exception{
		String[] query = inputQuery.split(" ");
		int i = 0, j;
        String arg;
        char flag;
        boolean hitFlag = false;
        boolean quoFlag = false;
        boolean andFlag = false;
        boolean orFlag = false;        
        boolean searchFlag = false;
        String outputfile = "";
        int no = 0;
        String terms = "";
        String unQuotedQuery="";
        hits=0;
        if (i == 0 && query[i].equals("search")){
        	searchFlag=true;
        	if (!(query.length==1)){
        	arg = query[i++];      
		        for (int l = i; l < query.length; l++){
		        	try {
		        		while (i < query.length && query[i].startsWith("-")) {
				            arg = query[i++];
				            
				            if (arg.equals("--AND")) {			            				                
				                andFlag = true;			                
				            }
				            
				            else if (arg.equals("--OR")) {  
				                orFlag = true;			                
				            }
				            else if (arg.substring(0, 7).equals("--hits=")) {			            	
				            	int length = arg.length();
				            	no = Integer.parseInt(arg.substring(7,length));
				                hits = no;      
				            }
				            else if (arg.substring(0, 11).equals("--relevant=")) {
				            	int length = arg.length();
				            	relevance = arg.substring(11,length);				            	
				            	if (relevance.startsWith("(") && relevance.endsWith(")")){
				            		relevance =relevance.substring(1,relevance.length()-1);				            		
				            	}
				                relFlag = true;
				            }
				            else if (arg.substring(0, 13).equals("--irrelevant=")) {
				            	int length = arg.length();
				            	irrelevance = arg.substring(13,length);				            	
				            	if (irrelevance.startsWith("(") && irrelevance.endsWith(")")){
				            		irrelevance =irrelevance.substring(1,irrelevance.length()-1);
				            	}		        
				                irrelFlag = true;
				            }
				        }
				        // finding the the words between double quote ("t1.. t2.. tn")
				        String quotedQuery="";
				        while (i < query.length && query[i].startsWith("\"")) {
				            while (!arg.endsWith("\"")){
				            	arg = query[i++];
				            	quotedQuery += arg + " ";
				            }
				            quoFlag = true;
				        }
				        String[] rejoinQuery= quotedQuery.split(" ");			        
				        // intersect the quoted words
				        for (int queryNo=0; queryNo < rejoinQuery.length-1; queryNo++){
				        	if (!(query[queryNo].toString()==null)|
				        			!(query[queryNo].trim().equals(""))){
				        		String intersectQuery = rejoinQuery[queryNo].trim()+" "+rejoinQuery[queryNo+1].trim();
					        	if (intersectQuery.startsWith("\""))
					        		intersectQuery = intersectQuery.substring(1, intersectQuery.length());
					        	if (intersectQuery.endsWith("\""))
					        		intersectQuery = intersectQuery.substring(0, intersectQuery.length()-1);
					        	unQuotedQuery += intersectQuery+" ";
				        	}			        	
				        }			        
				        if (i == query.length){
				        	System.out.print("");
				        } else {			        	
				        	arg = query[i++];			        	
				        	terms += arg.trim() + " ";			        	
				        }
					} catch (Exception e) {
						// TODO: handle exception		
						catchError();
						searchFlag=false;
					}
			        
		        }
		        
        	}
        } else {        	
        	System.err.println("Usage: Begin with the word " +
        			"'search' to start searching.");	
        	System.out.println();        	
        }
        if (unQuotedQuery.trim()=="" && terms.trim()==""){
        	catchError();
        	searchFlag=false;
        }
        if (searchFlag){
        	myQuery = unQuotedQuery + terms;
	        if (orFlag)
	    		getDisplayedInt(getResultWithOR(unQuotedQuery + terms));
	    	else if (andFlag)
	    		getDisplayedInt(getResultWithAND(unQuotedQuery + terms));
	    	else if (relFlag | irrelFlag){	    		
	    		getDisplayed(getCosineScore(unQuotedQuery + terms));
	    	}
	    	else
	    		getDisplayed(getCosineScore(unQuotedQuery + terms));       	
		
    	System.out.println();			        	
		System.err.println("Options: ");
		System.err.println("1) Always begin with 'search'");
		System.err.println("2) Hit option to fetch Top K-N. e.g.: search [--hits=10] [query]");
		System.err.println("3) Relevance feedback,  e.g.: search [--relevant=(relevance document)] [query]");
		System.err.println("4) Irrelevance feedback ,  e.g.: search [--irrelevant=(relevance document)] [query]");
		System.err.println("5) OR Query option, e.g.: search [--OR] [query]");
		System.err.println("6) AND Query option, e.g.: search [--AND] [query]");
		System.out.println("Type your query 'search [your query]' below or type 'quit' to exit :");
		System.out.println();
        }
	}
	
	
	public int getTermId(String searchTerm) throws IOException{
		
		int termid = 0;		
		TreeMap<String, Integer> postinglist = getPostingList();
		if (postinglist.containsKey(searchTerm)){			
			termid = postinglist.get(searchTerm);
		}	
		return termid;
	}
		
	public void getTermDocPosIndex(int tid)throws IOException{
		
		ZipInputStream zis;
		FileInputStream fis = null;	
		ZipFile zipFile = null;
		ZipEntry ze;
		fis = new FileInputStream(indexFile);
		zipFile = new ZipFile(indexFile);
		zis = new ZipInputStream(fis);
		//int record = 0;		
		
		while ((ze = zis.getNextEntry()) != null){
			String indexData = "";
			String[] splittedString = null;
			InputStream is = zipFile.getInputStream(ze);
			InputStreamReader r = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(r);
			indexData = br.readLine();
			splittedString = indexData.split(";");
			
			for (int i = 0; i < splittedString.length; i++){							
				if (splittedString[i].contains(tid+":")){
					String[] subToken = splittedString[i].toString().split(":");						
					int termid = Integer.parseInt(subToken[0]);
					if (termid==tid){
						int docid = Integer.parseInt(subToken[1]);								
						String[] positions = subToken[2].toString().split(",");
						List<Integer> lpos = new LinkedList<Integer>();
						for (int j=0; j< positions.length; j++)
							lpos.add(Integer.parseInt(positions[j]));						
						
						if (termdocposindex.containsKey(termid)){ 
							TreeMap<Integer, List<Integer>> docids = termdocposindex.get(termid);
							docids.put(docid, lpos);
							termdocposindex.put(termid, docids);
						} else { // if not create new list of doc and positions
							TreeMap<Integer, List<Integer>> docids = new TreeMap<Integer, List<Integer>>();
							docids.put(docid, lpos);
							termdocposindex.put(termid, docids);
						}						
					}
				}
			}			
		}				
		zipFile.close();
		fis.close();		
	}
	
	public String getStemmedQuery(String searchTearm){
		String stemmed = "";
		Stemmer s = new Stemmer();		
		char[] queryArr = searchTearm.toCharArray();
		for (int i=0; i<queryArr.length;i++){
			s.add(queryArr[i]);
		}
		s.stem();
		stemmed = s.toString();
		return stemmed;
	}
	
	public TreeMap<String, Integer> getPostingList() throws IOException{
		TreeMap<String, Integer> postinglist = new TreeMap<String, Integer>();
		FileInputStream fis = new FileInputStream(postinglistFile);
		InputStreamReader r = new InputStreamReader(fis);   
		BufferedReader br = new BufferedReader(r);		
		String[] entries = br.readLine().toString().split(";");
		for (int i=0; i < entries.length; i++){
			String [] termid= entries[i].split(":");
			postinglist.put(termid[0], Integer.parseInt(termid[1]));
		}		
		return postinglist;
	}
	public TreeMap<Integer, String> getDocsMap() throws IOException{
		TreeMap<Integer, String> docmap = new TreeMap<Integer, String>();
		FileInputStream fis = new FileInputStream(docmapFile);
		InputStreamReader r = new InputStreamReader(fis);   
		BufferedReader br = new BufferedReader(r);		
		String[] entries = br.readLine().toString().split(";");
		for (int i=0; i < entries.length; i++){
			String[] tokens = entries[i].split(":");
				docmap.put(Integer.parseInt(tokens[0]), tokens[1]);				
		}		
		return docmap;
	}	
	
	public void getMatchIndex(String query) throws IOException{
		
	}
	
	public void getNotMatchIndex(){
		
	}
	
	public TreeMap<Double, List<Integer>> getCosineScore(String query) throws IOException{
		TreeMap<Double, List<Integer>> rankedResults = 
			new TreeMap<Double, List<Integer>>();
		String[] terms = null;
		if (query.contains("-")){
			terms = query.split(" ");	
			String refinedQuery="";
			for (String term: terms){			
				if (term.contains("-")){
					String[] splittedTerms = term.split("-");
					term="";
					for (String splittedTerm: splittedTerms){						
						term += splittedTerm+ " ";						
					}					
				}
				refinedQuery +=term + " ";
			}	
			
			query = refinedQuery;
			terms = query.split(" ");			
		} else {
			terms = query.split(" ");
		}
		double start = System.currentTimeMillis();
		List<Integer> results = new LinkedList<Integer>();
		HashMap<Integer, Double> docScores = 
			new HashMap<Integer, Double>();
		TreeMap<Integer, List<Integer>> docpos = null;
		TreeMap<Integer, Double> listOfDocScores = 
			new TreeMap<Integer, Double>();
		double tfCorpus=0;			
		for (String term : terms){
			String searchTerm = getStemmedQuery(term.trim());
			int termid = getTermId(searchTerm);
			List<Integer> docs = new LinkedList<Integer>();
			if (termid > 0){				
				if (!termdocposindex.containsKey(termid)){
					getTermDocPosIndex(termid);
					docpos = termdocposindex.get(termid);
				} else {
					docpos = termdocposindex.get(termid);
				}
				Iterator docIter = docpos.keySet().iterator();
				Object doc = null;
				while(docIter.hasNext()){
					doc = docIter.next();
					docs.add((Integer) doc);						
				}
				
				// select only documents contain the query
				if (results.isEmpty())
					results.addAll(docs);
				else{ 
					results.retainAll(docs);
				}
				
			}		
			
			// calculating tf in document			
			for(int result: results){
				double tfDocument = 0;
				List<Integer> lPositions = docpos.get(result);
				tfDocument = lPositions.size();
				tfCorpus += tfDocument;listOfDocScores.put(result, tfDocument);
			}			
		}
		
		// calculating tf in corpus
		Iterator iteratorKey = listOfDocScores.keySet().iterator();
		Object docKey;			
		double tfDocument=0;
		while (iteratorKey.hasNext()){
			docKey = iteratorKey.next();
			tfDocument = listOfDocScores.get(docKey);
			if (! docScores.containsKey(docKey)){					
				docScores.put((Integer) docKey, tfDocument/tfCorpus);
			}
		} 
		
		// rank the results ascending
		for (Integer docid : docScores.keySet()) {
			if (rankedResults.containsKey(docScores.get(docid))) {
				List<Integer> l = rankedResults.get(docScores.get(docid));
				if (l.contains(docid)){						
					rankedResults.put(docScores.get(docid), l);
				}					
			} else {
				List<Integer> l = new LinkedList<Integer>();
				l.add(docid);
				rankedResults.put(docScores.get(docid), l);
			}
		}
		double end = (System.currentTimeMillis()-start)/1000;
		System.out.println("Match documents found in "+end+" seconds.");
		return rankedResults;	
	}
	
	public void getNotMatchDocs(){
		
	}
	
	public List<Integer> getResultWithOR (String query) throws Exception{
		String[] terms = query.split(" ");		
		List<Integer> results = new LinkedList<Integer>();
		for (String term : terms){
			String searchTerm = getStemmedQuery(term);
			int termid = getTermId(searchTerm);
			List<Integer> docs = new LinkedList<Integer>();
			if (termid > 0){
				TreeMap<Integer, List<Integer>> docpos = null;
				if (!termdocposindex.containsKey(termid)){
					getTermDocPosIndex(termid);
					docpos = termdocposindex.get(termid);
				} else {
					docpos = termdocposindex.get(termid);
				}
				Iterator docIter = docpos.keySet().iterator();
				Object doc = null;
				while(docIter.hasNext()){
					doc = docIter.next();			
					List<Integer> lpos1 = docpos.get(doc);						
					//list of docs
					docs.add((Integer) doc);						
				}	
			}		
			
			if (results.isEmpty())
				results.addAll(docs);
			else 
				for (Integer docid : docs) 
					if (! results.contains(docid))
						results.add(docid);
		}
		return results;
	}
	
	public List<Integer> getResultWithAND(String query) throws Exception{
		String[] terms = query.split(" ");		
		List<Integer> results = new LinkedList<Integer>();
		for (String term : terms){
			String searchTerm = getStemmedQuery(term);
			int termid = getTermId(searchTerm);
			List<Integer> docs = new LinkedList<Integer>();
			if (termid > 0){
				TreeMap<Integer, List<Integer>> docpos = null;
				if (!termdocposindex.containsKey(termid)){
					getTermDocPosIndex(termid);
					docpos = termdocposindex.get(termid);
				} else {
					docpos = termdocposindex.get(termid);
				}
				Iterator docIter = docpos.keySet().iterator();
				Object doc = null;
				while(docIter.hasNext()){
					doc = docIter.next();			
					List<Integer> lpos1 = docpos.get(doc);						
					//list of docs
					docs.add((Integer) doc);						
				}	
			}		
			
			if (results.isEmpty())
				results.addAll(docs);
			else 
				results.retainAll(docs);
			
		}
		return results;
		
	}
	
	public void rankResult(){
		
	}
	
	public void getDisplayed(TreeMap<Double, List<Integer>> rankResults) throws Exception{
		int record = 0;
		if (relFlag){
			System.out.println("Relevant docs : "+relevance);
		}else if (irrelFlag){
			System.out.println("Irrelevant docs : "+irrelevance);
		}
		System.out.println();
		if (relFlag){
			System.out.println("Top K query :");
			//calculateTopTenQuery();
		}
		
		System.out.println();
		System.out.println("Retrieving documents...");
		
		FileWriter toFile = new FileWriter("d:/search/machine.txt", true);
		Iterator itr = rankResults.descendingKeySet().iterator();
		Object obj;
		String filenames="";		
		while(itr.hasNext()){			
			obj = itr.next();
			List<Integer> ls = rankResults.get(obj);			
			for (int l: ls){
				record++;
				String filename = docmapindex.get(l);
				filenames += filename.substring(9,filename.length())+",";				
				System.out.println(record+") ["+(Double) obj+"] Filename : "+filename+".");
				fetchDoc(filename);
				if(record == hits)
					break;
			}
			if(record == hits)
				break;
		}
		toFile.write("\n");
		toFile.write(myQuery+","+filenames);
		toFile.flush(); 
		toFile.close();
	}
	
	public void getDisplayedInt(List<Integer> results) throws IOException{
		int record = 0;		
			
		
		if (hits == 0)
			for (int result: results){
				record++;
				String filename = docmapindex.get(result);						
				System.out.println("["+record+"] Document No: "+filename+".");
				fetchDoc(filename);
			}
		else 
			for (int result: results){
				record++;
				String filename = docmapindex.get(result);				
				System.out.println("["+record+"] Document No: "+filename+".");
				fetchDoc(filename);
				if (record == hits){
					break;
				}
			}
	}
	
	public void fetchDoc(String filename) throws IOException{
		ZipEntry ze = null;
		FileInputStream fis = new FileInputStream("d:/search/tdt3.zip");
		ZipFile zipFile = new ZipFile("d:/search/tdt3.zip");
		ZipInputStream zis = new ZipInputStream(fis);		
		while ((ze=zis.getNextEntry())!=null){				
			if (!ze.isDirectory())
				if (ze.toString().equals(filename)){
					InputStream input = zipFile.getInputStream(ze);
					Reader r = new InputStreamReader(input);   
					StringWriter sw = new StringWriter();   
					char[] buffer = new char[1024];   
					for (int n; (n = r.read(buffer)) != -1; )   
					    sw.write(buffer, 0, n);
					// &amp | regular expression stripping off the text
					Pattern p = Pattern.compile(" & | &AMP; |&AMP;|&");		
			        Matcher m = p.matcher(" &amp; ");		        
			        m.reset(sw.toString());		        
			        XMLReader(m.replaceAll(" &amp; "));			        
					r.close();
				}
		}
		
	}
	
	public void XMLReader(String xmlString){
		try {			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				
				private boolean insideText;				
				private StringBuffer textBuffer;			
			
				@Override
				public void startElement(String ns, String ln, String qname, Attributes atts)
				throws SAXException
				{					
					if (qname.equalsIgnoreCase("TEXT")) {			
						insideText = true;			
						textBuffer = new StringBuffer();						
					}					
				}
			
				@Override
				public void characters(char[] ch, int offset, int length)
				throws SAXException
				{
					if (insideText) {
						textBuffer.append(new String(ch, offset, length));
					}
				}
			
				@Override
				public void endElement(String ns, String ln, String qname)
				throws SAXException
				{					
					Stemmer s = new Stemmer();
					if (qname.equalsIgnoreCase("TEXT")) {
						if (insideText) {
							try {			
								InputStream is = new 
								ByteArrayInputStream(textBuffer.toString()
										.trim().getBytes("UTF-8"));			
								BufferedReader br = 
									new BufferedReader(
											new InputStreamReader(is));
								for (int i=0; i< 3; i++)
									System.out.println(br.readLine());
								System.out.println("--------------------");
								br.close();
							} catch (Exception e) {
								System.out.println("Exception: " + e);
							}
						}
					}
				}
			};						
			
			saxParser.parse(new InputSource(new StringReader(xmlString)), handler);			
		} catch (Exception e){
				e.printStackTrace();
		}
	}
	
	public TreeMap<Double, Integer> calculateTopTenQuery() throws IOException{
		// calculating relevance and irrelevance docs
		ZipInputStream zis;
		FileInputStream fis = null;	
		ZipFile zipFile = null;
		ZipEntry ze;
		fis = new FileInputStream(indexFile);
		zipFile = new ZipFile(indexFile);
		zis = new ZipInputStream(fis);
		
		TreeMap<Double, Integer> results = new TreeMap<Double, Integer>();
		TreeMap<Integer, List<Integer>> docTermIndex = new TreeMap<Integer, List<Integer>>();
		List<Integer> termids = new LinkedList<Integer>();
		if (relFlag){
			String[] reldocs = relevance.split(",");
			for (String reldoc: reldocs){
				int docid = getDocId(reldoc);
				// Editing starts here
				
				while ((ze = zis.getNextEntry()) != null){
					String indexData = "";
					String[] splittedString = null;
					InputStream is = zipFile.getInputStream(ze);
					InputStreamReader r = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(r);
					indexData = br.readLine();
					splittedString = indexData.split(";");
					
					for (int i = 0; i < splittedString.length; i++){							
						if (splittedString[i].contains(":"+docid+":")){
							String[] subToken = splittedString[i].toString().split(":");						
							int termid = Integer.parseInt(subToken[0]);
							int sDocId = Integer.parseInt(subToken[1]);
							if(!termids.contains(termid)){
								termids.add(termid);
								//docTermIndex.put(sDocId, termids);
								//calculating the terms' vector scores here
							}
						}
					}			
				}
				for (int termid: termids){
					System.out.println(termid);
					getTermDocPosIndex(termid);
				}				
				// Editing ends here
				// Last editing here!!
				
				Iterator iterator = termdocposindex.keySet().iterator();
				Object obj;
				List<Integer> lterms = new LinkedList<Integer>();
				TreeMap<Integer, List<Integer>> tdocs = new TreeMap<Integer, List<Integer>>();
				while(iterator.hasNext()){
					obj = iterator.next();
					tdocs = termdocposindex.get(obj);
					Iterator iterator2 = tdocs.keySet().iterator();
					Object obj2;
					while(iterator2.hasNext()){
						obj2 = iterator2.next();
						if ((Integer) obj2 == docid){							
							lterms.add((Integer) obj2);
							System.out.println(obj.toString());
						}
					}
				}
				//System.out.println(docid);
			}			
		}else if (irrelFlag){
			String[] irreldocs = irrelevance.split(",");
			for (String irreldoc: irreldocs){
				int docid = getDocId(irreldoc);
				//System.out.println(docid);
			}	
		}
		return results;
	}
	
	public int getDocId(String filename){
		int docid=0;
		//if (docmapindex.containsValue(filename)){
			Iterator iterator = docmapindex.keySet().iterator();
			Object obj;
			while (iterator.hasNext()){
				obj = iterator.next();
				String value = docmapindex.get((Integer) obj);				
				if (value.substring(9, value.length()).equals(filename))
					docid = (Integer) obj;
			}
		//}
		return docid;
	}
	public void catchError(){
		System.err.println("Typing or format error. Please try again. ");
		System.out.println();		
		System.err.println("Options: ");
		System.err.println("1) Always begin with 'search'");
		System.err.println("2) Hit option to fetch Top K-N. e.g.: search [--hits=10] [query]");
		System.err.println("3) Relevance feedback,  e.g.: search [--relevant=(relevance document)] [query]");
		System.err.println("4) Irrelevance feedback ,  e.g.: search [--irrelevant=(relevance document)] [query]");
		System.err.println("5) OR Query option, e.g.: search [--OR] [query]");
		System.err.println("6) AND Query option, e.g.: search [--AND] [query]");
		System.out.println("Type your query 'search [your query]' below or type 'quit' to exit :");
		System.out.println();		
	}
	
}

