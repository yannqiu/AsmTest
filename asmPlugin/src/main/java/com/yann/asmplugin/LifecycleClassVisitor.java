package com.yann.asmplugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 */
public class LifecycleClassVisitor extends ClassVisitor {

    private String mClassName;

    public LifecycleClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        System.out.println("---MyPlugin--- : visit -----> started ：" + name);
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        System.out.println("---MyPlugin--- : visitMethod : " + name);
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        //匹配Activity
        if ("androidx/appcompat/app/AppCompatActivity".equals(this.mClassName)) {
            if ("onCreate".equals(name) ) {
                System.out.println("---MyPlugin--- : change method ----> " + name);
                return new LifecycleOnCreateMethodVisitor(mv);
            } else if ("onDestroy".equals(name)) {
                System.out.println("---MyPlugin--- : change method ----> " + name);
                return new LifecycleOnDestroyMethodVisitor(mv);
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        System.out.println("---MyPlugin--- : visit -----> end");
        super.visitEnd();
    }
}
