package test;

import java.io.File;

import ts.ICompletionEntry;
import ts.ICompletionInfo;
import ts.TSException;
import ts.server.ITypeScriptServiceClient;
import ts.server.collectors.CompletionInfo;
import ts.server.nodejs.NodeJSTypeScriptServiceClient;

public class Main {

	public static void main(String[] args) throws InterruptedException, TSException {

		// sample2.ts has the following content: 
		// var s = "";s.
		String fileName = "sample2.ts";
		
		// Create TypeScript client
		ITypeScriptServiceClient client = new NodeJSTypeScriptServiceClient(new File("./samples"), new File("../../core/ts.repository/node_modules/typescript/bin/tsserver"), null);
		
		// Open "sample2.ts" in an editor
		client.openFile(fileName);
		
		// Do completion after the last dot of "s" variable which is a String (charAt, ....)
		CompletionInfo completionInfo = new CompletionInfo(null);
		client.completions(fileName, 1, 14, null, completionInfo);
		display(completionInfo);

		// Update the editor content to set s as number
		client.updateFile(fileName, "var s = 1;s.");
				
		// Do completion after the last dot of "s" variable which is a Number (toExponential, ....)
		completionInfo = new CompletionInfo(null);
		client.completions(fileName, 1, 14, null, completionInfo);
		display(completionInfo);
		
		client.join();
		client.dispose();
		
	}

	private static void display(ICompletionInfo completionInfo) {
		System.out.println("getCompletionsAtLineOffset:");
		ICompletionEntry[] entries = completionInfo.getEntries();
		for (ICompletionEntry entry : entries) {
			System.out.println(entry.getName());	
		}
	}
}
