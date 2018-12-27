package jenkins.telemetry.agents;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.LoaderClassPath;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public class CatchExceptionTransformer implements ClassFileTransformer {

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(className.endsWith("ClassNotFoundException")){
            System.out.format("Transforming '%s'%n", className);
            //ClassPool classPool = new ClassPool();
            //classPool.appendClassPath(new LoaderClassPath(loader));

            CtClass cl = null;
            try {
                cl = createNewClassInJenkinsUberClassLoader(loader, classfileBuffer);

                if (cl.isInterface() == false) {
                    // add a message in the constructors
                    CtConstructor[] constr = cl.getDeclaredConstructors();
                    for (CtConstructor con:constr) {
                        con.insertAfter("System.out.println(\"A ClassNotFoundException has been created, cause:\" + getMessage()); printStackTrace();");
                    }

                    //It gives me:
                    //Caused by: java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the schema (add/remove fields)

                    //add a field to test
                    //CtField field = CtField.make("public String text = \"Hello world\";", cl);
                    //cl.addField(field);

                    //get the byte array of the class
                    classfileBuffer = cl.toBytecode();
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (cl != null) {
                    cl.detach();
                }
            }
        } else {
            //System.out.format("Not transforming the class: '%s'%n", className);
        }

        return classfileBuffer;
    }

    private CtClass createNewClassInJenkinsUberClassLoader(ClassLoader loader, byte[] classfileBuffer) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException {
        /*Class jenkinsClass = loader.loadClass("jenkins.model.Jenkins");
        Method factoryMethod = jenkinsClass.getDeclaredMethod("get");
        Object jenkins = factoryMethod.invoke(null, null);

        Field pluginManagerField = jenkinsClass.getDeclaredField("pluginManager");
        Object pluginManager = pluginManagerField.get(jenkins);

        Field uberClassLoaderField = pluginManager.getClass().getField("uberClassLoader");
        Object uberClassLoader = uberClassLoaderField.get(pluginManager);

*/
        CtClass cl = null;
        ClassPool classPool = ClassPool.getDefault();
        cl = classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));

        return cl;
    }
}
