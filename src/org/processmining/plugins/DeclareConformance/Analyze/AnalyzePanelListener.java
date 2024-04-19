package org.processmining.plugins.DeclareConformance.Analyze;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class AnalyzePanelListener implements ActionListener
{

	private AnalyzePanel panel;

	public AnalyzePanelListener(AnalyzePanel analyzePanel) {
		panel=analyzePanel;
	}

	public void actionPerformed(ActionEvent e) {
		try {
			panel.removeTree();
			panel.createPanel();
			panel.validate();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
}
