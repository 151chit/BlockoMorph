package net.blockomorph.utils;

public class SideSelector {
	private static final ThreadLocal<Boolean> IS_CLIENT = new InheritableThreadLocal<>();
	public static void markClient() {
		IS_CLIENT.set(true);
	}

	public static void markServer() {
		IS_CLIENT.set(false);
	}

	public static boolean isClient() {
		return IS_CLIENT.get();
	}
}

