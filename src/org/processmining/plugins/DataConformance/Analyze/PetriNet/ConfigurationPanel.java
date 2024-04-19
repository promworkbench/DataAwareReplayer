package org.processmining.plugins.DataConformance.Analyze.PetriNet;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import com.fluxicon.slickerbox.components.NiceDoubleSlider;
import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class ConfigurationPanel extends JPanel implements ViewInteractionPanel {

	private static final long serialVersionUID = -5452989803410781485L;
	
	private final JCheckBox pruneBox;
	private final NiceDoubleSlider confThreshold;
	private final NiceIntegerSlider minNumInstancePerLeaf;
	private final NiceIntegerSlider numFoldErrorPruning;
	private final JCheckBox saveDataBox;
	private final JCheckBox binaryBox;

	public ConfigurationPanel(final DecisionTreeFrame frame, int numInstances)
	{
		SlickerFactory instance=SlickerFactory.instance();
		pruneBox= instance.createCheckBox("Prune Tree", true);
		binaryBox= instance.createCheckBox("Binary Tree", false);
		saveDataBox=	instance.createCheckBox("Instance data to be associated with tree's elements", false);
		confThreshold = instance.createNiceDoubleSlider("Set confidence threshold for pruning", 0.1, 1, 0.25, Orientation.HORIZONTAL);
		minNumInstancePerLeaf=	instance.createNiceIntegerSlider("Minimum Number of instances per leaf", 2, Math.max(3, numInstances), 3, Orientation.HORIZONTAL);
		numFoldErrorPruning=		instance.createNiceIntegerSlider("Number of folds for reduced error pruning", 2, 10, 2, Orientation.HORIZONTAL);

		setLayout(new GridLayout(7,1));
		add(pruneBox);
		add(binaryBox);
		add(saveDataBox);
		add(confThreshold);
		add(minNumInstancePerLeaf);
		add(numFoldErrorPruning);
		JPanel panel=new JPanel();
		JButton button=SlickerFactory.instance().createButton("Update Decision Tree");
		button.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				try {
					frame.createPanel();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		});;
		panel.add(button);
		add(panel);
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Configuration";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth();
	}

	public void setParent(ScalableViewPanel viewPanel) {
		
	}

	public void setScalableComponent(ScalableComponent scalable) {
	
	}

	public void willChangeVisibility(boolean to) {
		
	}

	public void updated() {

	}

	public boolean prunedTree() {
		return pruneBox.isSelected();
	}

	public float getConfidenceThreshold() {
		return (float) confThreshold.getValue();
	}

	public int getMinNumInstancePerLeaf() {
		return minNumInstancePerLeaf.getValue();
	}

	public int getNumFoldErrorPruning() {
		return numFoldErrorPruning.getValue();
	}

	public boolean binaryTree() {
		return binaryBox.isSelected();
	}

	public boolean saveData() {
		return saveDataBox.isSelected();
	}

}
