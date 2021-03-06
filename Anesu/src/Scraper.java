import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jaunt.Element;
import com.jaunt.Elements;
import com.jaunt.JauntException;
import com.jaunt.NotFound;
import com.jaunt.ResponseException;
import com.jaunt.UserAgent;
import com.jaunt.component.Table;


public class Scraper {
	public static void main(String[] args){
		String website = "http://www.intern.supply/"; //enter the website to be scraped
		String websiteDesign = "http://www.intern.supply/design.html";
		
		JSONObject data = new JSONObject();
		PrintWriter pw = null;
			
		try {
			pw = new PrintWriter(new File("data/db.json"));
			pw.println(data.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			List<InternshipLink> designLinks = getLinks(websiteDesign);
			int count = 0;
			for(InternshipLink link : designLinks){
				String type = getCompanyType(link);
				link.setType(parseType(type));
				link.setLangauges(new HashSet<String>());
				link.setLocation(getLocation(link));
				link.setPositions(new HashSet<String>(Arrays.asList("UI")));
				link.setSize(getCompanySize(link));
				link.setPlatform("media");
		
				JSONObject company = getJSONObject(link);
				pw.println(company + ",");
				//System.out.println(++count + " of " + designLinks.size());
				//data.wrap(company);
			}
			
			count = 0;
			List<InternshipLink> links = getLinks(website);
			for(InternshipLink link : links){
				String type = getCompanyType(link);
				link.setType(parseType(type));
				link.setLangauges(getLanguageSkills(link));
				link.setLocation(getLocation(link));
				link.setPositions(getPositions(link));
				link.setSize(getCompanySize(link));
				link.setPlatform(getPlatform(link));
				
				JSONObject company = getJSONObject(link);
				pw.println(company + ",");
				//System.out.println(++count + " of " + links.size());
				//data.wrap(company);
			}
			
		} catch (JauntException e) {
			e.printStackTrace();
		}
		
		pw.close();
		//System.out.println(data);
		
	}
	
	private static String parseType(String type){
		if(type == null){
			return null;
		}
		for(String str : Constants.softwareKeyWords){
			if(type.toLowerCase().trim().indexOf(str) >= 0){
				System.out.println(type.toLowerCase() + " -> " + str);
				return "software";
			}
		}
		
		for(String str : Constants.designKeyWords){
			if(type.indexOf(str) >= 0){
				return "design";
			}
		}
		
		for(String str : Constants.hardwareKeyWords){
			if(type.indexOf(str) >= 0){
				return "hardware";
			}
		}
		
		for(String str : Constants.realEstateKeyWords){
			if(type.indexOf(str) >= 0){
				return "real estate";
			}
		}
		
		for(String str : Constants.financeKeyWords){
			if(type.indexOf(str) >= 0){
				return "finance";
			}
		}
		
		for(String str : Constants.entertainmentKeyWords){
			if(type.indexOf(str) >= 0){
				return "entertainment";
			}
		}
		
		for(String str : Constants.travelKeyWords){
			if(type.indexOf(str) >= 0){
				return "travel";
			}
		}

		return null;
	}
	
	private static JSONObject getJSONObject(InternshipLink link){
		JSONObject company = new JSONObject();
		
		
		System.out.println(link.getType());
		if(link.getType() == null)
			company.put("type", "");
		else
			company.put("type", link.getType());
		company.put("company", link.getCompany());
		company.put("link", link.getLink());
		company.put("size", link.getSize());
		company.put("platform", link.getPlatform());
		company.put("locations", new JSONArray(link.getLocation()));
		company.put("positions", new JSONArray(link.getPositions()));
		company.put("languages", new JSONArray(link.getLangauges()));
		
		return company;
	}
	
	private static List<InternshipLink> getLinks(String site) throws JauntException {
		List<InternshipLink> links = new ArrayList<>();
		UserAgent agent = new UserAgent();
		agent.visit(site);
		
		Element div = agent.doc.findFirst("<div class=inner>");
		Elements ul = div.findEach("<li>");
		
		int len = ul.size();
		for(int i = 0; i < len; i++){
			Element li = ul.getElement(i);
			Element child = li.getChildElements().get(0);
			
			if(child.getName().equals("a")){
				String company = li.getText();
				String link = child.getAt("href");
				
				links.add(new InternshipLink(company, link));
			}
	
		}
		return links;	
	}
	
	private static Set<String> getLocation(InternshipLink link) throws ResponseException{
		Set<String> locations = new HashSet<>();
		UserAgent agent = new UserAgent();
		try{
			agent.visit(link.getLink());
		}catch(Exception e){
			return new HashSet<String>();
		}
		
		Element doc = agent.doc;
		Pattern p = Pattern.compile("[A-Z][a-zA-Z]{1,15}(\\.)?(\\s){0,2}([A-Z][a-zA-Z]{1,21})?,(\\s)?(([A-Z]{2})|(United States)|(US))");
		Matcher m = p.matcher(doc.innerHTML());
		
		while(m.find()) {
			String loc = m.group();
			if(isValidLocation(loc))
				locations.add(loc);
		}
		return locations;
	}
	
	private static Set<String> getLanguageSkills(InternshipLink link)throws ResponseException {
		Set<String> skills = new HashSet<>();
		UserAgent agent = new UserAgent();
		try{
			agent.visit(link.getLink());
		}catch(Exception e){
			return new HashSet<String>();
		}
		
		Element doc = agent.doc;
		Pattern p = Pattern.compile(Constants.getLanguagesString());
		Matcher m = p.matcher(doc.innerHTML().toLowerCase());
		
		while(m.find()) {
			String skill = m.group().trim().toLowerCase();
			if(Constants.languagesSet.contains(skill))
				skills.add(skill);
		}
		
		return skills;
	}
	
	private static Set<String> getPositions(InternshipLink link) throws ResponseException, NotFound{
		Set<String> positions = new HashSet<>();
		UserAgent agent = new UserAgent();
		try{
			agent.visit(link.getLink());
		}catch(Exception e){
			return new HashSet<String>();
		}
		
		Element doc = agent.doc;
		
		Pattern p = Pattern.compile(Constants.getPositionString());
		Matcher m = p.matcher(doc.innerHTML().toLowerCase());
		
		while(m.find()) {
			String skill = m.group().trim().toLowerCase();
			if(Constants.types.containsKey(skill))
				positions.add(Constants.types.get(skill));
		}
		
		return positions;
	}
	
	public static String getPlatform(InternshipLink link) throws ResponseException{
		Set<String> positions = new HashSet<>();
		UserAgent agent = new UserAgent();
		try{
			agent.visit(link.getLink());
		}catch(Exception e){
			return "all";
		}
		
		int mobile = 0;
		int web = 0;
		
		Pattern p = Pattern.compile(Constants.getMobileString());
		Matcher m = p.matcher(agent.doc.innerHTML().toLowerCase());
		
		while(m.find()){
			mobile++;
		}
		
		p = Pattern.compile(Constants.getWebString());
		m = p.matcher(agent.doc.innerHTML().toLowerCase());
		
		while(m.find()){
			web++;
		}
		
		if(mobile > web){
			return "mobile";
		}else{
			return "web";
		}
	}
	
	public static String getCompanySize(InternshipLink link){
		String company = link.getCompany().toLowerCase();
		if(Constants.largeCompaniesSet.contains(company)){
			return "large";
		}
		return "medium";
	}
	
	public static String getCompanyType(InternshipLink link){
		String type = null;
		UserAgent agent = new UserAgent();
		
		URL wiki = null;
		try {
			wiki = new URL("https://en.wikipedia.org/wiki/" + link.getCompany());
			//wiki = new URL("https://en.wikipedia.org/wiki/Akamai_Technologies");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		}
		
		try {
			agent.visit(wiki.toString());
		} catch (ResponseException e) {
			e.printStackTrace();
			return null;
		}
		
		Table table = null;
		try {
			table = agent.doc.getTable("<table>");
		} catch (NotFound e) {
			e.printStackTrace();
			return null;
		}
		
		Elements th = table.getElement().findEach("<th>");
		for(Element t : th){
			Element td = null;
			if(t.getText().equals("Industry")){
				try {
					td = t.nextSiblingElement();
				} catch (NotFound e) {
					e.printStackTrace();
					return null;
				}
				
				Element a = null;
				try {
					a = td.getFirst("<a>");
				} catch (NotFound e) {
					e.printStackTrace();
					return null;
				}
				
				type = a.getText();
				
			}
		}
		
		
		
		return type;
	}
	
	private static boolean isValidLocation(String loc){
		String city = loc.split(",")[0].trim().toLowerCase();
		return Constants.cities.contains(city);
	}
}
