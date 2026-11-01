package com.sakhcheese.modules.resizer.props;

public class Size {
	public static float x = 1.0f;
	public static float y = 1.0f;
	public static float z = 1.0f;

    public static boolean isDefault() {
        return x == 1.0F && y == 1.0F && z == 1.0F;
    }

    public static void set(float newX, float newY, float newZ) {
        x = clamp(newX);
        y = clamp(newY);
        z = clamp(newZ);
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.2F, Math.min(12.0F, value)) : 1.0F;
    }

	
	public static void reset(){
		x = 1.0f;
		y = 1.0f;
		z = 1.0f;
	}
}
