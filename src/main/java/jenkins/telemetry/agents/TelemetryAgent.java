package jenkins.telemetry.agents;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelemetryAgent extends ClassNotFoundException {
    private final static Logger logger = Logger.getLogger(TelemetryAgent.class.getName());

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        logger.log(Level.INFO, "Runtime: {0}: {1}", new Object[]{runtimeMxBean.getName(), runtimeMxBean.getInputArguments()});
        logger.log(Level.INFO, "Starting agent with arguments " + agentArguments);

        // parse the arguments:
        // graphite.host=localhost,graphite.port=2003
      /*  Map<String, String> properties = new HashMap<String, String>();
        for (String propertyAndValue : agentArguments.split(",")) {
            String[] tokens = propertyAndValue.split(":", 2);
            if (tokens.length != 2) {
                continue;
            }
            properties.put(tokens[0], tokens[1]);

        }*/
        for (Class c : instrumentation.getAllLoadedClasses()){
            System.out.println(c.getSimpleName());
        }
        instrumentation.addTransformer(new CatchExceptionTransformer(), true);
        try {
            instrumentation.retransformClasses(java.lang.ClassNotFoundException.class);
        } catch (UnmodifiableClassException e) {
            //TODO: change for something more appropriate
            e.printStackTrace();
        }
    }
}
