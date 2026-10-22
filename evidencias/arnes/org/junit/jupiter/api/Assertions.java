package org.junit.jupiter.api;
/** Arnés mínimo equivalente a las aserciones usadas (NO es JUnit real). */
public final class Assertions {
    public interface Executable { void execute() throws Throwable; }
    private static void fail(String m){ throw new AssertionError(m); }
    public static void assertTrue(boolean b){ if(!b) fail("esperado true"); }
    public static void assertFalse(boolean b){ if(b) fail("esperado false"); }
    public static void assertNull(Object o){ if(o!=null) fail("esperado null: "+o); }
    public static void assertEquals(int a,int b){ if(a!=b) fail(a+" != "+b); }
    public static void assertEquals(int a,Integer b){ if(b==null||a!=b) fail(a+" != "+b); }
    public static void assertEquals(Integer a,int b){ if(a==null||a!=b) fail(a+" != "+b); }
    public static void assertEquals(long a,long b){ if(a!=b) fail(a+" != "+b); }
    public static void assertEquals(Object a,Object b){ if(!java.util.Objects.equals(a,b)) fail(a+" != "+b); }
    public static <T extends Throwable> T assertThrows(Class<T> t, Executable e){
        try { e.execute(); } catch(Throwable x){ if(t.isInstance(x)) return t.cast(x); fail("lanzó "+x.getClass().getSimpleName()+", esperado "+t.getSimpleName()); }
        fail("no lanzó "+t.getSimpleName()); return null; }
}
