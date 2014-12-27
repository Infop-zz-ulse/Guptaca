package com.infopulse.guptaca;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;


public class Guptaca {
  
 
	private static String SOURCE_FILE_NAME_TEMPLATE = "(?i:((.*?)\\.(APP|APT|APL)))$";
		
	private static final String SAL_LINE_HEADER = "^((\\s*)|(.head \\d (\\+|-)  ))";

	private static final String SAL_COMMENT = SAL_LINE_HEADER + "((!)|(Description: )).*";
	private static final String SAL_INCLUDE = SAL_LINE_HEADER + "((File Include)|(Dynalib)):.*";
	
	private static final String SAL_FUNCTION = "Function";
	private static final String SAL_FUNCTION_DECLARATION = SAL_LINE_HEADER + SAL_FUNCTION + ":.*";
	private static final String SAL_CLASS = "(Functional|General Window|Column|Dialog Box|Form Window|MDI Window|Table Window|Data Field|Pushbutton|Check Box|Option Button|Picture|Group Box|Frame|Background Text|Multiline Field|Radio Button|List Box|Combo Box|Custom Control|Child Table) Class";
	private static final String SAL_CLASS_DECLARATION = SAL_LINE_HEADER + SAL_CLASS + ":.*";
	
	private static final String SAL_MODULE_MEMBER_DECLARATION = SAL_LINE_HEADER + "((" + SAL_FUNCTION + ")|(" + SAL_CLASS + ")): ";
	


	private enum SalCodeSection {HEADER, LIBRARIES, INTERNAL_FUNCTIONS, CLASS_DEFINITIONS, FORMS};

	private static final String SAL_CODE_SECTION_TEMPLATE_LIBRARIES = "Libraries";
	private static final String SAL_CODE_SECTION_TEMPLATE_INTERNAL_FUNCTIONS = "Internal Functions";
	private static final String SAL_CODE_SECTION_TEMPLATE_CLASS_DEFINITIONS = "Class Definitions";
	
	
	
	private static Hashtable<String, Module> modules = new Hashtable <String, Module> ();
	
	final static Logger log = Logger.getLogger(Guptaca.class.getName());
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		
		BasicConfigurator.configure();
		
		log.info("started");
		
		String srcFolder = "M:\\Infopulse\\Projects\\Galilei";
		
	
		loadModules(srcFolder);
		parseModules(modules); 
		
		//printModulesInfo(modules);
		
		checkMembersUsing(modules);
		
