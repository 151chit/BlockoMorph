package net.blockomorph.interfaceInjectionSupport;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectorWrapper {
	//Annotation value(); - put with asm
}