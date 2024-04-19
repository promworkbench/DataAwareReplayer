package org.processmining.plugins.DataConformance.Analyze.PetriNet;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;
import org.processmining.plugins.DataConformance.framework.ExecutionStep;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;
import org.processmining.plugins.DataConformance.visualization.alignment.ColorTheme;
import org.processmining.plugins.DataConformance.visualization.alignment.XTraceResolver;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignmentMasterDetail;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignmentMasterView.GroupedAlignmentInput;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignments;
import org.processmining.plugins.DataConformance.visualization.grouping.GroupedAlignmentsSimpleImpl;
import org.processmining.plugins.balancedconformance.export.XAlignmentConverter;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;

public class AnalyzePanel extends JPanel {

	private static final long serialVersionUID = 5707969720823392664L;

	private static class AnalyzePanelListener extends MouseAdapter {

		private final ProMJGraphPanel panel;
		private final ResultReplayPetriNetWithData replayResult;
		private final PluginContext context;
		private final DataPetriNet net;
		private final TransEvClassMapping activityMapping;

		public AnalyzePanelListener(PluginContext context, ProMJGraphPanel panel, DataPetriNet net,
				ResultReplayPetriNetWithData replayResult, TransEvClassMapping activityMapping) {
			this.context = context;
			this.panel = panel;
			this.net = net;
			this.replayResult = replayResult;
			this.activityMapping = activityMapping;
		}

