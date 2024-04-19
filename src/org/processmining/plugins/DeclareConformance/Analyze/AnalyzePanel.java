package org.processmining.plugins.DeclareConformance.Analyze;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.ResultReplay;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

public class AnalyzePanel extends JPanel {
	private static final String CONFORMING = "Yes";
	private static final String NONCONFORMING = "No";
	private HashMap<String, Attribute> attributeMap = new HashMap<String, Attribute>();
	private String options[] = { "-A" };
	private ResultReplay resReplay;
	private Attribute classAttribute;
	VariablePanel varPanel = null;
	private ScalableViewPanel tvScalable;
	private ConfigurationPanel confPanel;

	public void removeTree() {
		this.remove(tvScalable);
	}

	public AnalyzePanel(ResultReplay net) throws Exception {
		setLayout(new GridLayout(1, 1));
		ArrayList<String> classValues = new ArrayList<String>();
		classValues.add(CONFORMING);
		classValues.add(NONCONFORMING);
		classAttribute = new Attribute("Conforming", classValues);
		attributeMap = generateAttributes(net.getAlignedLog());
		resReplay = net;
		varPanel = new VariablePanel(attributeMap, this);
		confPanel = new ConfigurationPanel(net.getAlignedLog().size(), this);
		createPanel();
	}

	public void createPanel() throws Exception {
		if (varPanel.getSelectedItems().size() == 0) {
			JOptionPane.showMessageDialog(this, "Impossible to build a decision tree if no attribute is selected",
					"Classify instances using Decision Trees", JOptionPane.ERROR_MESSAGE);
			return;
		}
		ArrayList<Attribute> selectedAttributes = new ArrayList<Attribute>();
		for (Object attrib : varPanel.getSelectedItems())
			selectedAttributes.add(attributeMap.get(attrib));
		selectedAttributes.add(classAttribute);
		Instances instances = new Instances("Classification", selectedAttributes, resReplay.getAlignedLog().size());
		instances.setClassIndex(instances.numAttributes() - 1);
		int i = 0;
		for (XTrace trace : resReplay.getAlignedLog()) {
			String traceName = XConceptExtension.instance().extractName(trace);			
			Alignment al = resReplay.getAlignmentByTraceName(traceName == null ? String.valueOf(i) : traceName);
			if (al != null) {
				Instance newInstance = createInstance(instances, trace, al.getFitness() == 1);
				instances.add(newInstance);	
			} else {
				JOptionPane.showMessageDialog(this, "Missing Alignment for Trace "+XConceptExtension.instance().extractName(trace), "Missing Alignment", JOptionPane.WARNING_MESSAGE);
			}
			i++;
		}

		J48 tree = new J48();
		tree.setOptions(options);
		tree.setUnpruned(!confPanel.prunedTree());
		tree.setBinarySplits(confPanel.binaryTree());
		tree.setSaveInstanceData(confPanel.saveData());
		tree.setConfidenceFactor(confPanel.getConfidenceThreshold());
		tree.setMinNumObj(confPanel.getMinNumInstancePerLeaf());
		tree.setNumFolds(confPanel.getNumFoldErrorPruning());
		tree.buildClassifier(instances);
		final TreeVisualizer tv = new TreeVisualizer(null, tree.graph(), new PlaceNode2());
		tv.setBackground(Color.GRAY);
		tvScalable = new ScalableViewPanel(new ScalableComponent() {

			public void setScale(double newScale) {
			}

			public void removeUpdateListener(UpdateListener listener) {
			}

			public double getScale() {
				return 1;
			}

			public JComponent getComponent() {
				return tv;
			}

			public void addUpdateListener(UpdateListener listener) {
			}
		});
		tvScalable.addViewInteractionPanel(varPanel, SwingConstants.EAST);
		tvScalable.addViewInteractionPanel(confPanel, SwingConstants.EAST);
		tvScalable.addViewInteractionPanel(new Summary(tree, instances), SwingConstants.SOUTH);
		this.add(tvScalable);
	}

