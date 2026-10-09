package pzmousefix;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static java.lang.constant.ConstantDescs.*;

/** Usage: -javaagent:pz-mouse-fix.jar=fix | log | fix,log */
public final class Agent {
    private static final ClassDesc FIX = ClassDesc.of("pzmousefix.MouseFix");
    private static final String MOUSE_LWJGLX = "org/lwjglx/input/Mouse";
    private static final String MOUSE_STATE = "zombie/input/MouseState";

    public static void premain(String args, Instrumentation inst) {
        String a = args == null ? "fix" : args;
        MouseFix.init(a.contains("fix"), a.contains("log"));
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader l, String name, Class<?> c, ProtectionDomain pd, byte[] buf) {
                try {
                    if (MOUSE_LWJGLX.equals(name)) { MouseFix.note("patched " + name); return patchAddButtonEvent(buf); }
                    if (MOUSE_STATE.equals(name)) { MouseFix.note("patched " + name); return patchPoll(buf); }
                } catch (Throwable t) {
                    System.err.println("[pz-mouse-fix] failed to patch " + name + ": " + t);
                }
                return null;
            }
        });
    }

    private static byte[] patchAddButtonEvent(byte[] buf) {
        ClassFile cf = ClassFile.of();
        ClassModel cm = cf.parse(buf);
        return cf.transformClass(cm, (cb, ce) -> {
            if (ce instanceof MethodModel mm && mm.methodName().equalsString("addButtonEvent")
                    && mm.methodType().equalsString("(IZ)V") && mm.code().isPresent()) {
                cb.transformMethod(mm, MethodTransform.transformingCode(new CodeTransform() {
                    @Override
                    public void atStart(CodeBuilder b) {
                        b.iload(0).iload(1).invokestatic(FIX, "onButton", MethodTypeDesc.of(CD_void, CD_int, CD_boolean));
                    }
                    @Override
                    public void accept(CodeBuilder b, CodeElement e) { b.with(e); }
                }));
            } else {
                cb.with(ce);
            }
        });
    }

    private static byte[] patchPoll(byte[] buf) {
        ClassFile cf = ClassFile.of();
        ClassModel cm = cf.parse(buf);
        return cf.transformClass(cm, (cb, ce) -> {
            if (ce instanceof MethodModel mm && mm.methodName().equalsString("poll") && mm.code().isPresent()) {
                cb.transformMethod(mm, MethodTransform.transformingCode((b, e) -> {
                    if (e instanceof InvokeInstruction i && i.owner().asInternalName().equals(MOUSE_LWJGLX)
                            && i.name().equalsString("isButtonDown")) {
                        b.invokestatic(FIX, "isDown", MethodTypeDesc.of(CD_boolean, CD_int));
                    } else {
                        b.with(e);
                    }
                }));
            } else {
                cb.with(ce);
            }
        });
    }
}