		public void mouseClicked(MouseEvent arg0) {

			//FM, changed to double click
			if (arg0.getClickCount() == 2) {
				Collection<DirectedGraphNode> selectNodes = panel.getSelectedNodes();
				panel.getGraph().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				if (selectNodes.size() > 0) {
					PetrinetNode node = (PetrinetNode) selectNodes.iterator().next();

					try {
						new DecisionTreeFrame(node.getLabel(), node, net, replayResult, activityMapping);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(panel, e.toString());
						e.printStackTrace();
					}

				}
				panel.getGraph().setCursor(Cursor.getDefaultCursor());
			}

		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger())
				showMenu(e);
		}

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger())
				showMenu(e);
		}

		private void showMenu(MouseEvent e) {
			Collection<DirectedGraphNode> selectNodes = panel.getSelectedNodes();
			if (selectNodes.size() > 0) {
				JPopupMenu menu = new JPopupMenu();
				final Transition node = getSelectedTransition(selectNodes);

				if (node != null) {
					JMenuItem menuDecisionTree = new JMenuItem("Show Decision Tree for " + node.getLabel());
					menuDecisionTree.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							try {
								new DecisionTreeFrame(node.getLabel(), node, net, replayResult, activityMapping);
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(panel, ex.toString());
								ex.printStackTrace();
							}
						}
					});
					menu.add(menuDecisionTree);

					JMenuItem menuShowViolationDetails = new JMenuItem(
							"Show Alignments with violations for " + node.getLabel());
					menuShowViolationDetails.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							final JFrame groupedAlignmentFrame = new JFrame(
									"Alignments with violations for " + node.getLabel());

							SwingWorker<GroupedAlignmentInput<XAlignment>, Object> bgWorker = new SwingWorker<GroupedAlignmentInput<XAlignment>, Object>() {

								@Override
								public GroupedAlignmentInput<XAlignment> doInBackground() {
									final Map<String, XTrace> traceMap = new HashMap<>();
									ArrayList<XAlignment> filteredAlignments = new ArrayList<>();
									XAlignmentConverter converter = new XAlignmentConverter();
									converter.setClassifier(replayResult.getClassifier());
									converter.setVariableMapping(replayResult.getVariableMapping());
									for (XTrace trace : replayResult.getAlignedLog()) {
										String traceName = XConceptExtension.instance().extractName(trace);
										Alignment alignment = replayResult.getAlignmentByTraceName(traceName);
										XAlignment xAlignment = converter.viewAsXAlignment(alignment, trace);
										if (alignment != null && hasTransitionWithViolation(node, alignment)) {
											filteredAlignments.add(xAlignment);
											traceMap.put(traceName, trace);
										}
									}
									Map<String, Color> activityColorMap = ColorTheme.createColorMap(filteredAlignments);
									GroupedAlignments<XAlignment> groupedResult = new GroupedAlignmentsSimpleImpl(
											filteredAlignments, activityColorMap);
									return new GroupedAlignmentInput<>(groupedResult, new XTraceResolver() {

										public boolean hasOriginalTraces() {
											return true;
										}

										public XTrace getOriginalTrace(String name) {
											return traceMap.get(name);
										}
									}, activityColorMap);
								}

								@Override
								protected void done() {
									try {
										GroupedAlignmentInput<XAlignment> input = get();
										groupedAlignmentFrame.getContentPane()
												.add(new GroupedAlignmentMasterDetail(context, input));
										groupedAlignmentFrame.getContentPane().validate();
									} catch (InterruptedException e) {
										e.printStackTrace();
									} catch (ExecutionException e) {
										JOptionPane.showMessageDialog(panel, e.toString());
										e.printStackTrace();
									}
								}

							};
							bgWorker.execute();

							Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
							groupedAlignmentFrame.setSize((int) dim.getWidth(), (int) dim.getHeight());
							groupedAlignmentFrame.setLocation(0, 0);
							groupedAlignmentFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							groupedAlignmentFrame.setVisible(true);
						}

						private boolean hasTransitionWithViolation(final PetrinetNode node, Alignment a) {
							boolean include = false;
							int index = 0;
							for (ExecutionStep step : a.getProcessTrace()) {
								DataAwareStepTypes type = a.getStepTypes().get(index);
								if (node.getLabel().equals(step.getActivity())) {
									if (type != DataAwareStepTypes.LMGOOD && type != DataAwareStepTypes.MINVI) {
										include = true;
									}
								} else {
									// Log step, compare label
									ExecutionStep logStep = a.getLogTrace().get(index);
									if (node.getLabel().equals(logStep.getActivity())) {
										if (type != DataAwareStepTypes.LMGOOD && type != DataAwareStepTypes.MINVI) {
											include = true;
										}
									}
								}
								index++;
							}
							return include;
						}

					});
					menu.add(menuShowViolationDetails);

					JMenuItem menuShowDetails = new JMenuItem("Show Alignments containing '" + node.getLabel() + "'");
					menuShowDetails.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							final JFrame groupedAlignmentFrame = new JFrame(
									"Alignments containing '" + node.getLabel() + "'");

							SwingWorker<GroupedAlignmentInput<XAlignment>, Object> bgWorker = new SwingWorker<GroupedAlignmentInput<XAlignment>, Object>() {

								@Override
								public GroupedAlignmentInput<XAlignment> doInBackground() {
									final Map<String, XTrace> traceMap = new HashMap<>();
									List<XAlignment> filteredAlignments = new ArrayList<>();
									XAlignmentConverter converter = new XAlignmentConverter();
									converter.setClassifier(replayResult.getClassifier());
									converter.setVariableMapping(replayResult.getVariableMapping());
									for (XTrace trace : replayResult.getAlignedLog()) {
										String traceName = XConceptExtension.instance().extractName(trace);
										if (traceName != null) {
											Alignment alignment = replayResult.getAlignmentByTraceName(traceName);
											XAlignment xAlignments = converter.viewAsXAlignment(alignment, trace);
											if (hasTransition(node, alignment)) {
												filteredAlignments.add(xAlignments);
												traceMap.put(traceName, trace);
											}
										}
									}
									Map<String, Color> activityColorMap = ColorTheme.createColorMap(filteredAlignments);
									GroupedAlignments<XAlignment> groupedResult = new GroupedAlignmentsSimpleImpl(
											filteredAlignments, activityColorMap);
									return new GroupedAlignmentInput<>(groupedResult, new XTraceResolver() {

										public boolean hasOriginalTraces() {
											return true;
										}

										public XTrace getOriginalTrace(String name) {
											return traceMap.get(name);
										}
									}, activityColorMap);
								}

								@Override
								protected void done() {
									try {
										GroupedAlignmentInput<XAlignment> input = get();
										GroupedAlignmentMasterDetail masterDetail = new GroupedAlignmentMasterDetail(
												context, input);
										groupedAlignmentFrame.getContentPane().add(masterDetail);
										groupedAlignmentFrame.getContentPane().validate();
									} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
									} catch (ExecutionException e) {
										ProMUIHelper.showErrorMessage(panel, e.toString(), "Error", e);
									}
								}

							};
							bgWorker.execute();

							Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
							groupedAlignmentFrame.setSize((int) dim.getWidth(), (int) dim.getHeight());
							groupedAlignmentFrame.setLocation(0, 0);
							groupedAlignmentFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							groupedAlignmentFrame.setVisible(true);
						}

						private boolean hasTransition(final PetrinetNode node, Alignment a) {
							int index = 0;
							for (ExecutionStep step : a.getProcessTrace()) {
								ExecutionStep logStep = a.getLogTrace().get(index);
								//TODO fix the ugly cloning of the original net, and use equals on the node
								if ((step.getActivityObject() instanceof PetrinetNode
										&& ((PetrinetNode) step.getActivityObject()).getLabel().equals(node.getLabel()))
										|| (logStep.getActivity() != null
												&& logStep.getActivity().equals(node.getLabel()))) {
									return true;
								}
								index++;
							}
							return false;
						}
					});

					menu.add(menuShowDetails);
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}

		private Transition getSelectedTransition(Collection<DirectedGraphNode> selectNodes) {
			Iterator<DirectedGraphNode> iterator = selectNodes.iterator();
			Transition node = null;
			while (iterator.hasNext() && node == null) {
				DirectedGraphNode nextNode = iterator.next();
				if (nextNode instanceof Transition && !((Transition) nextNode).isInvisible()) {
					return (Transition) nextNode;
				}
			}
			return null;
		}

	}

	private final ProMJGraphPanel panel;

	public AnalyzePanel(PluginContext context, DataPetriNet net, ResultReplayPetriNetWithData replayResult,
			TransEvClassMapping activityMapping) throws Exception {
		setLayout(new GridLayout(1, 1));

		ViewSpecificAttributeMap viewSpecificMap = new ViewSpecificAttributeMap();
		this.panel = ProMJGraphVisualizer.instance().visualizeGraph(context, net, viewSpecificMap);
		this.panel.getGraph().setEditable(false);
		this.panel.getGraph()
				.addMouseListener(new AnalyzePanelListener(context, panel, net, replayResult, activityMapping));
		this.add(panel);
	}

}