	private Instance createInstance(Instances instances, XTrace trace, boolean fitting) {
		HashSet<String> attributesConsidered = new HashSet<String>();
		Instance newInstance = new DenseInstance(instances.numAttributes());
		Iterator<XEvent> iter = trace.iterator();
		HashSet<XAttribute> map = new HashSet<XAttribute>();
		for (Entry<String, XAttribute> attr : trace.getAttributes().entrySet())
			if (!attr.getKey().contains(":"))
				map.add(attr.getValue());
		HashMap<String, Integer> numExecutionActivities = new HashMap<String, Integer>();
		if (iter.hasNext()) {
			do {
				XEvent event = iter.next();
				map.addAll(event.getAttributes().values());
				String conceptName = XConceptExtension.instance().extractName(event);
				if (conceptName != null) {
					Integer numExecutions = numExecutionActivities.get(conceptName);
					if (numExecutions == null)
						numExecutionActivities.put(conceptName, 1);
					else
						numExecutionActivities.put(conceptName, numExecutions + 1);
				}
				for (XAttribute xattr : map) {
					if (attributesConsidered.contains(xattr.getKey()))
						continue;
					Attribute attr = instances.attribute(xattr.getKey());
					if (attr == null)
						continue;
					if (xattr instanceof XAttributeLiteral) {
						newInstance.setValue(attr, ((XAttributeLiteral) xattr).getValue());
					} else if (xattr instanceof XAttributeTimestamp) {
						newInstance.setValue(attr, ((XAttributeTimestamp) xattr).getValue().getTime());

					} else if (xattr instanceof XAttributeDiscrete) {
						newInstance.setValue(attr, ((XAttributeDiscrete) xattr).getValue());
					} else if (xattr instanceof XAttributeContinuous) {
						newInstance.setValue(attr, ((XAttributeContinuous) xattr).getValue());
					}
				}
				map.clear();
			} while (iter.hasNext());
		}
		for (Entry<String, Integer> numExecutions : numExecutionActivities.entrySet()) {
			Attribute attr = instances.attribute("#" + numExecutions.getKey());
			if (attr != null)
				newInstance.setValue(attr, numExecutions.getValue());
		}
		if (fitting)
			newInstance.setValue(instances.classAttribute(), CONFORMING);
		else
			newInstance.setValue(instances.classAttribute(), NONCONFORMING);
		return newInstance;
	}

	private HashMap<String, Attribute> generateAttributes(XLog log) {
		HashMap<String, Attribute> attributesMap = new HashMap<String, Attribute>();
		HashMap<String, HashSet<String>> stringAttribute = new HashMap<String, HashSet<String>>();
		HashSet<String> conceptNames = new HashSet<String>();
		for (XTrace t : log) {
			Iterator<XEvent> iter = t.iterator();

			HashSet<XAttribute> map = new HashSet<XAttribute>();
			for (Entry<String, XAttribute> attr : t.getAttributes().entrySet())
				if (!attr.getKey().contains(":"))
					map.add(attr.getValue());
			if (iter.hasNext()) {
				do {
					XEvent event = iter.next();
					map.addAll(event.getAttributes().values());
					String conceptName = XConceptExtension.instance().extractName(event);
					if (conceptName != null)
						conceptNames.add(conceptName);
					for (XAttribute attr : map) {

						if (attr instanceof XAttributeLiteral) {
							HashSet<String> values = stringAttribute.get(attr.getKey());
							if (values == null) {
								values = new HashSet<String>();
								stringAttribute.put(attr.getKey(), values);
							}
							values.add(((XAttributeLiteral) attr).getValue());
						} else if (!attributesMap.containsKey(attr.getKey())) {
							if (attr instanceof XAttributeTimestamp) {
								attributesMap.put(attr.getKey(), new Attribute(attr.getKey()));
							} else if (attr instanceof XAttributeDiscrete || attr instanceof XAttributeContinuous) {
								attributesMap.put(attr.getKey(), new Attribute(attr.getKey()));
							}
						}
					}
					map.clear();
				} while (iter.hasNext());
			}
		}
		for (Entry<String, HashSet<String>> entry : stringAttribute.entrySet())
			attributesMap.put(entry.getKey(), new Attribute(entry.getKey(), new ArrayList<String>(entry.getValue())));
		for (String conceptName : conceptNames)
			attributesMap.put("#" + conceptName, new Attribute("#" + conceptName));
		return attributesMap;
	}
}
