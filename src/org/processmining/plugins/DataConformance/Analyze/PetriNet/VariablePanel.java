package org.processmining.plugins.DataConformance.Analyze.PetriNet;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import csplugins.id.mapping.ui.CheckComboBox;

public class VariablePanel extends JPanel implements ViewInteractionPanel {

	private CheckComboBox selectedAttributesBox;
	

	public VariablePanel(Set<String> set, final DecisionTreeFrame decisionTreeFrame) {
		selectedAttributesBox=new CheckComboBox(set);
		selectedAttributesBox.addSelectedItems(set);
		selectedAttributesBox.setToolTipText("Attributes starting with # correspond to the number of occurrences of " +
				"a given activity until the occurrence of a move for the activity");
		
		JButton button=SlickerFactory.instance().createButton("Update Decision Tree");
		button.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				try {
					decisionTreeFrame.createPanel();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		});
		
		JButton buttond=SlickerFactory.instance().createButton("Remove Attributes relative to the occurrences' number");
		buttond.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				HashSet<Object> toSelect=new HashSet<Object>();
				for(Object select : selectedAttributesBox.getSelectedItems())
				{
					if (!((String)select).startsWith("#"))
						toSelect.add(select);
				}
				selectedAttributesBox.clearSelection();
				selectedAttributesBox.addSelectedItems(toSelect);
			}
		});
		
		this.setLayout(new BorderLayout());
		JPanel centerPanel=new JPanel(new BorderLayout());
		centerPanel.add(SlickerFactory.instance().createLabel("Attributes to consider when building decision tree:"),
				BorderLayout.NORTH);
		centerPanel.add(selectedAttributesBox,BorderLayout.CENTER);
		this.add(centerPanel,BorderLayout.CENTER);
		JPanel southPanel=new JPanel();
		southPanel.add(button);
		southPanel.add(buttond);
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
