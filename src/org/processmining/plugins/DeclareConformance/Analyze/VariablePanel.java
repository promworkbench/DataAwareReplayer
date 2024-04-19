package org.processmining.plugins.DeclareConformance.Analyze;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import weka.core.Attribute;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import csplugins.id.mapping.ui.CheckComboBox;

public class VariablePanel extends JPanel implements ViewInteractionPanel {

	private CheckComboBox selectedAttributesBox;
	private AnalyzePanelListener listener;
	
	public VariablePanel(HashMap<String, Attribute> attributeMap,AnalyzePanel analyzePanel) {
		selectedAttributesBox=new CheckComboBox(attributeMap.keySet());
		selectedAttributesBox.addSelectedItems(attributeMap.keySet());
		selectedAttributesBox.setToolTipText("Attributes starting with # correspond to the number of occurrences of " +
				"a given activity in the log trace");
		
		listener=new AnalyzePanelListener(analyzePanel);
		JButton button=SlickerFactory.instance().createButton("Update Decision Tree");
		button.addActionListener(listener);
		
		this.setLayout(new BorderLayout());
		JPanel centerPanel=new JPanel(new BorderLayout());
		centerPanel.add(SlickerFactory.instance().createLabel("Attributes to consider when building decision tree:"),
				BorderLayout.NORTH);
		centerPanel.add(selectedAttributesBox,BorderLayout.CENTER);
		this.add(centerPanel,BorderLayout.CENTER);
		JPanel southPanel=new JPanel();
		southPanel.add(button);
		this.add(southPanel,BorderLayout.SOUTH);
		
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Variables";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth()/2;
	}

	public void setParent(ScalableViewPanel viewPanel) {

	}

	public void setScalableComponent(ScalableComponent scalable) {

	}

	public void willChangeVisibility(boolean to) {

	}

	public void updated() {

	}

	@SuppressWarnings("unchecked")
	public Collection getSelectedItems() {
		return selectedAttributesBox.getSelectedItems();
	}

}
