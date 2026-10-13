package io.github.b1tr0n1n.signal.client;

import com.mojang.logging.LogUtils;
import io.github.b1tr0n1n.signal.SignalMod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

/**
 * Development playtest pilot: lets a local script play the client the way a person would, without chat
 * commands or synthetic OS input. Off unless the environment variable {@code SIGNAL_PILOT_PORT} is set; then it
 * listens on that port on the loopback interface only. One text command per line, one JSON line back.
 *
 * <p>Movement holds the real key mappings each tick and steers toward a target; the camera eases toward a
 * target direction every frame through {@link Entity#turn}, the same path the mouse takes, so it renders
 * smoothly. Screens are driven by pressing their buttons by label, containers by quick-moving a slot.
 */
@EventBusSubscriber(modid = SignalMod.MOD_ID, value = Dist.CLIENT)
public final class PlaytestPilot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PORT_ENV = "SIGNAL_PILOT_PORT";
    private static final Integer PORT = parsePort(System.getenv(PORT_ENV));
    private static final double ENTITY_RADIUS = 48;

    private static boolean started;
    // Walk target (all state is touched on the client thread only).
    private static Vec3 walkTo;
    private static double walkTolerance = 1.2;
    private static boolean sprint;
    private static Vec3 stuckCheck;
    private static int stuckTicks;
    private static int strafeTicks;
    private static boolean strafeLeft;
    // Look target, in degrees; null when the pilot leaves the camera alone.
    private static Float lookYaw;
    private static Float lookPitch;
    private static double turnSpeed = 300; // degrees per second
    private static String aimType;
    private static long lastFrameNanos;
    private static int useTicks;
    // Planned route for goto; null for a straight walk.
    private static List<Vec3> path;
    private static int pathIndex;
    private static double[] goal;
    private static int replans;
    private static String lastResult = "";
    private static BlockHitResult pendingPlace;
    private static BlockPos pendingBreak;
    private static Double altitude;
    private static int pendingTicks;

    private PlaytestPilot() {
    }

    private static Integer parsePort(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int port = Integer.parseInt(value.trim());
            return port > 1023 && port < 65536 ? port : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ socket

    private static void start() {
        started = true;
        Thread thread = new Thread(PlaytestPilot::serve, "signal-playtest-pilot");
        thread.setDaemon(true);
        thread.start();
    }

    private static void serve() {
        try (ServerSocket server = new ServerSocket(PORT, 4, InetAddress.getLoopbackAddress())) {
            LOGGER.info("Playtest pilot listening on 127.0.0.1:{}", PORT);
            while (true) {
                Socket socket = server.accept();
                Thread conn = new Thread(() -> converse(socket), "signal-playtest-pilot-conn");
                conn.setDaemon(true);
                conn.start();
            }
        } catch (IOException e) {
            LOGGER.error("Playtest pilot could not listen on {}", PORT, e);
        }
    }

    /** One client connection; commands still run one at a time on the client thread. */
    private static void converse(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                out.println(dispatch(line.trim()));
            }
        } catch (IOException e) {
            LOGGER.debug("Playtest pilot connection closed: {}", e.getMessage());
        }
    }

    private static String dispatch(String line) {
        if (line.isEmpty()) {
            return err("empty command");
        }
        try {
            return Minecraft.getInstance().submit(() -> handle(line)).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return err(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ commands

    private static String handle(String line) {
        Minecraft mc = Minecraft.getInstance();
        String[] a = line.split("\\s+");
        String op = a[0].toLowerCase(Locale.ROOT);
        LocalPlayer p = mc.player;
        if (p == null && !op.equals("state")) {
            return err("no player");
        }
        switch (op) {
            case "state":
                return state(mc);
            case "walk": // walk X Z [sprint] [tolerance]: straight line, no planning
                path = null;
                walkTo = new Vec3(Double.parseDouble(a[1]), p.getY(), Double.parseDouble(a[2]));
                sprint = a.length > 3 && a[3].equals("sprint");
                walkTolerance = a.length > 4 ? Double.parseDouble(a[4]) : 1.2;
                stuckCheck = p.position();
                stuckTicks = 0;
                return ok();
            case "goto": { // goto X Z [sprint]: pathfind, then follow the route
                lastResult = "";
                double tx = Double.parseDouble(a[1]);
                double tz = Double.parseDouble(a[2]);
                List<Vec3> route = PilotPath.find(mc.level, p.position(), tx, tz);
                if (route.isEmpty()) {
                    return err("no route to " + a[1] + " " + a[2]);
                }
                path = new ArrayList<>(route);
                pathIndex = 0;
                goal = new double[]{tx, tz};
                replans = 0;
                sprint = a.length > 3 && a[3].equals("sprint");
                walkTo = path.get(0);
                walkTolerance = 0.45;
                stuckCheck = p.position();
                stuckTicks = 0;
                return "{\"ok\":true,\"waypoints\":" + route.size() + "}";
            }
            case "place": { // place X Y Z: turn onto a neighbouring face, then right-click it with the held item
                BlockPos target = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
                if (!mc.level.getBlockState(target).canBeReplaced()) {
                    return err("occupied " + target.toShortString());
                }
                BlockHitResult hit = a.length > 4 ? supportFrom(mc, target, Direction.byName(a[4])) : support(mc, target);
                if (hit == null) {
                    return err("nothing to place against at " + target.toShortString());
                }
                if (p.getEyePosition().distanceTo(hit.getLocation()) > p.blockInteractionRange()) {
                    return err("too far from " + target.toShortString());
                }
                aimType = null;
                aimAt(p, hit.getLocation());
                pendingPlace = hit;
                pendingTicks = 0;
                lastResult = "placing";
                return ok();
            }
            case "hold": { // hold ITEM: select it from the hotbar, or (creative) pick it into the selected slot
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(a[1]));
                for (int i = 0; i < 9; i++) {
                    if (p.getInventory().getItem(i).is(item)) {
                        p.getInventory().selected = i;
                        return ok();
                    }
                }
                if (!p.getAbilities().instabuild || mc.gameMode == null) {
                    return err("not in the hotbar and not in creative: " + a[1]);
                }
                ItemStack stack = new ItemStack(item, 64);
                p.getInventory().setItem(p.getInventory().selected, stack);
                mc.gameMode.handleCreativeModeItemAdd(stack, 36 + p.getInventory().selected);
                return ok();
            }
            case "break": { // break X Y Z: turn onto the block and hit it (instant in creative)
                BlockPos target = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
                if (mc.level.getBlockState(target).isAir()) {
                    return ok();
                }
                if (p.getEyePosition().distanceTo(Vec3.atCenterOf(target)) > p.blockInteractionRange() + 0.5) {
                    return err("too far from " + target.toShortString());
                }
                aimType = null;
                aimAt(p, Vec3.atCenterOf(target));
                pendingBreak = target;
                pendingTicks = 0;
                lastResult = "breaking";
                return ok();
            }
            case "fly": // fly on|off (creative)
                if (!p.getAbilities().mayfly) {
                    return err("cannot fly in this game mode");
                }
                p.getAbilities().flying = a[1].equals("on");
                p.onUpdateAbilities();
                if (!p.getAbilities().flying) {
                    altitude = null;
                }
                return ok();
            case "alt": // alt Y: while flying, rise or sink to Y
                altitude = Double.parseDouble(a[1]);
                return ok();
            case "heights": { // heights X0 Z0 W D: ground height (top non-leaf surface) per column, rows by z
                int x0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]), w = Integer.parseInt(a[3]), d = Integer.parseInt(a[4]);
                List<String> rows = new ArrayList<>();
                for (int dz = 0; dz < d; dz++) {
                    List<String> row = new ArrayList<>();
                    for (int dx = 0; dx < w; dx++) {
                        row.add(Integer.toString(ground(mc, x0 + dx, z0 + dz)));
                    }
                    rows.add("[" + String.join(",", row) + "]");
                }
                return "{\"ok\":true,\"heights\":[" + String.join(",", rows) + "]}";
            }
            case "block": // block X Y Z: what is there
                return "{\"ok\":true,\"block\":" + q(mc.level.getBlockState(new BlockPos(Integer.parseInt(a[1]),
                        Integer.parseInt(a[2]), Integer.parseInt(a[3]))).toString()) + "}";
            case "probe":
                return "{\"ok\":true,\"probe\":" + q(PilotPath.probe(mc.level, p.position())) + "}";
            case "stop":
                stopAll(mc);
                return ok();
            case "look": // look YAW PITCH [speed]
                aimType = null;
                lookYaw = Float.parseFloat(a[1]);
                lookPitch = Mth.clamp(Float.parseFloat(a[2]), -90, 90);
                if (a.length > 3) {
                    turnSpeed = Double.parseDouble(a[3]);
                }
                return ok();
            case "turn": // turn DYAW [DPITCH]: relative
                aimType = null;
                lookYaw = p.getYRot() + Float.parseFloat(a[1]);
                lookPitch = Mth.clamp(p.getXRot() + (a.length > 2 ? Float.parseFloat(a[2]) : 0), -90, 90);
                return ok();
            case "lookat": // lookat X Y Z
                aimType = null;
                aimAt(p, new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
                return ok();
            case "aim": // aim TYPE|off: keep facing the nearest entity of that type
                aimType = a[1].equals("off") ? null : a[1];
                if (aimType == null) {
                    lookYaw = null;
                    lookPitch = null;
                }
                return ok();
            case "key": // key forward|back|left|right|jump|sneak|sprint down|up|tap
                KeyMapping key = key(mc, a[1]);
                if (key == null) {
                    return err("unknown key " + a[1]);
                }
                if (a[2].equals("tap")) {
                    KeyMapping.click(key.getKey());
                } else {
                    key.setDown(a[2].equals("down"));
                }
                return ok();
            case "use": // use [ticks]: click, or hold for ticks
                int ticks = a.length > 1 ? Integer.parseInt(a[1]) : 0;
                if (ticks <= 0) {
                    KeyMapping.click(mc.options.keyUse.getKey());
                } else {
                    useTicks = ticks;
                }
                return ok();
            case "attack":
                KeyMapping.click(mc.options.keyAttack.getKey());
                return ok();
            case "slot": // slot 1..9
                p.getInventory().selected = Mth.clamp(Integer.parseInt(a[1]) - 1, 0, 8);
                return ok();
            case "cmd": // cmd TEXT: a slash command without opening chat
                p.connection.sendCommand(line.substring(4).trim().replaceFirst("^/", ""));
                return ok();
            case "say":
                p.connection.sendChat(line.substring(4).trim());
                return ok();
            case "screen":
                return screen(mc);
            case "press": // press LABEL: press the first enabled button whose label contains LABEL
                return press(mc, line.substring(6).trim().toLowerCase(Locale.ROOT));
            case "close":
                if (mc.screen != null) {
                    mc.screen.onClose();
                }
                return ok();
            case "take": // take SLOT: quick-move a slot of the open container into the inventory
                if (mc.screen == null || mc.gameMode == null) {
                    return err("no container open");
                }
                mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, Integer.parseInt(a[1]), 0, ClickType.QUICK_MOVE, p);
                return ok();
            case "respawn":
                p.respawn();
                if (mc.screen != null) {
                    mc.setScreen(null);
                }
                return ok();
            default:
                return err("unknown command " + op);
        }
    }

    private static KeyMapping key(Minecraft mc, String name) {
        return switch (name) {
            case "forward" -> mc.options.keyUp;
            case "back" -> mc.options.keyDown;
            case "left" -> mc.options.keyLeft;
            case "right" -> mc.options.keyRight;
            case "jump" -> mc.options.keyJump;
            case "sneak" -> mc.options.keyShift;
            case "sprint" -> mc.options.keySprint;
            case "reload" -> ClientSetup.RELOAD;
            default -> null;
        };
    }

    private static void stopAll(Minecraft mc) {
        walkTo = null;
        path = null;
        aimType = null;
        lookYaw = null;
        lookPitch = null;
        useTicks = 0;
        for (KeyMapping k : List.of(mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keySprint, mc.options.keyShift, mc.options.keyUse)) {
            k.setDown(false);
        }
    }

    private static void aimAt(LocalPlayer p, Vec3 target) {
        Vec3 d = target.subtract(p.getEyePosition());
        lookYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        lookPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
    }

    /**
     * Top ground block of a column, looking through trees and plants. Clients only receive the MOTION_BLOCKING
     * heightmap, so start there and walk down past leaves, logs and anything replaceable.
     */
    private static int ground(Minecraft mc, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        while (p.getY() > mc.level.getMinBuildHeight()) {
            var s = mc.level.getBlockState(p);
            if (!(s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS))) {
                break;
            }
            p.move(Direction.DOWN);
        }
        return p.getY();
    }

    /** A face of a solid neighbour to place {@code target} against: below first, then the sides, then above. */
    private static BlockHitResult support(Minecraft mc, BlockPos target) {
        List<Direction> order = List.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP);
        for (Direction d : order) {
            BlockHitResult hit = supportFrom(mc, target, d);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /** Place {@code target} against its neighbour in direction {@code d} (e.g. UP hangs a lantern), if solid. */
    private static BlockHitResult supportFrom(Minecraft mc, BlockPos target, Direction d) {
        if (d == null) {
            return null;
        }
        BlockPos n = target.relative(d);
        var state = mc.level.getBlockState(n);
        if (state.isAir() || state.canBeReplaced()) {
            return null;
        }
        Direction face = d.getOpposite();
        // Side hits low on the face so slabs and stairs come out bottom-half, like a player aiming low.
        Vec3 at = Vec3.atCenterOf(target).relative(d, 0.5).add(0, face.getAxis().isHorizontal() ? -0.25 : 0, 0);
        return new BlockHitResult(at, face, n, false);
    }

    private static String press(Minecraft mc, String label) {
        if (mc.screen == null) {
            return err("no screen");
        }
        for (var child : mc.screen.children()) {
            if (child instanceof Button b && b.active && b.visible
                    && b.getMessage().getString().toLowerCase(Locale.ROOT).contains(label)) {
                b.onPress();
                return ok();
            }
        }
        return err("no enabled button matching '" + label + "'");
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        if (PORT == null) {
            return;
        }
        if (!started) {
            start();
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        if (aimType != null) {
            nearest(mc, aimType).ifPresent(e -> aimAt(p, e.getEyePosition()));
        }
        if (useTicks > 0) {
            mc.options.keyUse.setDown(--useTicks > 0);
        }
        if (altitude != null && p.getAbilities().flying) {
            double dy = altitude - p.getY();
            mc.options.keyJump.setDown(dy > 0.25);
            mc.options.keyShift.setDown(dy < -0.25);
            if (Math.abs(dy) <= 0.25) {
                altitude = null;
                mc.options.keyJump.setDown(false);
                mc.options.keyShift.setDown(false);
            }
        }
        if (pendingBreak != null) {
            float yawErr = Math.abs(Mth.wrapDegrees(lookYaw - p.getYRot()));
            float pitchErr = Math.abs(lookPitch - p.getXRot());
            if (yawErr < 3 && pitchErr < 3 || ++pendingTicks > 30) {
                mc.gameMode.startDestroyBlock(pendingBreak, Direction.UP);
                p.swing(InteractionHand.MAIN_HAND);
                lastResult = "broke";
                pendingBreak = null;
            }
        }
        if (pendingPlace != null) {
            float yawErr = Math.abs(Mth.wrapDegrees(lookYaw - p.getYRot()));
            float pitchErr = Math.abs(lookPitch - p.getXRot());
            if (yawErr < 3 && pitchErr < 3 || ++pendingTicks > 30) {
                var result = mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, pendingPlace);
                if (result.consumesAction()) {
                    p.swing(InteractionHand.MAIN_HAND);
                }
                lastResult = result.consumesAction() ? "placed" : "place failed: " + result;
                pendingPlace = null;
            }
        }
        if (walkTo != null) {
            steer(mc, p);
        }
    }

    private static void releaseMovement(Minecraft mc) {
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
    }

    private static void steer(Minecraft mc, LocalPlayer p) {
        double dx = walkTo.x - p.getX();
        double dz = walkTo.z - p.getZ();
        double reach = path != null && sprint ? 0.8 : walkTolerance;
        if (Math.hypot(dx, dz) < reach) {
            if (path != null && ++pathIndex < path.size()) {
                walkTo = path.get(pathIndex);
                return;
            }
            lastResult = "arrived";
            walkTo = null;
            path = null;
            releaseMovement(mc);
            return;
        }
        // On a route, jump where the next waypoint is a step up and we are close to it.
        boolean stepUp = path != null && walkTo.y - p.getY() > 0.6 && Math.hypot(dx, dz) < 1.8;
        float desired = (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (aimType == null) {
            lookYaw = desired;
            if (lookPitch == null || Math.abs(lookPitch) > 30) {
                lookPitch = 5F;
            }
        }
        float error = Math.abs(Mth.wrapDegrees(desired - p.getYRot()));
        mc.options.keyUp.setDown(error < 70);
        mc.options.keySprint.setDown(sprint && error < 25);
        // Off a route, bumping into something is the only cue to jump; on a route only planned steps are.
        mc.options.keyJump.setDown(stepUp && p.onGround() || path == null && p.horizontalCollision && p.onGround() || p.isInWater());
        if (path != null) {
            // On a route, being stuck for two seconds means the plan was wrong: plan again from here.
            if (++stuckTicks >= 40) {
                if (p.position().distanceTo(stuckCheck) < 0.5) {
                    List<Vec3> route = replans++ < 3 ? PilotPath.find(mc.level, p.position(), goal[0], goal[1]) : List.of();
                    if (route.isEmpty()) {
                        lastResult = "stuck at " + p.blockPosition().toShortString();
                        walkTo = null;
                        path = null;
                        releaseMovement(mc);
                        return;
                    }
                    path = new ArrayList<>(route);
                    pathIndex = 0;
                    walkTo = path.get(0);
                }
                stuckCheck = p.position();
                stuckTicks = 0;
            }
            return;
        }
        // Unstick: if pressing forward but barely moving for a second, strafe for a moment.
        if (strafeTicks > 0) {
            strafeTicks--;
            mc.options.keyLeft.setDown(strafeLeft && strafeTicks > 0);
            mc.options.keyRight.setDown(!strafeLeft && strafeTicks > 0);
        } else if (++stuckTicks >= 20) {
            if (p.position().distanceTo(stuckCheck) < 0.4 && error < 70) {
                strafeTicks = 14;
                strafeLeft = !strafeLeft;
            }
            stuckCheck = p.position();
            stuckTicks = 0;
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre event) {
        long now = System.nanoTime();
        double dt = lastFrameNanos == 0 ? 0 : Math.min((now - lastFrameNanos) / 1e9, 0.1);
        lastFrameNanos = now;
        LocalPlayer p = Minecraft.getInstance().player;
        if (PORT == null || p == null || lookYaw == null || Minecraft.getInstance().screen != null) {
            return;
        }
        double step = turnSpeed * dt;
        double dy = Mth.clamp(Mth.wrapDegrees(lookYaw - p.getYRot()), -step, step);
        double dp = Mth.clamp(lookPitch - p.getXRot(), -step, step);
        if (Math.abs(dy) > 1e-3 || Math.abs(dp) > 1e-3) {
            p.turn(dy / 0.15, dp / 0.15); // Entity.turn scales by 0.15, like mouse input
        }
    }

    // ------------------------------------------------------------------ reporting

    private static java.util.Optional<Entity> nearest(Minecraft mc, String type) {
        LocalPlayer p = mc.player;
        Entity best = null;
        double bestD = ENTITY_RADIUS * ENTITY_RADIUS;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e != p && e.isAlive() && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(type)) {
                double d = e.distanceToSqr(p);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
        }
        return java.util.Optional.ofNullable(best);
    }

    private static String state(Minecraft mc) {
        LocalPlayer p = mc.player;
        StringBuilder s = new StringBuilder("{\"ok\":true");
        s.append(",\"screen\":").append(q(mc.screen == null ? "" : mc.screen.getClass().getSimpleName()));
        if (p == null) {
            return s.append('}').toString();
        }
        s.append(String.format(Locale.ROOT, ",\"pos\":[%.2f,%.2f,%.2f],\"yaw\":%.1f,\"pitch\":%.1f", p.getX(), p.getY(), p.getZ(),
                Mth.wrapDegrees(p.getYRot()), p.getXRot()));
        s.append(String.format(Locale.ROOT, ",\"hp\":%.1f,\"food\":%d,\"dead\":%b,\"ground\":%b,\"time\":%d",
                p.getHealth(), p.getFoodData().getFoodLevel(), p.isDeadOrDying(), p.onGround(), mc.level.getDayTime() % 24000));
        s.append(",\"walking\":").append(walkTo != null).append(",\"result\":").append(q(lastResult));
        if (path != null) {
            s.append(",\"waypoint\":").append(pathIndex).append(",\"of\":").append(path.size());
        }
        s.append(",\"held\":").append(item(p.getMainHandItem()));
        s.append(",\"slot\":").append(p.getInventory().selected + 1);
        List<String> inv = new ArrayList<>();
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            ItemStack it = p.getInventory().items.get(i);
            if (!it.isEmpty()) {
                inv.add("[" + i + "," + item(it) + "]");
            }
        }
        s.append(",\"inv\":[").append(String.join(",", inv)).append(']');
        List<Entity> near = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e != p && e.distanceTo(p) < ENTITY_RADIUS && (e instanceof LivingEntity || e instanceof net.minecraft.world.entity.item.ItemEntity)) {
                near.add(e);
            }
        }
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        List<String> ents = new ArrayList<>();
        for (Entity e : near.subList(0, Math.min(12, near.size()))) {
            String hp = e instanceof LivingEntity l ? String.format(Locale.ROOT, "%.1f", l.getHealth()) : "null";
            ents.add(String.format(Locale.ROOT, "{\"type\":%s,\"id\":%d,\"pos\":[%.1f,%.1f,%.1f],\"dist\":%.1f,\"hp\":%s}",
                    q(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()), e.getId(), e.getX(), e.getY(), e.getZ(), e.distanceTo(p), hp));
        }
        s.append(",\"near\":[").append(String.join(",", ents)).append(']');
        return s.append('}').toString();
    }

    private static String screen(Minecraft mc) {
        if (mc.screen == null) {
            return "{\"ok\":true,\"screen\":\"\"}";
        }
        List<String> widgets = new ArrayList<>();
        for (var child : mc.screen.children()) {
            if (child instanceof AbstractWidget w) {
                widgets.add(q((w.active ? "" : "(off) ") + w.getMessage().getString()));
            }
        }
        return "{\"ok\":true,\"screen\":" + q(mc.screen.getClass().getSimpleName()) + ",\"title\":" + q(mc.screen.getTitle().getString())
                + ",\"widgets\":[" + String.join(",", widgets) + "]}";
    }

    private static String item(ItemStack it) {
        return it.isEmpty() ? "null" : "{\"id\":" + q(BuiltInRegistries.ITEM.getKey(it.getItem()).toString()) + ",\"n\":" + it.getCount()
                + ",\"c\":" + q(it.getComponentsPatch().toString()) + "}";
    }

    private static String ok() {
        return "{\"ok\":true}";
    }

    private static String err(String message) {
        return "{\"ok\":false,\"error\":" + q(message) + "}";
    }

    private static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> b.append(c < 0x20 ? ' ' : c);
            }
        }
        return b.append('"').toString();
    }
}
