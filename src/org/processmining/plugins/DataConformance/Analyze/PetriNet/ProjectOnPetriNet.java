package org.processmining.plugins.DataConformance.Analyze.PetriNet;

import java.awt.Color;
import java.util.concurrent.Executors;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithDataFactory;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import weka.gui.GenericObjectEditor;

public class ProjectOnPetriNet {

	@Plugin(name = "1 Projection on the net", level = PluginLevel.PeerReviewed, //
			returnLabels = { "Projection of the alignment onto Data-Aware Petri Net" }, // 
			returnTypes = { JComponent.class }, parameterLabels = { "Matching Instances" }, //
			userAccessible = true)
	@Visualizer
		@PluginVariant(requiredParameterLabels = { 0 })
		public JComponent visualize(PluginContext context, final ResultReplayPetriNetWithData replayResult) throws Exception {
			DataPetriNet original = replayResult.getNet();
			PetriNetWithDataFactory factory=new PetriNetWithDataFactory(original, original.getLabel());
			
			final DataPetriNet cloneNet=factory.getRetValue();
			for(DataElement elem : original.getVariables()) {
				cloneNet.addVariable(elem.getVarName(), elem.getType(), elem.getMinValue(), elem.getMaxValue());
			}
			for(Transition tOrig : original.getTransitions()) {
				PNWDTransition tNew = (PNWDTransition) factory.getTransMapping().get(tOrig);
				if (tOrig instanceof PNWDTransition) {
					for(DataElement elem : ((PNWDTransition)tOrig).getReadOperations()) {
						cloneNet.assignReadOperation(tNew, cloneNet.getVariable(elem.getVarName()));
					}
					for(DataElement elem : ((PNWDTransition)tOrig).getWriteOperations()) {
						cloneNet.assignWriteOperation(tNew, cloneNet.getVariable(elem.getVarName()));
					}
				}
			}
			for(Transition node : cloneNet.getTransitions())
			{
				if (node.isInvisible())
				{
					node.getAttributeMap().remove(AttributeMap.TOOLTIP);
					node.getAttributeMap().put(AttributeMap.FILLCOLOR, new Color(0,0,0,127));
				}
				else
				{
				float actArray[]=replayResult.actArray.get(node.getLabel());
				if (actArray!=null)
				{
					float value;
					if (actArray[0]+actArray[1]==0 || ((PNWDTransition)node).getWriteOperations().size()==0)
						value=(actArray[0]+actArray[1])/(actArray[0]+actArray[1]+actArray[2]+actArray[3]);
					else
					{
						float dataFlowValue=actArray[0]/(actArray[0]+actArray[1]);
						float controlFlowValue=(actArray[0]+actArray[1])/(actArray[0]+actArray[1]+actArray[2]+actArray[3]);
						value=2*dataFlowValue*controlFlowValue/(dataFlowValue+controlFlowValue);
					}
					Color fillColor=getColorForValue(value);
					Color textColor=new Color(255-fillColor.getRed(),255-fillColor.getGreen(),255-fillColor.getBlue());
					node.getAttributeMap().put(AttributeMap.FILLCOLOR, fillColor);
					node.getAttributeMap().put(AttributeMap.LABELCOLOR, textColor);
					StringBuffer tooltip=new StringBuffer("<html><table><tr><td><b>Number moves in both without incorrect write operations:</b> ");
					tooltip.append((int)actArray[0]);
					tooltip.append("</td></tr><tr><td><b>Number moves in both with incorrect write operations:</b> ");
					tooltip.append((int)actArray[1]);
					tooltip.append("</td></tr><tr><td><b>Number moves in log:</b> ");
					tooltip.append((int)actArray[2]);
					tooltip.append("</td></tr><tr><td><b>Number moves in model:</b> ");
					tooltip.append((int)actArray[3]);
					tooltip.append("</td></tr></table></html>");
					node.getAttributeMap().put(AttributeMap.TOOLTIP,tooltip.toString());
				}
				}
			}
			for(DataElement node : cloneNet.getVariables())
			{
				float attrArray[]=replayResult.attrArray.get(node.getVarName());
				if (attrArray != null) {
					float value=1-(attrArray[1]+attrArray[4])/(attrArray[0]+attrArray[1]+attrArray[4]);
					Color fillColor=getColorForValue(value);
					node.getAttributeMap().put(AttributeMap.FILLCOLOR, fillColor);
					StringBuffer tooltip=new StringBuffer("<html><table><tr><td><b>Number of correct write operations:</b> ");
					tooltip.append((int)attrArray[0]);
					tooltip.append("</td></tr><tr><td><b>Number of wrong write operations:</b> ");
					tooltip.append((int)attrArray[4]);
					tooltip.append("</td></tr><tr><td><b>Number of missing write operations:</b> ");
					tooltip.append((int)attrArray[1]);
					tooltip.append("</td></tr></table></html>");
					node.getAttributeMap().put(AttributeMap.TOOLTIP,tooltip.toString());
				} else {
					context.log("Could not get statistics for variable "+node.getVarName(), MessageLevel.WARNING);
				}
			}
			EvClassLogPetrinetConnection conn;
			try {	
				conn = context.getConnectionManager().getFirstConnection(EvClassLogPetrinetConnection.class, context, original,
						replayResult.getAlignedLog());
			} catch (Exception e) {
				JOptionPane.showMessageDialog(new JPanel(), "No mapping can be constructed between the net and the log");
				context.getFutureResult(0).cancel(true);
				return null;
			}

			// init gui for each step
			final TransEvClassMapping activityMapping = (TransEvClassMapping) conn.getObjectWithRole(EvClassLogPetrinetConnection.TRANS2EVCLASSMAPPING);
			
			// Load Weka on a separate thread as this takes quite some time
			Executors.newSingleThreadExecutor().execute(new Runnable() {
				
				public void run() {
					try {
						GenericObjectEditor.class.newInstance();
					} catch (InstantiationException e) {
					} catch (IllegalAccessException e) {
					}					
				}
			});

			return new AnalyzePanel(context, cloneNet, replayResult, activityMapping);
		}



	/**
	 * Associate the value with a Color. Value 0 corresponds to RED colour and Value 1 to GREEN. 
	 * @param value A float value between 0 and 1
	 * @return A Color
	 */
	public Color getColorForValue(float value) {
		assert(value<=1 && value>=0);
		value=0.2F+value*0.8F;
		float red=(float) (Math.min(value, 0.3333)*3);
		value=(float) Math.max(value-0.3333, 0);
		float green=(float) (Math.min(value, 0.3333)*3);
		value=(float) Math.max(value-0.3333, 0);		
		float blue=(float) (Math.min(value, 0.3333)*3);
		return new Color(red,green,blue);
	}
}
