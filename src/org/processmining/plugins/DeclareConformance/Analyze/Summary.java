package org.processmining.plugins.DeclareConformance.Analyze;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class Summary extends JPanel implements ViewInteractionPanel{


	public Summary(Classifier tree, Instances instances) {
		Evaluation evaluation;
		StringBuffer sb;
		try {
			evaluation = new Evaluation(instances);
			evaluation.evaluateModel(tree, instances);

			sb=new StringBuffer(evaluation.toSummaryString());
			sb.append(evaluation.toClassDetailsString());	
			sb.append(evaluation.toMatrixString());		
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			JTextArea textArea = new JTextArea(sb.toString());
			textArea.setEditable(false);
			textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
			JScrollPane scrollPane = new JScrollPane(textArea);
			this.setLayout(new BorderLayout());
			add(scrollPane, BorderLayout.CENTER);

	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Summary";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth();
	}

	public void setParent(ScalableViewPanel viewPanel) {
	
	}

	public void setScalableComponent(ScalableComponent scalable) {
		
	}

	public void willChangeVisibility(boolean to) {
		// TODO Auto-generated method stub
		
	}

	public void updated() {
		// TODO Auto-generated method stub
		
	}
}
