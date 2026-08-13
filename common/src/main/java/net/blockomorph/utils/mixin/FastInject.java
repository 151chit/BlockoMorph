package net.blockomorph.utils.mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* HINT:
	void - (boolean: true - continue; false - cancel)
	object - (object: some - cancel; CONTINUE_EXECUTION - continue)
	boolean - (byte: 1 - true; 0 - continue; -1 - false)
	primitive - (primitive: some - cancel; from @PrimitiveCancelSignal - continue)
*/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FastInject {

	String[] method() default {};
	Desc[] target() default {};
	Slice[] slice() default {};
	At[] at();
	boolean remap() default true;
	int require() default -1;
	int expect() default 1;
	int allow() default -1;

	PrimitiveCancelSignal continueIf() default @PrimitiveCancelSignal;
	Object CONTINUE_EXECUTION = new Object();
}