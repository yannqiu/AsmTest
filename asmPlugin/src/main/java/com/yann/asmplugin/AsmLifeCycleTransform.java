package com.yann.asmplugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

public class AsmLifeCycleTransform extends Transform {

    private String name;

    public AsmLifeCycleTransform(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        System.out.println("---MyPlugin--- transform");
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider transformOutputProvider = transformInvocation.getOutputProvider();
        if (transformOutputProvider != null) {
            transformOutputProvider.deleteAll();
        }
        inputs.forEach(transformInput -> {
            transformInput.getDirectoryInputs().forEach( directoryInput -> {
                try {
                    handleDirectory(directoryInput, transformOutputProvider);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            transformInput.getJarInputs().forEach(jarInput -> {
                try {
                    handleJar(jarInput, transformOutputProvider);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void handleDirectory(DirectoryInput directoryInput, TransformOutputProvider transformOutputProvider) throws IOException {
        System.out.println("---MyPlugin--- handleDirectory");
        List<File> fileList = new ArrayList<>();
        if (directoryInput.getFile().isDirectory()) {
            FileUtils.getAllFiles(directoryInput.getFile(), fileList);
        } else {
            fileList.add(directoryInput.getFile());
        }
        fileList.forEach(file -> {
            String name = file.getName();
            System.out.println("---MyPlugin--- name:" + name);
            if (checkClassFile(name)) {
                System.out.println("---MyPlugin--- handleDirectory");
                try {
                    ClassReader classReader = new ClassReader(FileUtils.getBytes(file));
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = new LifecycleClassVisitor(classWriter);
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES);
                    byte[] code = classWriter.toByteArray();
                    FileOutputStream fos = new FileOutputStream(file.getParentFile().getAbsolutePath() + File.separator + name);
                    fos.write(code);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        File dest = transformOutputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
        org.apache.commons.io.FileUtils.copyDirectory(directoryInput.getFile(), dest);
    }

    private boolean checkClassFile(String name) {
        return (name.endsWith(".class") && !name.startsWith("R")
                && !"R.class".equals(name) && !"BuildConfig.class".equals(name)
                && "androidx/appcompat/app/AppCompatActivity.class".equals(name));
    }

    private void handleJar(JarInput jarInput, TransformOutputProvider transformOutputProvider) throws IOException {
        System.out.println("---MyPlugin--- handleJar" + jarInput.getFile().getAbsolutePath());
        if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
            //重命名输出文件,因为可能同名,会覆盖
            String jarName = jarInput.getName();
            String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4);
            }
            JarFile jarFile = new JarFile(jarInput.getFile());
            Enumeration enumeration = jarFile.entries();
            File tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_temp.jar");
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
            //用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                System.out.println("---MyPlugin---  entryName " + entryName);
                //插桩class
                if (checkClassFile(entryName)) {
                    //class文件处理
                    System.out.println("---MyPlugin--- check entryName " + entryName);
                    jarOutputStream.putNextEntry(zipEntry);
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = new LifecycleClassVisitor(classWriter);
                    classReader.accept(cv, EXPAND_FRAMES);
                    byte[] code = classWriter.toByteArray();
                    jarOutputStream.write(code);
                } else {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            //结束
            jarOutputStream.close();
            jarFile.close();
            File dest = transformOutputProvider.getContentLocation(jarName + md5Name,
                    jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            org.apache.commons.io.FileUtils.copyFile(tmpFile, dest);
            tmpFile.delete();
        }
    }
}
