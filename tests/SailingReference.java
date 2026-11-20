package sail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.StringJoiner;
import sun.misc.Unsafe;

public final class SailingReference {
    static final class OutputStub extends OutPanel {
        int sweeps;
        double residual;
        @Override public void showanswer(String text) {
            sweeps++;
            residual = Double.parseDouble(text.substring("residual = ".length()));
        }
    }

    static final class GraphStub extends GraphPad {
        StringJoiner route;
        StringJoiner times = new StringJoiner(",", "[", "]");
        @Override public void setType(String type) {}
        @Override public void clear() { route = new StringJoiner(",", "[", "]"); }
        @Override public void repaint() {}
        @Override public void simgraphics(int x, int y, int tack, int wind, int dx, int dy, int wx, int wy) {
            route.add("[" + x + "," + y + "," + tack + "," + wind + "," + dx + "," + dy + "]");
        }
        @Override public void histographics(double time) { times.add(Double.toString(time)); }
    }

    private static final double[][] TRIPLE = {
        {0.3,0.4,0.3}, {0.4,0.3,0.3}, {0.4,0.3,0.3}, {0.4,0.3,0.3},
        {0.4,0.2,0.4}, {0.3,0.3,0.4}, {0.3,0.3,0.4}, {0.3,0.3,0.4},
    };

    public static void main(String[] args) throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe allocator = (Unsafe) field.get(null);
        SailFrame sail = (SailFrame) allocator.allocateInstance(SailFrame.class);
        OutputStub output = (OutputStub) allocator.allocateInstance(OutputStub.class);
        GraphStub graph = new GraphStub();
        sail.op = output;
        sail.gp = graph;
        sail.r = new Random(Integer.parseInt(args[0]));
        sail.n = Integer.parseInt(args[1]);
        sail.x_a = Integer.parseInt(args[2]); sail.y_a = Integer.parseInt(args[3]);
        sail.x_b = Integer.parseInt(args[4]); sail.y_b = Integer.parseInt(args[5]);
        sail.into_wind_time = 100000;
        sail.up_wind_time = 4;
        sail.cross_wind_time = 3;
        sail.down_wind_time = 2;
        sail.away_wind_time = 1;
        sail.delay = Double.parseDouble(args[6]);
        sail.p = new double[8][8];
        boolean calm = Boolean.parseBoolean(args[7]);
        for (int i = 0; i < 8; i++) {
            for (int j = -1; j <= 1; j++) {
                sail.p[i][(i+j+8)%8] = calm ? (j == 0 ? 1.0 : 0.0) : TRIPLE[i][j+1];
            }
        }
        sail.v = new double[sail.n+1][sail.n+1][3][8];
        sail.bestheading = new int[sail.n+1][sail.n+1][3][8];
        sail.dynamic_prog(sail.v, sail.bestheading);
        sail.Simulate();
        String first = graph.route.toString();
        for (int i = 0; i < 100; i++) sail.Simulate();
        System.out.println("{\"values\":" + Arrays.deepToString(sail.v)
            + ",\"headings\":" + Arrays.deepToString(sail.bestheading)
            + ",\"sweeps\":" + output.sweeps + ",\"residual\":" + output.residual
            + ",\"first\":" + first + ",\"last\":" + graph.route
            + ",\"times\":" + graph.times + "}");
    }
}
