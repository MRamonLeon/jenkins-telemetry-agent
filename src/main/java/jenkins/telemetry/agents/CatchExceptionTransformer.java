package jenkins.telemetry.agents;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.LoaderClassPath;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CatchExceptionTransformer implements ClassFileTransformer {
    private final static Logger LOGGER = Logger.getLogger(TelemetryAgent.class.getName());

    private String className;
    private ClassLoader classLoader;

    public CatchExceptionTransformer(String className, ClassLoader classLoader) {
        this.className = className.replaceAll("\\.", "/");
        this.classLoader = classLoader;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;

        // This method is called at every class loaded, we transform only the specific class
        // We have the class loader null (default) and WebAppClassLoader=Jenkins v2.157-SNAPSHOT@1f0f1111 (jenkins)
        if (className.equals(this.className) /*&& loader.equals(this.classLoader)*/) {
            LOGGER.log(Level.WARNING, "Transforming '%s'%n", classBeingRedefined);

            CtClass cl = null;
            try {
                ClassPool classPool = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(loader));
                cl = classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));

                // add a message in the constructors
                CtConstructor[] constructors = cl.getDeclaredConstructors();
                for (CtConstructor con : constructors) {
                    con.insertAfter("System.out.println(\"A ClassNotFoundException has been created, cause:\" + getMessage()); printStackTrace();");
                }

                //add a field to test
                //CtField field = CtField.make("public String text = \"Hello world\";", cl);
                //cl.addField(field)
                //It gives me:
                //Caused by: java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the schema (add/remove fields)
                //Because instrumentation doesn't allow changing fields, arguments on methods, and some other changes

                //get the byte array of the class
                byteCode = cl.toBytecode();

            } catch (IOException | CannotCompileException e) {
                LOGGER.log(Level.SEVERE, "Exception transforming " + classBeingRedefined, e);
            } finally {
                if (cl != null) {
                    cl.detach();
                }
            }
        }/*else{
            System.out.format("Avoid transform %s with class loader %s. Only transform %s in class loader %s%n", className, loader, this.className, this.classLoader);
        }*/

        return byteCode;
    }
}
