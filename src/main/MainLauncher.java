package main;

public class MainLauncher {
	public static void main(String[] args) {
		// required Helper Class that avoids Missing Components exception when starting runnable Jar
		// Alternative would be to add this to VM options: --module-path /path/to/JavaFX/lib --add-modules=javafx.controls
		boolean runGui = true;
		if (args.length != 0) {
			for (String arg : args) {
				if (arg.equals("--cmd")) {
					runGui = false;
					MainCmd.main(args);
					break;
				}
			}
		}
		if (runGui) {
			MainGui.main(args);
		}
	}
}
