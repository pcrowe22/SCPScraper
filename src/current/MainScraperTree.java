package current;

import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*getHTML modifies a pre-existing list, rather than returning a new one

list is a hashmap modified to also contain a rank field
    O(1) amortized find, O(1) amortized insert
getHTML(url=input, list=[], rank=0)
    for every <a> in div id="page content"
        list.add(url:rank)
        getHTML(href of a, list, rank++)
*/

public class MainScraperTree {

	private static JTree tree;
	
	static Set<String> badSet = new HashSet<>(Arrays.asList(new String[] {"javascript:;", "http://scp-wiki.wikidot.com/", 							//will be listed in file, but won't be recursed into
			"http://www.scpwiki.com", "", "#u-credit-view", "btn-false", "http://www.wikidot.com/user", "/groups-of-interest", "/foundation-tales", 
			"/series-archive", "/incident-reports-eye-witness-interviews-and-personal-logs", "/creepy-pasta", "/user-curated-lists",
			"/joke-scps", "/joke-scps-tales-edition", "/scp-ex", "/goi-formats", "/audio-adaptations", "/scp-artwork-hub",
			"/contest-archive", "/canon-hub", "/log-of-anomalous-items", "/log-of-extranormal-events", "/log-of-unexplained-locations",
			"/about-the-scp-foundation", "/object-classes", "/personnel-and-character-dossier", "/security-clearance-levels",
			"/secure-facilities-locations", "/task-forces", "/scp-calendar", "/task-forces-complete-list", "/faq", "/listpages-magic-and-you"}));  
	static int maxRank = 10;		//identifies how many layers the user is comforable with having
	
	public static void main(String[] args) {
		
		String url;
		Scanner sc = new Scanner(System.in);
		Hashtable<String, Integer> nest = new Hashtable<String, Integer>();		//holds set of urls visited
		int rank = 0;															//holds current level of node
		File f = new File("SCPnest.txt");							//output file written to
		File o = new File("SCPnest.opml");
		final JFrame frame = new JFrame("Table of Contents");		//displays tree
		frame.setSize(750, 750);	//could be bigger
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		//stops program when jframe closed
		frame.getContentPane().setLayout(new FlowLayout());
		
		try {
			f.createNewFile();
			o.createNewFile();
			FileWriter fw = new FileWriter("SCPnest.txt", false);
			FileWriter ow = new FileWriter("SCPnest.opml", false);
			
			System.out.print("URL to root from: ");				//specify where the tree shall start
			url = sc.nextLine();
			System.out.print("Max depth: ");				//specify how many levels the tree should have max
			try {
				maxRank = Integer.parseInt(sc.nextLine());
			} catch (Exception e) {
				maxRank = 2147483647;					//no limit
			}
			
			if (url.startsWith("http:")) {
				
				nest.put(url, rank);			//put initial url in list of sites visited
				System.out.println(url);
				DefaultMutableTreeNode top = new DefaultMutableTreeNode(url);	//add first site to tree
				
				ow.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + 
						"<opml version=\"2.0\">\r\n" + 
						"  <head>\r\n" + 
						"    <title>SCP Nest starting at "+url+"</title>\r\n" + 
						"    <flavor>dynalist</flavor>\r\n" + 
						"    <source>https://dynalist.io</source>\r\n" + 
					    "    <ownerName>Patrick Crowe</ownerName>\r\n" +
					    "    <ownerEmail>pcrowe.piano@gmail.com</ownerEmail>\r\n" +
						"  </head>\r\n" + 
						"  <body>\r\n" + 
						"    <outline text=\"SCP Nest\" checkbox=\"true\">\r\n");
				getNest(url, nest, rank, fw, ow, top);		//begin recursion
				ow.write("    </outline>\r\n" +
						"  </body>\r\n" +
						"</opml>");
				tree = new JTree(top);	//build tree from DefaultMutableTreeNode structure
			} /*else {		//if the user doesn't use a url, the user is specifying a file saved in library
				FileInputStream fis = new FileInputStream("C:\\Users\\pcrow\\eclipse-workspace\\SCP_Scraper\\trees/" + url);
				ObjectInputStream in = new ObjectInputStream(fis);
				
				tree = (JTree) in.readObject();		//load saved tree
				in.close();
			}*/
			fw.close();
			ow.close();
			JScrollPane treeView = new JScrollPane(tree);		//display tree
			treeView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			treeView.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			frame.getContentPane().add(treeView);
			frame.setVisible(true);
			if (url.startsWith("http:")) {			//if url, be sure to ask for saving name
				System.out.print("Save tree as: ");
				String name = sc.nextLine();
				/*FileOutputStream fos = new FileOutputStream("C:\\Users\\pcrow\\eclipse-workspace\\SCP_Scraper\\trees/" + name);
				ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(tree);
				out.close();*/
			}
			
		} catch (Exception e)
		{}
		sc.close();
		
		
		
		
	}
	
	public static boolean getNest(String url, Hashtable<String, Integer> nest, int rank, FileWriter fw, FileWriter ow, DefaultMutableTreeNode parent) {
		
		try {
			boolean b = false;
			int r = rank + 1;
			Document doc = Jsoup.connect(url).get();					//get html page
			Element content = doc.select("div#page-content").first();	//select the "a" tags within the #page-content div
			Elements links = content.select("a");
			Elements bottomNav = content.select("div.footer-wikiwalk-nav").select("a");
			for (Element bot : bottomNav) {					//exclude the set of "a" tags within the .footer-wikiwalk-nav div
				links.remove(bot);
			}
			
			
			for (Element src : links) {				//for every link in the set of "a" tag links...
				String path = src.attr("href");
				
				//check to make sure it doesn't start or end with a bad substring
				if (!path.matches("(/).*") || path.matches("(http|#|/forum|/system|/theme|mailto|/news|/tale-calendar|/scp-series|nav:side|javascript:;|/young-and-under-30|"
						+ "author-page/).*") || path.matches(".*(.png|.jpg|.jpeg|.JPG|.mp3)") || path.matches(".*(p/)[0-9]+"))		//list of paths that won't be listed in file
					continue;
				
				String link = "http://www.scpwiki.com" + path;
				if (!nest.containsKey(link) ) {			//if the link has not already been visited...
					ow.write("    ");
					for (int i=0; i<r; i++) {
						System.out.print("  ");
						fw.write("  ");
						ow.write("  ");
					}
					System.out.println(link);		//write it,
					fw.write(link + "\n");
					ow.write("<outline text=\"" + link + "\" checkbox=\"true\">\r\n");
					nest.put(link, r);				//add it to the list of visited sites
					DefaultMutableTreeNode child = new DefaultMutableTreeNode(link);		//and add it to the parent node as a child
					parent.add(child);
					if (badSet.contains(path)) {			//if the link is problematic, continue to the next child
						continue;
					}
					if (r<maxRank)	{			//if the max level hasn't been hit yet, perform all the above operations on the child
						b = true;
						getNest(link, nest, r, fw, ow, child);
							ow.write("    ");
							for (int i=0; i<r; i++) {
								ow.write("  ");
							}
							ow.write("</outline>\r\n");
						//} else {
						//	ow.write("/>\r\n");
						
					//} else {
						//ow.write("/>\r\n");
						//continue;
					}	
				}
			}
			return b;
		} catch (HttpStatusException e) {
			System.out.println("Could not reach " + e.getUrl());
			try {
				fw.write("Could not reach " + e.getUrl() + "\n");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			System.out.println("\nError url: " + url);
			e.printStackTrace();
			System.out.println("\nError url: " + url);
			return false;
		}
		
		

	}

}
