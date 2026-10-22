import java.lang.reflect.*; import java.util.*;
public class Runner {
  public static void main(String[] args) throws Exception {
    int ok=0, ko=0;
    for (String cn : args) {
      Class<?> c = Class.forName(cn); Constructor<?> k = c.getDeclaredConstructor(); k.setAccessible(true);
      List<Method> ms = new ArrayList<>(); for (Method m : c.getDeclaredMethods()) if (m.isAnnotationPresent(org.junit.jupiter.api.Test.class)) ms.add(m);
      ms.sort(Comparator.comparing(Method::getName));
      for (Method m : ms) { m.setAccessible(true);
        try { m.invoke(k.newInstance()); ok++; System.out.println("PASA  "+c.getSimpleName()+"."+m.getName()); }
        catch (InvocationTargetException e) { ko++; System.out.println("FALLA "+c.getSimpleName()+"."+m.getName()+" -> "+e.getCause()); } } }
    System.out.println("\nRESULTADO: "+ok+" pasan, "+ko+" fallan, "+(ok+ko)+" en total"); System.exit(ko==0?0:1);
  }
}
