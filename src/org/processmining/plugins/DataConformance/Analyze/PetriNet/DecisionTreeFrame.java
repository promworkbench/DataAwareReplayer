package org.processmining.plugins.DataConformance.Analyze.PetriNet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.models.FunctionEstimator.DecisionTreeFunctionEstimator;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

public class DecisionTreeFrame extends JFrame {

	private static final long serialVersionUID = -2077695345210733691L;
	
	private VariablePanel varPanel = null;
	private ScalableViewPanel tvScalable = null;
	private ConfigurationPanel confPanel;
	
	private TupleSet set;
	private JProgressBar progressBar;
	
	private boolean initialLoad = true;

	public DecisionTreeFrame(String label, final PetrinetNode node, final DataPetriNet net, final ResultReplayPetriNetWithData replayResult, final TransEvClassMapping activityMapping) throws Exception {
		super(label);
		this.getContentPane().setLayout(new BorderLayout());
		
		progressBar = new JProgressBar();
		progressBar.setString("Loading ...");
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(true);
		progressBar.setMaximumSize(new Dimension(400, 100));
		
		showProgressBar();
		
		// Build instances on BG thread
		SwingWorker<TupleSet, Object> bgWorker = new SwingWorker<TupleSet, Object>() {

			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#doInBackground()
			 */
			protected TupleSet doInBackground() throws Exception {
				return DecisionTreeCreator.createInstanceSet(node, net, replayResult, activityMapping);
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				try {
					set = get();
					varPanel = new VariablePanel(set.getAttributesNames().keySet(), DecisionTreeFrame.this);
					confPanel = new ConfigurationPanel(DecisionTreeFrame.this, set.getInstances().size());
					createPanel();					
				} catch (Exception e) {
					JOptionPane.showMessageDialog(DecisionTreeFrame.this, e.toString());
				}
			}
			
		};
		
		bgWorker.execute();
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setSize((int) dim.getWidth() / 2, (int) dim.getHeight() / 2);
		setLocation((int) dim.getWidth() / 4, (int) dim.getHeight() / 4);
		setVisible(true);
		// Allow GC to collect the instance data / weka data
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	public void createPanel() throws Exception {
		
		if (varPanel.getSelectedItems().size() == 0) {
			JOptionPane.showMessageDialog(this, "Impossible to build a decision tree if no attribute is selected",
					"Classify instances using Decision Trees", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (tvScalable != null)
			this.getContentPane().remove(tvScalable);
		
		if (!initialLoad) {
			showProgressBar();
		}

		final HashMap<String, org.processmining.models.FunctionEstimator.Type> selectAttrib = new HashMap<>(
				set.getAttributesNames());
		selectAttrib.keySet().retainAll(varPanel.getSelectedItems());
	
		// Build decision tree in a background thread
		SwingWorker<DecisionTreeFunctionEstimator, Object> bgWorker = new SwingWorker<DecisionTreeFunctionEstimator, Object>() {

			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#doInBackground()
			 */
			@Override
			public DecisionTreeFunctionEstimator doInBackground() throws Exception {
				Set<String> outputValues = set.getLiteralAttributeValues().get(set.getClassAttribute());
				DecisionTreeFunctionEstimator dt = new DecisionTreeFunctionEstimator(selectAttrib,
						set.getLiteralAttributeValues(), outputValues.toArray(new String[0]), "", set.getInstances()
								.size());

				for (Map<String, Object> elem : set.getInstances()) {
					HashMap<String, Object> aux = new HashMap<>(elem);
					aux.remove(set.getClassAttribute());
					dt.addInstance(aux, elem.get(set.getClassAttribute()), 1);
				}

				dt.setUnpruned(!confPanel.prunedTree());
				dt.setConfidenceFactor(confPanel.getConfidenceThreshold());
				dt.setMinNumObj(confPanel.getMinNumInstancePerLeaf());
				dt.setNumFolds(confPanel.getNumFoldErrorPruning());
				dt.setBinarySplit(confPanel.binaryTree());
				dt.setSaveData(confPanel.saveData());
				dt.createAndSetTree(null);				
				return dt;
			}

			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				try {
					DecisionTreeFunctionEstimator dt = get();
					final JPanel tv = dt.getVisualization();
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
					tvScalable.addViewInteractionPanel(new Summary(dt.getEvaluation()), SwingConstants.SOUTH);
					hideProgressBar();
					getContentPane().add(tvScalable, BorderLayout.CENTER);
					getContentPane().validate();
					
					initialLoad = false;
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			

		};
		bgWorker.execute();
	}

	private void showProgressBar() {
		getContentPane().add(progressBar, BorderLayout.CENTER);
		getContentPane().validate();
	}
	
	private void hideProgressBar() {
		getContentPane().remove(progressBar);
		getContentPane().validate();
	}

	public void dispose() {
		tvScalable = null;
		set = null;
		varPanel = null;
		confPanel = null;
		progressBar = null;
		super.dispose();
	}
	
	

}
