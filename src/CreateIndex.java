
import java.io.*;
import java.security.KeyStore.LoadStoreParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
/**
 * 
 */

/**
 * @author Khalilur
 *
 */
public class CreateIndex {	
	
	public int docID = 0;
	public int termID = 0;
	
	public char[] stringArray;
	public TreeMap <Integer, String> documentMap = 
		new TreeMap<Integer, String>();
	public TreeMap <String, Integer> lexicon = 
		new TreeMap<String, Integer>();
	public TreeMap<Integer, List<Integer>> termtodocindex = 
		new TreeMap<Integer, List<Integer>>();
	public TreeMap<Integer, List<Integer>> doctotermindex = 
		new TreeMap<Integer, List<Integer>>();	
	public TreeMap<Integer, TreeMap<Integer, List<Integer>>> termtodocposindex = 
		new TreeMap<Integer, TreeMap<Integer,List<Integer>>>();
	public TreeMap<Integer, TreeMap<List<Integer>, List<Integer>>> doctotermposindex = 
		new TreeMap<Integer, TreeMap<List<Integer>,List<Integer>>>();
	public TreeMap <String, String> stopwords = 
		new TreeMap<String, String>();
	public List<String> textPosition = 
		new ArrayList<String>();
	
    
	public static void main(String[] args) throws Exception {
		CreateIndex ci = new CreateIndex();
		ci.loadStopWords();
		ci.loadLexicon();		
		//timer		
		double startTokenizing = System.currentTimeMillis();
		System.out.println("Generating indexes...");
		String tdt3 = "d:/search/tdt3.zip";
		ci.DocParser(tdt3);
		System.out.println("Total : "+ci.docID+" documents");
		double endTokenizing = (System.currentTimeMillis() - startTokenizing)/1000/60;		
		System.out.println("Tokenizing time : "+endTokenizing+" minutes.");
	}
	
	
	
