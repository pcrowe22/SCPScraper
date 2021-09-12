package current;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

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

public class MainScraper {

	//private static JTree tree;
	
	static Set<String> badSet = new HashSet<>(Arrays.asList(new String[] {"javascript:;", "http://scp-wiki.wikidot.com/", 							//will be listed in file, but won't be recursed into
			"http://www.scpwiki.com", "", "#u-credit-view", "btn-false", "http://www.wikidot.com/user", "/groups-of-interest", "/foundation-tales", 
			"/series-archive", "/incident-reports-eye-witness-interviews-and-personal-logs", "/creepy-pasta", "/user-curated-lists",
			"/joke-scps", "/joke-scps-tales-edition", "/scp-ex", "/goi-formats", "/audio-adaptations", "/scp-artwork-hub",
			"/contest-archive", "/canon-hub", "/log-of-anomalous-items", "/log-of-extranormal-events", "/log-of-unexplained-locations",
			"/about-the-scp-foundation", "/object-classes", "/personnel-and-character-dossier", "/security-clearance-levels",
			"/secure-facilities-locations", "/task-forces", "/scp-calendar", "/task-forces-complete-list", "/faq", "/listpages-magic-and-you"}));  
    
	
	public static void main(String[] args) {
		
		String url;
		Scanner sc = new Scanner(System.in);
		Hashtable<String, Integer> nest = new Hashtable<String, Integer>();
		int rank = 0;
		File f = new File("SCPnest.txt");
		/*final JFrame frame = new JFrame("Table of Contents");
		frame.setSize(500, 500);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new FlowLayout());*/
		
		try {
			f.createNewFile();
			FileWriter fw = new FileWriter("SCPnest.txt", false);
			
			System.out.print("URL to root from: ");
			url = sc.nextLine();
			
			if (url.startsWith("http:")) {
				
				nest.put(url, rank);
				System.out.println(url);
				//DefaultMutableTreeNode top = new DefaultMutableTreeNode(url);
				getNest(url, nest, rank, fw);//, top);
			
				//tree = new JTree(top);
			}/* else {
				FileInputStream fis = new FileInputStream("C:\\Users\\pcrow\\eclipse-workspace\\SCP_Scraper/" + url);
				ObjectInputStream in = new ObjectInputStream(fis);
				
				tree = (JTree) in.readObject();
				in.close();
			}*/
			fw.close();
			/*JScrollPane treeView = new JScrollPane(tree);
			treeView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			treeView.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			frame.getContentPane().add(treeView);
			
			System.out.print("Save tree as: ");
			String name = sc.nextLine();
			FileOutputStream fos = new FileOutputStream("C:\\Users\\pcrow\\eclipse-workspace\\SCP_Scraper/" + name);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(tree);
			out.close();*/
			
		} catch (Exception e)
		{}
		sc.close();
		
		
		
		
	}
	
	public static void getNest(String url, Hashtable<String, Integer> nest, int rank, FileWriter fw) {//, DefaultMutableTreeNode parent) {
		
		try {
			int r = rank + 1;
			Document doc = Jsoup.connect(url).get();
			Element content = doc.select("div#page-content").first();
			Elements links = content.select("a");
			Elements bottomNav = content.select("div.footer-wikiwalk-nav").select("a");
			for (Element bot : bottomNav) {
				links.remove(bot);
				//links.remove(links.size()-1);
			}
			
			
			for (Element src : links) {
				String path = src.attr("href");
				if (!path.matches("(/).*") || path.matches("(http|#|/forum|/system|/theme|mailto|/news|/tale-calendar|/scp-series|nav:side|javascript:;|/young-and-under-30|"
						+ "drakimoto-s-author-page/).*") || path.matches(".*(.png|.jpg|.jpeg|.JPG|.mp3)") || path.matches(".*(p/)[0-9]+"))		//list of paths that won't be listed in file
					continue;
				String link = "http://www.scpwiki.com" + path;
				if (!nest.containsKey(link) ) {
					
					for (int i=0; i<r; i++) {
						System.out.print("  ");
						fw.write("  ");
					}
					System.out.println(link);
					fw.write(link + "\n");
					nest.put(link, r);
					/*DefaultMutableTreeNode child = new DefaultMutableTreeNode(link);
					parent.add(child);*/
					if (badSet.contains(path)) {
						continue;
					}
					getNest(link, nest, r, fw);//, child);
				}
			}
		} catch (HttpStatusException e) {
			System.out.println("Could not reach " + e.getUrl());
			try {
				fw.write("Could not reach " + e.getUrl() + "\n");
			} catch (IOException ioe)
			{}
		} catch (Exception e) {
			System.out.println("\nError url: " + url);
			e.printStackTrace();
			System.out.println("\nError url: " + url);
		}
		
		

	}

}
