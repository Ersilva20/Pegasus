package dev.mylauncher.core.util;

import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.mixin.ShaderGroupAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minecraft can only run one post-processing shader at a time. Mods request one with a priority;
 * the highest active request wins (e.g. Menu Blur over Motion Blur over Color Saturation).
 */
public final class PostShaders {
    public interface Uniforms {
        void apply(ShaderGroup group);
    }

    private static final class Request {
        final int priority;
        final ResourceLocation shader;
        final Uniforms uniforms;

        Request(int priority, ResourceLocation shader, Uniforms uniforms) {
            this.priority = priority;
            this.shader = shader;
            this.uniforms = uniforms;
        }
    }

    private static final Map<Object, Request> REQUESTS = new LinkedHashMap<Object, Request>();
    private static ResourceLocation active;

    public static void request(Object owner, int priority, ResourceLocation shader, Uniforms uniforms) {
        REQUESTS.put(owner, new Request(priority, shader, uniforms));
    }

    public static void release(Object owner) {
        REQUESTS.remove(owner);
    }

    public static void setUniform(ShaderGroup group, String name, float... values) {
        for (Shader s : ((ShaderGroupAccessor) group).ml$getShaders()) {
            ShaderUniform u = s.getShaderManager().getShaderUniform(name);
            if (u == null) continue;
            if (values.length == 1) u.set(values[0]);
            else if (values.length == 2) u.set(values[0], values[1]);
            else if (values.length == 3) u.set(values[0], values[1], values[2]);
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer == null || !OpenGlHelper.shadersSupported) return;
        Request best = null;
        for (Request r : REQUESTS.values()) if (best == null || r.priority > best.priority) best = r;
        ShaderGroup current = mc.entityRenderer.getShaderGroup();
        try {
            if (best == null) {
                if (active != null) {
                    if (current != null && current.getShaderGroupName().equals(active.toString())) mc.entityRenderer.stopUseShader();
                    active = null;
                }
                return;
            }
            if (current == null || !current.getShaderGroupName().equals(best.shader.toString())) {
                mc.entityRenderer.loadShader(best.shader);
                current = mc.entityRenderer.getShaderGroup();
            }
            active = best.shader;
            if (current != null && best.uniforms != null) best.uniforms.apply(current);
        } catch (Exception e) {
            MLCore.LOG.error("Post shader failed", e);
        }
    }
}
