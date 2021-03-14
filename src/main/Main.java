package main;

import java.io.FileNotFoundException;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import data.PropositionData;
import utils.DeclareModelUtils;
import utils.LogUtils;

public class Main {
	
	private static PropositionData propositionData = new PropositionData();
	
	public static void main(String[] args) {
		//TODO Uncomment and review after the code is done
		//CommandLine cmd = CmdArgsUtil.handleArgs(args);
		//String declareModelPath = cmd.getOptionValue("declareModel");
		//String logPath = cmd.getOptionValue("log");

		String declareModelPath = "input/DrivingTest-Model.decl";
		String logPath = "input/DrivingTest-Log-Positive.xes";
		
		
		//Reading the data needed for propositionalization
		try {
			DeclareModelUtils.readPropositionData(declareModelPath, propositionData);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open Declare model: " + declareModelPath);
			e.printStackTrace();
		}
		

		
		//Replaying the event log
		XLog xlog = LogUtils.convertToXlog(logPath);
		for (XTrace xtrace : xlog) {
			for (XEvent xevent : xtrace) {
				propositionData.eventToProposition(xevent);
			}
		}
		
	}

}
