package jenkins.telemetry.agents;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelemetryAgent extends ClassNotFoundException {
    private final static Logger LOGGER = Logger.getLogger(TelemetryAgent.class.getName());

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        LOGGER.log(Level.INFO, "Runtime: {0}: {1}", new Object[]{runtimeMxBean.getName(), runtimeMxBean.getInputArguments()});
        LOGGER.log(Level.INFO, "Starting agent with arguments " + agentArguments);

        String classToTransform = "java.lang.ClassNotFoundException";
        transformClass(classToTransform, instrumentation);
    }

    public static void agentmain(String agentArguments, Instrumentation instrumentation) {
        premain(agentArguments, instrumentation);
    }

    private static void transformClass(String className, Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();
            transform(targetCls, targetClassLoader, instrumentation);
            return;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Class [{0}] not found with Class.forName", className);
        }

        // otherwise iterate all loaded classes and find what we want
        for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {
            if(clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                return;
            }
        }
        throw new RuntimeException("Failed to find class [" + className + "]");
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
        instrumentation.addTransformer(new CatchExceptionTransformer(clazz.getName(), classLoader), true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for: [" + clazz.getName() + "]", ex);
        }
    }
}
