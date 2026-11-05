package dev.mylauncher.mods.physics;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * A cape simulated as cloth (Verlet integration on a grid of points).
 *
 * The simulation runs in the player's model space - x = player's left, y = down, z = backwards - the same
 * space the cape is drawn in. When the player moves or turns, every point is moved by the opposite amount,
 * so the cloth "stays behind" in the world and swings naturally. The top row is pinned to the shoulders.
 */
final class ClothCape {
    private static final float MODEL_SCALE = 0.9375f; // RenderPlayer scales players by 15/16
    private static final float PX = 1f / 16f;
    private static final float BACK = 2.5f * PX; // just behind the body (body back is at z = 2px)

    final int cols, rows;
    private final float[] x, y, z, px, py, pz; // current + previous (Verlet)
    private final float[] rx, ry, rz; // snapshot from the previous tick, for smooth rendering
    private final float restX, restY;
    int lastTick = Integer.MIN_VALUE;
    long lastRendered;
    private double lastPosX, lastPosY, lastPosZ;
    private float lastYaw;
    private boolean started;
    private float shoulder; // height of the pinned row (lower while sneaking)

    ClothCape(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        int n = cols * rows;
        x = new float[n]; y = new float[n]; z = new float[n];
        px = new float[n]; py = new float[n]; pz = new float[n];
        rx = new float[n]; ry = new float[n]; rz = new float[n];
        restX = 10 * PX / (cols - 1);
        restY = 16 * PX / (rows - 1);
    }

    private int idx(int c, int r) {
        return r * cols + c;
    }

    private float anchorX(int c) {
        return 5 * PX - c * restX; // c = 0 is the player's left (+x)
    }

