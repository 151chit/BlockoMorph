package net.blockomorph;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public abstract class WrapMixinAnnotationsTask extends DefaultTask {
	private static final String WRAPPER = "L" +
			"net.blockomorph.interfaceInjectionSupport.InjectorWrapper"
					.replace(".", "/") + ";";

	@InputDirectory
	public abstract DirectoryProperty getClassesDir();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDir();

	@TaskAction
	public void execute() throws IOException {
		File classesFolder = getClassesDir().get().getAsFile();
		File outputFolder = getOutputDir().get().getAsFile();

		List<String> infos = new ArrayList<>();
		Files.walk(classesFolder.toPath()).filter(path -> path.toString().endsWith(".class")).forEach(path -> {
			try {
				byte[] bytes = Files.readAllBytes(path);
				ClassReader cr = new ClassReader(bytes);
				ClassNode modClass = new ClassNode();
				cr.accept(modClass, 0);

				if (Modifier.isInterface(modClass.access)) {
					for (MethodNode method : modClass.methods) {
						if (method.visibleAnnotations != null) {
							for (AnnotationNode anno : method.visibleAnnotations) {
								if (anno.values != null && this.hasMethodArg(anno)) {
									this.wrapAnnotation(anno);
									this.wrapModifier(method, anno);
									infos.add("Class: " + modClass.name + ", method: " + method.name + method.desc);
								}
							}
						}
					}

					ClassWriter cw = new ClassWriter(cr, 0);
					modClass.accept(cw);

					String relPath = classesFolder.toURI().relativize(path.toFile().toURI()).getPath();
					File outFile = new File(outputFolder, relPath);
					outFile.getParentFile().mkdirs();
					Files.write(outFile.toPath(), cw.toByteArray());
				}
			} catch (IOException e) {
				getLogger().error("Error while wrapping interface injectors", e);
			}
		});
		Files.write(new File(outputFolder, "iFaceMixinPatch.info").toPath(), infos);
	}

	private boolean hasMethodArg(AnnotationNode anno) {
		for (int i = 0; i < anno.values.size(); i += 2) {
			if ("method".equals(anno.values.get(i))) return true;
		}
		return false;
	}

	private void wrapAnnotation(AnnotationNode anno) {
		AnnotationNode oldCopy = new AnnotationNode(Opcodes.ASM9, anno.desc);
		oldCopy.values = new ArrayList<>(anno.values);

		anno.desc = WRAPPER;
		anno.values = new ArrayList<>();
		anno.values.add("value");
		anno.values.add(oldCopy);
	}

	private void wrapModifier(MethodNode method, AnnotationNode wrapper) {
		wrapper.values.add("accessModifier");
		wrapper.values.add(method.access);
		method.access = ACC_PUBLIC;
	}
}