package net.blockomorph.utils.mixin;

public @interface PrimitiveCancelSignal {
	int intValue() default 0;
	float floatValue() default 0.0F;
	long longValue() default 0L;
	double doubleValue() default 0.0;
	short shortValue() default 0;
	byte byteValue() default 0;
	char charValue() default 0;
}