	private  void DocParser(String tdt3) throws Exception {
				
		FileInputStream fis = new FileInputStream(tdt3);
		ZipInputStream zis = new ZipInputStream(fis);
		ZipFile zipFile = new ZipFile(tdt3);
		ZipEntry ze;		
		int idFile = 0;
		//Parsing process		
		double start = System.currentTimeMillis();		
		while ((ze = zis.getNextEntry()) != null){
			if (!ze.isDirectory()){					
				docID++;				
				//split index file into 2K files each to avoid:
				//1. java heap lack in JVM on index creation
				//2. yet, it works to decrease searching time
				if (docID%2000==0){
					idFile++;
					try {
						//WriteTermDocIndex("d:/search/term_doc_index_"+idFile+".txt");
						//WriteDocTermIndex("d:/search/doc_term_index_"+idFile+".txt");
						WriteTermDocPosIndex("d:/search/term_doc_pos_index_"+idFile+".txt");						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}								
				
				//Parse to XML				
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
		        documentMap.put(docID, ze.toString());
				r.close();				
			}						
		}
		zis.close();
		zipFile.close();
		
		idFile++;
		try {
			WriteDocMap();
			//WriteTermDocIndex("d:/search/term_doc_index_"+idFile+".txt");
			//WriteDocTermIndex("d:/search/doc_term_index_"+idFile+".txt");
			WriteTermDocPosIndex("d:/search/term_doc_pos_index_"+idFile+".txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		System.out.println();		
		double end = ((System.currentTimeMillis() - start)/1000/60);	    
		System.out.println("Total documents : "+documentMap.size()+" terms.");
		System.out.println("Total posting list : "+lexicon.size()+" terms.");
		System.out.println("Overall process time : "+end+" minutes.");
		System.out.println("Indexing completed.");
		
		documentMap.clear();
		termtodocindex.clear();
		doctotermindex.clear();
		termtodocposindex.clear();
		
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
								StreamTokenizer st = new StreamTokenizer(br);
								int position = 0;
								while (st.nextToken() != StreamTokenizer.TT_EOF) {
									position++;
									//positional index									
									switch (st.ttype) {
									case StreamTokenizer.TT_WORD:										
										// Stop words stripping process		
										String[] splitter = null;
										if (st.sval.contains("-")){																						
											splitter = st.sval.split("-");											
											for (int i = 0; i<splitter.length; i++){
												if (i > 0)position++;											
												if (!isStopword(splitter[i].toString().toLowerCase())){
													stringArray = splitter[i].toString().toLowerCase().toCharArray();									    	
													for (int j=0; j<stringArray.length;j++){
														s.add(stringArray[j]);
													}			
													s.stem();
													Integer termKey = null;
													if(!s.toString().contains(".")){												
														if (lexicon.containsKey(s.toString())){
															termKey = lexicon.get(s.toString());												
															//CreateTermDocIndex(termKey, docID);											
															//CreateDocTermIndex(docID, termKey);
															CreateTermDocPosIndex(termKey, docID, position);													
														}
													}
												}else{									    	
											    	break;
											    }
											}
										} else {
											if (!isStopword(st.sval.toLowerCase())){
												stringArray = st.sval.toLowerCase().toCharArray();									    	
												for (int i=0; i<stringArray.length;i++){
													s.add(stringArray[i]);
												}			
												s.stem();			
												Integer termKey = null;
												if(!s.toString().contains(".")){												
													if (lexicon.containsKey(s.toString())){
														termKey = lexicon.get(s.toString());												
														//CreateTermDocIndex(termKey, docID);											
														//CreateDocTermIndex(docID, termKey);
														CreateTermDocPosIndex(termKey, docID, position);													
													}
												}
											}else{									    	
										    	break;
										    }
										}											
									    break;
									default:
									    break;
									}
								}
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
		
	public void CreateTermDocIndex(int termKey, int docid){		
		if (termtodocindex.containsKey(termKey)){
			List<Integer> l = termtodocindex.get(termKey);
			
			l.add(docid);			
			termtodocindex.put(termKey, l);																		
		} else {
			List <Integer> l = new LinkedList<Integer>(); 
			l.add(docid);
			termtodocindex.put(termKey,l);													
		}		
		
	}
	
	public void WriteTermDocIndex(String FileName) throws IOException{
		//writing term-doc-index
		System.out.println();
		System.out.println("Writing "+FileName+" to disk.");
		double start = System.currentTimeMillis();
		Iterator iterator = termtodocindex.keySet().iterator();
		
		
		FileWriter toFile = new FileWriter(FileName, true);		
		Object obj;		
		while (iterator.hasNext()){
			String docs="";
			obj = iterator.next();			
			HashSet<Integer> hash = new HashSet<Integer>(termtodocindex.get(obj));
			Iterator iter = hash.iterator();
			while (iter.hasNext()) {		
				String doc = iter.next().toString();
				docs += doc+",";
			}			
			toFile.write(obj+":"+docs+";");		
		}		    	    
		toFile.flush(); 
		toFile.close();		
		
		System.out.println();		
		double end = ((System.currentTimeMillis() - start)/1000/60);	    
		System.out.println("Total : "+termtodocindex.size()+" terms.");
		System.out.println("Writing "+FileName+" : "+end+" minutes.");
		System.out.println("Task completed.");
		
		termtodocindex = new TreeMap<Integer, List<Integer>>();
	}
	
	public void CreateDocTermIndex(int docid, int termKey){		
		if (doctotermindex.containsKey(docid)){
			List<Integer> l = doctotermindex.get(docid);			
			l.add(termKey);			
			doctotermindex.put(docid, l);																		
		} else {
			List <Integer> l = new LinkedList<Integer>(); 
			l.add(termKey);
			doctotermindex.put(docid,l);													
		}
	}
	
	public void WriteDocTermIndex(String FileName) throws IOException{
		//writing term-doc-index
		System.out.println();
		System.out.println("Writing "+FileName+" to disk.");
		double start = System.currentTimeMillis();
		Iterator iterator = doctotermindex.keySet().iterator();
		
		
		FileWriter toFile = new FileWriter(FileName, true);		
		Object obj;		
		while (iterator.hasNext()){
			String terms="";
			obj = iterator.next();			
			HashSet<Integer> hash = new HashSet<Integer>(doctotermindex.get(obj));
			Iterator iter = hash.iterator();
			while (iter.hasNext()) {		
				String term = iter.next().toString();
				terms += term+",";
			}			
			toFile.write(obj+":"+terms+";");		
		}		    	    
		toFile.flush(); 
		toFile.close();		
		
		System.out.println();		
		double end = ((System.currentTimeMillis() - start)/1000/60);	    
		System.out.println("Total : "+doctotermindex.size()+" terms.");
		System.out.println("Writing "+FileName+" : "+end+" minutes.");
		System.out.println("Task completed.");
		
		doctotermindex = new TreeMap<Integer, List<Integer>>();
	}
	
	public void CreateTermDocPosIndex(int termKey, int docid, int position){		
		if (termtodocposindex.containsKey(termKey)){
			TreeMap<Integer, List<Integer>> docpos = termtodocposindex.get(termKey);
			if (docpos.containsKey(docid)){
				List<Integer> l = docpos.get(docid);
				l.add(position);
				docpos.put(docid, l);
			} else {
				List<Integer> l = new LinkedList<Integer>();
				l.add(position);
				docpos.put(docid, l);
			}						
			termtodocposindex.put(termKey, docpos);																		
		} else {
			List<Integer> l = new LinkedList<Integer>();
			l.add(position);
			TreeMap<Integer, List<Integer>> docpos = new TreeMap<Integer, List<Integer>>();
			docpos.put(docid, l);
			termtodocposindex.put(termKey, docpos);			
		}
	}
	
	public void WriteTermDocPosIndex(String FileName) throws IOException{
		//writing term-doc-index
		System.out.println();
		System.out.println("Writing "+FileName+" to disk.");
		double start = System.currentTimeMillis();
		Iterator iterator = termtodocposindex.keySet().iterator();	
		FileWriter toFile = new FileWriter(FileName, true);		
		Object obj;		
		while (iterator.hasNext()){	
			String docpos ="";
			obj = iterator.next();				
			TreeMap<Integer, List<Integer>> tm = termtodocposindex.get(obj);
			Iterator tmIterator = tm.keySet().iterator();
			Object tmObject;
			while (tmIterator.hasNext()){	
				String positions ="";
				tmObject = tmIterator.next();
				HashSet<Integer> hash= new HashSet<Integer>(tm.get(tmObject));
				Iterator iter = hash.iterator();
				while(iter.hasNext()){
					String position = iter.next().toString();
					positions += position+",";
				}				
				docpos = tmObject +":"+ positions;
				toFile.write(obj+":"+docpos+";");
			}		
		}		    	    
		toFile.flush(); 
		toFile.close();		
		
		System.out.println();		
		double end = ((System.currentTimeMillis() - start)/1000/60);	    
		System.out.println("Total : "+termtodocposindex.size()+" terms.");
		System.out.println("Writing "+FileName+" : "+end+" minutes.");
		System.out.println("Task completed.");
		
		termtodocposindex = new TreeMap<Integer, TreeMap<Integer,List<Integer>>>();
	}
	
	public void loadLexicon() throws IOException{		
		FileInputStream fis = new FileInputStream("d:/search/postinglist.txt");
		InputStreamReader r = new InputStreamReader(fis);   
		BufferedReader br = new BufferedReader(r);		
		String[] entries = br.readLine().toString().split(";");
		for (int i=0; i < entries.length; i++){
			System.out.println(entries[i]);
			String [] termid= entries[i].split(":");
			lexicon.put(termid[0], Integer.parseInt(termid[1]));			
		}		
	}
	
	public void WriteDocMap() throws IOException{
		//Writing docmap index
		System.out.println("Writing document mapping to disk.");
		double start = System.currentTimeMillis();
		Iterator iterator = documentMap.keySet().iterator();
		
		FileWriter toFile = new FileWriter("d:/search/docmap.txt");		
		Object obj;		
		while (iterator.hasNext()){
			obj = iterator.next();			
			toFile.write(obj+":"+documentMap.get(obj)+";");		
		}		    	    
		toFile.flush(); 
		toFile.close();				
		
		System.out.println();		
		double end = ((System.currentTimeMillis() - start)/1000/60);	    
		System.out.println("Total : "+documentMap.size()+" documents.");
		System.out.println("Writing d:/search/docmap.txt : "+end+" minutes.");
		System.out.println("Task completed.");
	}
	
	public boolean isStopword(String term) throws IOException{		
		boolean status = false;

		if(stopwords.containsKey(term)){			
			status = true;
		}else {
			status = false;
		}		
		return status;
	}
	
	public void loadStopWords() throws IOException{
		FileInputStream input = new FileInputStream("d:/search/stop_words.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(input));		
		String line = null;
		while((line=br.readLine())!=null){
			stopwords.put(line, null);
		}
	}
}	