		log.info("finished");
				
	}
	

	
	private static void loadModules(String srcFolder){
		
		  File[] files = new File(srcFolder).listFiles();
		  for(File f: files){
		    if(f.getName().matches(SOURCE_FILE_NAME_TEMPLATE)){
		      
		      Module m = new Module(f.getAbsolutePath());
		      modules.put(m.getName(), m);
		      
		      log.info("loaded " + m.getType() + " module: " + m.getName() + " ( " + m.getFullName() + " )");
		      
		    }
		    
		    if(f.isDirectory()){
		    	loadModules(f.getAbsolutePath());
		    }
		  }	  
	}
	
	private static void parseModules(Hashtable<String, Module> modules){
		
		log.info("Starting parsing -->");

		
		Enumeration<String> moduleNames = modules.keys();
		
		while(moduleNames.hasMoreElements()) {
			
			Module m = modules.get(moduleNames.nextElement());
						
		    parseModule(m);
		    
		}
		
		log.info("Finishing parsing <--");
	}
	
	private static void parseModule(Module m){
		
		log.info("Parsing " + m.getType() + " module: " + m.getName() + " ( " +m.getFullName()+ " )");
	
		try {
			
			List<String> lines = Files.readAllLines(Paths.get(m.getFullName()), Charset.defaultCharset());
		  
			SalCodeSection codeSection = SalCodeSection.HEADER;
			for (String l : lines) {
				
				if ((codeSection == SalCodeSection.HEADER) && (l.trim().startsWith(SAL_CODE_SECTION_TEMPLATE_LIBRARIES))) {
					codeSection = SalCodeSection.LIBRARIES;
				}
				
				if ((codeSection == SalCodeSection.LIBRARIES) && (l.trim().startsWith(SAL_CODE_SECTION_TEMPLATE_INTERNAL_FUNCTIONS))) {
					codeSection = SalCodeSection.INTERNAL_FUNCTIONS;
				}
				
				if ((codeSection == SalCodeSection.INTERNAL_FUNCTIONS) && (l.trim().startsWith(SAL_CODE_SECTION_TEMPLATE_CLASS_DEFINITIONS))) {
					codeSection = SalCodeSection.CLASS_DEFINITIONS;
				}
				
				//log.info(l);
				
				// INCLUDES

				if (codeSection == SalCodeSection.LIBRARIES) {
					
					if (l.matches(SAL_INCLUDE)) {
					
						//log.info("File Include --> " + l);	
					
						int colonPos = l.indexOf(":");
						int dotPos = l.indexOf(".", colonPos);
					
						String libraryName = l.substring(colonPos+1, dotPos);
					
						log.info("library --> " + libraryName);
					
						Module library = modules.get(libraryName);
					
						if (library != null) {
					
							m.addLibrary(libraryName, library);
							library.addDependentModule(m);
						
						}
					}
				}
				
				// FUNCTIONS
				if (codeSection == SalCodeSection.INTERNAL_FUNCTIONS) {
					
					if (l.matches(SAL_FUNCTION_DECLARATION)) {
					
						//log.info("Function: " + l);	
					
						// from ": " (in "Function: ") till first space after OR till EOL
						int startPos = l.indexOf(":")+2;	
						int endPos = l.indexOf(" ", startPos) > 0 ? l.indexOf(" ", startPos)-1 : l.length() ;
					
						String functionName = l.substring(startPos, endPos);   

						log.info("function --> " + functionName);

						
						Function f = new Function(m, functionName);
						m.addFunction(f);
					}
				}
				
				// Classes
				if (codeSection == SalCodeSection.CLASS_DEFINITIONS) {

					if (l.matches(SAL_CLASS_DECLARATION)) {
						
						//log.info("Class: " + l);	
					
						// from ": " (in "... Class: ") till first space after OR till EOL
						int startPos = l.indexOf(":")+2;	
						int endPos = l.indexOf(" ", startPos) > 0 ? l.indexOf(" ", startPos)-1 : l.length() ;
					
						String className = l.substring(startPos, endPos);   

						log.info("class --> " + className);

						
						Class c = new Class(m, className);
						m.addClass(c);
					}

					
				}
				
				
			}
		} catch (IOException e) {
				log.error(e);
		}
	}
	
	private static void printModulesInfo(Hashtable<String, Module> modules){

		log.info("Start printing modules info -->");

		
		Enumeration<String> moduleNames = modules.keys();
		
		while(moduleNames.hasMoreElements()) {
			
			Module m = modules.get(moduleNames.nextElement());
			log.info("Printing module " + m.getName() + ", type: "+ m.getType() + ", ( " + m.getFullName() + " ) -->");
			
			log.info("libraries -->");
			
			Enumeration<String> libraryNames = m.getLibraries().keys();
			
			while(libraryNames.hasMoreElements()) {
				
				Module library = m.getLibraries().get(libraryNames.nextElement());
				
				log.info(library.getName());
				    
			}				
			
			log.info("libraries <--");

			log.info("functions -->");
			
			Enumeration<String> functions = m.getFunctions().keys();
			
			while(functions.hasMoreElements()) {
				
				Function f = m.getFunctions().get(functions.nextElement());
				
				log.info(f.getName());
				    
			}				
			
			log.info("functions <--");

			
		}
	
	log.info("Finished printing modules info <--");
	
	}
	
	private static void checkMembersUsing(Hashtable<String, Module> modules){

		log.info("Started checking module members using -->");
		
		Enumeration<String> moduleNames = modules.keys();
		
		while(moduleNames.hasMoreElements()) {
			
			Module m = modules.get(moduleNames.nextElement());
			
			log.info("Checking module " + m.getName());
			
			Enumeration<String> functions = m.getFunctions().keys();
			
			while(functions.hasMoreElements()) {
				
				Function f = m.getFunctions().get(functions.nextElement());
				log.info(f.getName() + " used? " + isModuleMemberUsed(f));						
			}				

			Enumeration<String> classes = m.getClasses().keys();

			while(classes.hasMoreElements()) {
				
				Class c = m.getClasses().get(classes.nextElement());
				log.info(c.getName() + " used? " + isModuleMemberUsed(c));						
			}				
			
			
	    }
		
		log.info("Finished checking module members using -->");

	}
		private static Boolean isModuleMemberUsed (ModuleMember mm) {

			// used in the "home" module?
			if (isMentionedInFile(mm.getName(), mm.getHomeModule().getFullName())) {
				
				mm.addDependentModule(mm.getHomeModule());
				return true;
			}
			
			// used in the "dependent" modules?
			Enumeration<String> dependentModuleNames = mm.getHomeModule().getDependentModules().keys();
			
			while(dependentModuleNames.hasMoreElements()) {
				
				Module dependentModule = mm.getHomeModule().getDependentModules().get(dependentModuleNames.nextElement());
				if (isMentionedInFile(mm.getName(), dependentModule.getFullName())) {
					
					mm.addDependentModule(dependentModule);
					return true;
				}
			}
						
			return false;
		}
		
		private static Boolean isMentionedInFile(String name, String file) {
			
			try {
				
				List<String> lines = Files.readAllLines(Paths.get(file), Charset.defaultCharset());
			  
				boolean processLines = false;
				
				for (String l : lines) {
					
					//log.info(l);
					
					// ignore beginning of the file
					if (l.trim().startsWith(SAL_CODE_SECTION_TEMPLATE_LIBRARIES)) {
						processLines = true;
					}
					
				    if (processLines) {
				    	
				    	// whole word
				    	if (l.matches(SAL_LINE_HEADER + ".*[^a-zA-Z0-9_]" + name.trim() + "[^a-zA-Z0-9_].*")) {
				    		
				    		// but not in comment
				    		if (!l.matches(SAL_COMMENT)) {
				    		
				    			// and not in definition
				    			if (!l.trim().matches(SAL_MODULE_MEMBER_DECLARATION + name.trim() +".*")) {				    			
				    				
				    								    				
				    				// and not inside a string constant --> even number of " before and after <name>
				    				
				    				String lBefore = l.substring(0, l.indexOf(name.trim()));
				    				String lAfter = l.substring(l.indexOf(name.trim()) + name.trim().length());
				    				
				    				if ( (StringUtils.countMatches(lBefore,"\"") & 1) == 0 && (StringUtils.countMatches(lAfter,"\"") & 1) == 0 ) {
				    					//log.info(SAL_MODULE_MEMBER_DECLARATION + name.trim() +".*");
				    					log.info(l.trim());
				    					return true;
				    				}
				    			}
				    		}
				    	}
					}						
				}
				
			} catch (IOException e) {
				log.error(e);
			}
			
			return false;
		}
				
}
