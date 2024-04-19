package org.processmining.plugins.DeclareConformance.Analyze;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.DataConformance.ResultReplay;

public class Analyze 
{
	private static final String CONFORMING="Conformant";
	private static final String NONCONFORMING="Non-conformant";

	
	@Plugin(name = "4 Classify instances using Decision Trees",
			parameterLabels = { "Alignment"},
			returnLabels = { "Visualization"},
			returnTypes = { JComponent.class },
			userAccessible = false,
			help = "Analyze Alignment")
			@Visualizer
			public JComponent plugin(UIPluginContext context, ResultReplay net) throws Exception 
			{
				return(new AnalyzePanel(net));
			}
}