    private void reset(boolean sneaking) {
        float top = shoulder = sneaking ? 0.2f : 0f;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = idx(c, r);
                x[i] = px[i] = rx[i] = anchorX(c);
                y[i] = py[i] = ry[i] = top + r * restY;
                z[i] = pz[i] = rz[i] = BACK + (sneaking ? r * restY * 0.5f : 0f);
            }
        }
    }

    // ------------------------------------------------------------ coordinate frames

    /** Model-space vector -> world-space vector (relative to the player's feet), for a given body yaw. */
    private static void toWorld(float mx, float my, float mz, float yaw, float[] out) {
        double t = Math.toRadians(180f - yaw);
        float vx = -mx * MODEL_SCALE, vy = -my * MODEL_SCALE, vz = mz * MODEL_SCALE;
        out[0] = (float) (vx * Math.cos(t) + vz * Math.sin(t));
        out[1] = vy;
        out[2] = (float) (-vx * Math.sin(t) + vz * Math.cos(t));
    }

    private static void toModel(float wx, float wy, float wz, float yaw, float[] out) {
        double t = Math.toRadians(180f - yaw);
        float vx = (float) (wx * Math.cos(t) - wz * Math.sin(t));
        float vz = (float) (wx * Math.sin(t) + wz * Math.cos(t));
        out[0] = -vx / MODEL_SCALE;
        out[1] = -wy / MODEL_SCALE;
        out[2] = vz / MODEL_SCALE;
    }

    // ------------------------------------------------------------ simulation

    void tick(AbstractClientPlayer p, PhysicsMod s, int ticks) {
        boolean sneaking = p.isSneaking();
        float yaw = p.renderYawOffset;
        double dx = p.posX - lastPosX, dy = p.posY - lastPosY, dz = p.posZ - lastPosZ;
        float turn = Math.abs(net.minecraft.util.MathHelper.wrapAngleTo180_float(yaw - lastYaw));
        if (!started || dx * dx + dy * dy + dz * dz > 64 || turn > 120 || broken()) { // first frame, teleport or bad state
            reset(sneaking);
            started = true;
        } else {
            // Snapshot for rendering BEFORE moving into the new body frame: the body is drawn interpolated from the
            // old frame (partialTicks 0) to the new one (1), so the cloth must go from old-frame to new-frame points
            // too. Snapshotting after the frame change made the cape jump back every tick (choppy movement).
            System.arraycopy(x, 0, rx, 0, x.length);
            System.arraycopy(y, 0, ry, 0, y.length);
            System.arraycopy(z, 0, rz, 0, z.length);
            // Keep the cloth where it was in the world: undo the player's movement and turning.
            float sway = s.sway * s.strength;
            float vSway = sway * 0.35f; // jumping/falling moves the cloth less than walking (air drag)
            float[] w = new float[3], m = new float[3];
            for (int i = 0; i < x.length; i++) {
                toWorld(x[i], y[i], z[i], lastYaw, w);
                toModel(w[0] - (float) dx * sway, w[1] - (float) dy * vSway, w[2] - (float) dz * sway, yaw, m);
                x[i] = m[0]; y[i] = m[1]; z[i] = m[2];
                toWorld(px[i], py[i], pz[i], lastYaw, w);
                toModel(w[0] - (float) dx * sway, w[1] - (float) dy * vSway, w[2] - (float) dz * sway, yaw, m);
                px[i] = m[0]; py[i] = m[1]; pz[i] = m[2];
            }
        }
        lastPosX = p.posX;
        lastPosY = p.posY;
        lastPosZ = p.posZ;
        lastYaw = yaw;

        float gravity = 0.012f * s.gravity;
        float damping = 0.9f;
        float time = ticks;
        for (int r = 1; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = idx(c, r);
                float vx = clamp((x[i] - px[i]) * damping), vy = clamp((y[i] - py[i]) * damping), vz = clamp((z[i] - pz[i]) * damping);
                px[i] = x[i]; py[i] = y[i]; pz[i] = z[i];
                float wind = s.wind ? s.windStrength * s.strength * 0.0025f * (float) (Math.sin(time * 0.13f + r * 0.6f) + Math.sin(time * 0.05f + c)) : 0f;
                x[i] += vx;
                y[i] += vy + gravity;
                z[i] += vz + Math.max(0, wind) + 0.0004f; // tiny push back so a still cape hangs behind you
            }
        }

        shoulder += ((sneaking ? 0.2f : 0f) - shoulder) * 0.5f; // ease into and out of sneaking
        float top = shoulder;
        int iterations = 3 + s.stiffness;
        for (int it = 0; it < iterations; it++) {
            for (int c = 0; c < cols; c++) { // pin the top row to the shoulders
                int i = idx(c, 0);
                x[i] = anchorX(c);
                y[i] = top;
                z[i] = BACK;
            }
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (c + 1 < cols) constrain(idx(c, r), idx(c + 1, r), restX);
                    if (r + 1 < rows) constrain(idx(c, r), idx(c, r + 1), restY);
                    if (c + 1 < cols && r + 1 < rows) {
                        float diag = (float) Math.sqrt(restX * restX + restY * restY);
                        constrain(idx(c, r), idx(c + 1, r + 1), diag);
                        constrain(idx(c + 1, r), idx(c, r + 1), diag);
                    }
                }
            }
            if (s.collision) collide(sneaking);
            for (int i = cols; i < y.length; i++) if (y[i] < top - 0.12f) y[i] = top - 0.12f; // never above the shoulders
        }
    }

    /** Safety net: NaN or points flung far from the shoulders means the cloth must be rebuilt. */
    private boolean broken() {
        for (int i = 0; i < x.length; i++) {
            if (Float.isNaN(x[i]) || Float.isNaN(y[i]) || Float.isNaN(z[i])) return true;
            if (Math.abs(x[i]) > 3 || Math.abs(y[i]) > 3 || Math.abs(z[i]) > 3) return true;
        }
        return false;
    }

    private void constrain(int a, int b, float rest) {
        float dx = x[b] - x[a], dy = y[b] - y[a], dz = z[b] - z[a];
        float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 1e-6f) return;
        float diff = (d - rest) / d;
        boolean pinA = a < cols, pinB = b < cols; // top row is pinned
        float wa = pinA ? 0f : (pinB ? 1f : 0.5f), wb = pinB ? 0f : (pinA ? 1f : 0.5f);
        x[a] += dx * diff * wa; y[a] += dy * diff * wa; z[a] += dz * diff * wa;
        x[b] -= dx * diff * wb; y[b] -= dy * diff * wb; z[b] -= dz * diff * wb;
    }

    /** Keep the cloth behind the back and legs (a plane that tilts forward when sneaking). */
    private void collide(boolean sneaking) {
        for (int i = cols; i < x.length; i++) {
            if (y[i] > 1.55f) continue; // below the feet
            float min = BACK;
            if (sneaking) min += Math.max(0f, y[i] - 0.2f) * 0.48f;
            if (z[i] < min) z[i] = min;
        }
    }

    private static float clamp(float v) {
        return Math.max(-0.35f, Math.min(0.35f, v));
    }

    // ------------------------------------------------------------ rendering

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Draws the cloth in model space; the cape texture must already be bound. */
    void render(float pt) {
        int n = x.length;
        float[] X = new float[n], Y = new float[n], Z = new float[n];
        for (int i = 0; i < n; i++) {
            X[i] = lerp(rx[i], x[i], pt);
            Y[i] = lerp(ry[i], y[i], pt);
            Z[i] = lerp(rz[i], z[i], pt);
        }
        // Per-point normals (pointing backwards, away from the player)
        float[] NX = new float[n], NY = new float[n], NZ = new float[n];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int l = idx(Math.max(0, c - 1), r), rr = idx(Math.min(cols - 1, c + 1), r);
                int u = idx(c, Math.max(0, r - 1)), d = idx(c, Math.min(rows - 1, r + 1));
                float ax = X[l] - X[rr], ay = Y[l] - Y[rr], az = Z[l] - Z[rr]; // across (toward +x)
                float bx = X[d] - X[u], by = Y[d] - Y[u], bz = Z[d] - Z[u]; // down
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len < 1e-6f) { nx = 0; ny = 0; nz = 1; len = 1; }
                nx /= len; ny /= len; nz /= len;
                if (nz < 0) { nx = -nx; ny = -ny; nz = -nz; }
                int i = idx(c, r);
                NX[i] = nx; NY[i] = ny; NZ[i] = nz;
            }
        }
        float half = 0.5f * PX;
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
        for (int r = 0; r + 1 < rows; r++) {
            for (int c = 0; c + 1 < cols; c++) {
                int[] q = {idx(c, r), idx(c, r + 1), idx(c + 1, r + 1), idx(c + 1, r)};
                // outer face (seen from behind): texture x 1..11, y 1..17 of a 64x32 cape
                for (int i : q) {
                    int cc = i % cols, rr = i / cols;
                    float u = (1f + 10f * cc / (cols - 1)) / 64f, v = (1f + 16f * rr / (rows - 1)) / 32f;
                    w.pos(X[i] + NX[i] * half, Y[i] + NY[i] * half, Z[i] + NZ[i] * half).tex(u, v).normal(NX[i], NY[i], NZ[i]).endVertex();
                }
                // inner face (seen from the front): texture x 12..22
                for (int k = 3; k >= 0; k--) {
                    int i = q[k];
                    int cc = i % cols, rr = i / cols;
                    float u = (22f - 10f * cc / (cols - 1)) / 64f, v = (1f + 16f * rr / (rows - 1)) / 32f;
                    w.pos(X[i] - NX[i] * half, Y[i] - NY[i] * half, Z[i] - NZ[i] * half).tex(u, v).normal(-NX[i], -NY[i], -NZ[i]).endVertex();
                }
            }
        }
        // Edges (left, right, bottom) so the cape has thickness, using the cape's edge pixels
        for (int r = 0; r + 1 < rows; r++) {
            edge(w, X, Y, Z, NX, NY, NZ, idx(0, r), idx(0, r + 1), half, 0.5f / 64f, r);
            edge(w, X, Y, Z, NX, NY, NZ, idx(cols - 1, r), idx(cols - 1, r + 1), half, 11.5f / 64f, r);
        }
        for (int c = 0; c + 1 < cols; c++) {
            edge(w, X, Y, Z, NX, NY, NZ, idx(c, rows - 1), idx(c + 1, rows - 1), half, (11.5f + c) / 64f, rows - 1);
        }
        t.draw();
    }

    private void edge(WorldRenderer w, float[] X, float[] Y, float[] Z, float[] NX, float[] NY, float[] NZ,
                      int a, int b, float half, float u, int row) {
        float v = (1f + 16f * row / (rows - 1)) / 32f;
        float ex = X[b] - X[a], ez = Z[b] - Z[a];
        float nx = ez, nz = -ex; // sideways normal (approximate)
        w.pos(X[a] + NX[a] * half, Y[a] + NY[a] * half, Z[a] + NZ[a] * half).tex(u, v).normal(nx, 0, nz).endVertex();
        w.pos(X[b] + NX[b] * half, Y[b] + NY[b] * half, Z[b] + NZ[b] * half).tex(u, v).normal(nx, 0, nz).endVertex();
        w.pos(X[b] - NX[b] * half, Y[b] - NY[b] * half, Z[b] - NZ[b] * half).tex(u, v).normal(nx, 0, nz).endVertex();
        w.pos(X[a] - NX[a] * half, Y[a] - NY[a] * half, Z[a] - NZ[a] * half).tex(u, v).normal(nx, 0, nz).endVertex();
    }
}
