package com.realciv.realciv.client;

public final class RealCivScreenScaleContext {
    private static final ThreadLocal<ScaleState> ACTIVE = new ThreadLocal<>();

    private RealCivScreenScaleContext() {
    }

    public static void activate(float scale, int offsetX, int offsetY) {
        ACTIVE.set(new ScaleState(scale, offsetX, offsetY));
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static boolean isActive() {
        return ACTIVE.get() != null;
    }

    public static int mapScissorX(int x) {
        ScaleState state = ACTIVE.get();
        if (state == null) {
            return x;
        }
        return (int) Math.floor(state.offsetX() + x * state.scale());
    }

    public static int mapScissorY(int y) {
        ScaleState state = ACTIVE.get();
        if (state == null) {
            return y;
        }
        return (int) Math.floor(state.offsetY() + y * state.scale());
    }

    public static int mapScissorW(int w) {
        ScaleState state = ACTIVE.get();
        if (state == null) {
            return w;
        }
        return Math.max(0, (int) Math.ceil(w * state.scale()));
    }

    public static int mapScissorH(int h) {
        ScaleState state = ACTIVE.get();
        if (state == null) {
            return h;
        }
        return Math.max(0, (int) Math.ceil(h * state.scale()));
    }

    private record ScaleState(float scale, int offsetX, int offsetY) {
    }
}
