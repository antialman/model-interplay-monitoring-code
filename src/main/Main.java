package main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import data.DeclareConstraint;
import data.PropositionData;
import data.proposition.AttributeType;
import utils.DeclareModelUtils;
import utils.LogUtils;

public class Main {
	
	private static PropositionData propositionData = new PropositionData();
	
	private static List<DeclareConstraint> declareConstraints;
	private static Map<String, AttributeType> attributeTypeMap;
	
	public static void main(String[] args) {
		//TODO Uncomment and review after the code is done
		//CommandLine cmd = CmdArgsUtil.handleArgs(args);
		//String declareModelPath = cmd.getOptionValue("declareModel");
		//String logPath = cmd.getOptionValue("log");

		String declareModelPath = "input/DrivingTest-Model.decl";
		String logPath = "input/DrivingTest-Log-Positive.xes";
		
		
		//Reading the data needed for propositionalization
		try {
			declareConstraints = DeclareModelUtils.readConstraints(declareModelPath);
			attributeTypeMap = DeclareModelUtils.readAttributeTypes(declareModelPath);
		} catch (FileNotFoundException e) {
			System.out.println("Unable to open Declare model: " + declareModelPath);
			e.printStackTrace();
		}
		
		
		propositionData = DeclareModelUtils.updatePropositionData(declareConstraints, attributeTypeMap, propositionData);
		
		
		
		

		
		//Replaying the event log
		XLog xlog = LogUtils.convertToXlog(logPath);
		for (XTrace xtrace : xlog) {
			for (XEvent xevent : xtrace) {
				propositionData.eventToProposition(xevent);
			}
		}
	}
}
