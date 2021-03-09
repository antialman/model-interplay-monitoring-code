package utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CmdArgsUtil {
	
	private CmdArgsUtil() {
		//Private constructor to avoid unnecessary instantiation of the class
	}
	
	//Handling cmd arguments
	public static CommandLine handleArgs(String[] args) {
		//TODO Review after implementation is done
		Options options = new Options();
		Option declareParam = new Option("d", "declareFile", true, "Declare model path");
		declareParam.setRequired(true);
		options.addOption(declareParam);
		
		Option petrinetParam = new Option("p", "petrinetFile", true, "Petrinet model path");
		petrinetParam.setRequired(true);
		options.addOption(petrinetParam);
		
		Option costParam = new Option("c", "costsFile", true, "Violation costs file path");
		costParam.setRequired(true);
		options.addOption(costParam);

		Option logParam = new Option("l", "log", true, "input event log path");
		logParam.setRequired(true);
		options.addOption(logParam);

		CommandLineParser parser = new org.apache.commons.cli.DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("java -jar .\\InterplayMonitor.jar ", options);
			System.exit(1);
		}

		return cmd;
	}
}
