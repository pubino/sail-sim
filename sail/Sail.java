package sail;


import java.awt.*;
import java.util.*;
import javax.swing.*;

import myutil.*;

public class Sail
{
	public static void main(String[] args) {
		SailFrame sf = new SailFrame();
		sf.loadImages();
		sf.setVisible(true);
	}
}

class SailFrame extends JFrame
{
	MenuPanel mp = new MenuPanel();
	InPanel   ip = new InPanel();
	OutPanel  op = new OutPanel();
	GraphPad  gp = new GraphPad();

	public SailFrame()
	{
		Panel p1 = new Panel();
		p1.setLayout(new FlowLayout(FlowLayout.LEFT));
		p1.setFont(new Font("Helvetica",Font.BOLD,10));
		p1.add(new Button("Calculate"));
		p1.add(new Button("Simulate"));
		p1.add(new Button("Simulate 100"));
		p1.add(new Button("View Stats"));
		p1.add(new Button("Exit"));

		Panel p2 = new Panel();
		p2.setLayout(new FlowLayout(FlowLayout.RIGHT));
		p2.add(mp);

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;

		RaisedPanel p = new RaisedPanel();
		// p.setLayout(new GridLayout(1,2));
		p.setLayout(gbl);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbl.setConstraints(p1,gbc);
		p.add(p1);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbl.setConstraints(p2,gbc);
		p.add(p2);

		Panel q = new Panel();
		q.setLayout(new BorderLayout(0,0));

		q.add("North",p);
		q.add("Center",ip);
		q.add("South",op);

		setTitle("Sailing Simulator - Optimal Sailing Strategies");
		setLayout(new BorderLayout(10,10));
		add("West", q);
		add("East", gp);

		/*
		setSize(1010,530);
		*/
		setSize(1010,560);
		gp.setFont(new Font("Courier",Font.BOLD,10));
	}

	public void loadImages() {
		Image ourimage = loadImage("sail/princeton_shield_small.gif");
		Image beach    = loadImage("sail/beach.gif");
		Image sailboat = loadImage("sail/sailboat_right.gif");
		mp.getPicture(ourimage);
		gp.getPicture(beach, sailboat);
	}

	private Image loadImage(String path) {
		java.net.URL url = getClass().getClassLoader().getResource(path);
		if (url != null) {
			return Toolkit.getDefaultToolkit().getImage(url);
		}
		return null;
	}

	public boolean action(Event evt, Object arg)
	{	if (arg.equals("Exit")) {
			System.exit(0);
		}
		else if (arg.equals("Calculate")) {
			Calculate();
		}
		else if (arg.equals("Simulate")) {
			Simulate();
		}
		else if (arg.equals("Simulate 100")) {
		    for (int i=0; i<100; i++) {
			Simulate();
		    }
		}
		else if (arg.equals("View Stats")) {
			Histogram();
		}
		else return super.action(evt,arg);
		return true;
	}

	public boolean handleEvent(Event evt)
	{	if (evt.id == Event.WINDOW_DESTROY) System.exit(0);
		return super.handleEvent(evt);
	}

	Random r = new Random();
	double p[][] = new double[8][8];
	int n=10;
	int x_a=2;
	int y_a=1;
	int x_b=7;
	int y_b=7;

	double  into_wind_time=1000000;
	double    up_wind_time=3;
	double cross_wind_time=2;
	double  down_wind_time=1;
	double  away_wind_time=2;
	double           delay=3;

	double v[][][][];
	int bestheading[][][][];

	public void Calculate()
	{
		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++) {
			    p[i][j] = ip.fromTextField(ip.P[i][j]);
			}
		}
		n = (int)ip.fromTextField(ip.N);
		x_a = (int)ip.fromTextField(ip.X_a);
		y_a = (int)ip.fromTextField(ip.Y_a);
		x_b = (int)ip.fromTextField(ip.X_b);
		y_b = (int)ip.fromTextField(ip.Y_b);
		into_wind_time = ip.fromTextField(ip.Into_wind_time);
		up_wind_time = ip.fromTextField(ip.Up_wind_time);
		cross_wind_time = ip.fromTextField(ip.Cross_wind_time);
		down_wind_time = ip.fromTextField(ip.Down_wind_time);
		away_wind_time = ip.fromTextField(ip.Away_wind_time);
		delay = ip.fromTextField(ip.Delay);

		int i,j,x,y,t,w,h;
		double totat_time;

		v = new double[n+1][n+1][3][8];
		bestheading = new int[n+1][n+1][3][8];

		gp.init(n, x_a, y_a, x_b, y_b);
		dynamic_prog(v, bestheading);
		op.showanswer("expected best time = "+v[x_a][y_a][1][1]);
		gp.putExpTime(v[x_a][y_a][1][1]);
		Simulate();
	}

	public void Simulate()
	{
		int x=x_a,y=y_a,t=1,w=1,h;
		double total_time = 0.0;

		gp.setType("Simulate");
		gp.clear();

		/******************************************************
		*   While not at destination move boat to new position
		*******************************************************/

		while(x!=x_b || y!=y_b){

		    /**************************************************
		    *   Get best heading
		    ***************************************************/

		    h=bestheading[x][y][t][w];

		    /**************************************************
		    *   Update time
		    **************************************************/

		    total_time += calc_delay(t,tackt(h,w))+time(h,w);

		    /*************************************************
		    *   Update tack
		    *************************************************/

		    t=tackt(h,w);

		    /*************************************************
		    *   Move boat
		    *************************************************/

		    gp.simgraphics(x,y,t,w,dx(h),dy(h),dx(w),dy(w));
		    x += dx(h);
		    y += dy(h);

		    /*************************************************
		    *   Get new wind direction
		    *************************************************/

		    w=wind(w);
		}

		gp.histographics(total_time);

		gp.repaint();

	}

	public void Histogram()
	{
		gp.setType("Histogram");
		gp.clear();
		gp.repaint();
	}

	int tack(int h, int w)
	{
	    return ( (w-h)>=0 ? w-h : w-h+8 );
	}

	int tackt(int h, int w)
	{
		int t;

		t = tack(h,w);
		if (t>=1 && t<=3) {
			return 1;
		} else if (t>=5 && t<=7) {
			return 0;
		} else {
			return 2;
		}
	}

	double calc_delay(int t, int t_prime)
	{
		return (t==t_prime || t==2 || t_prime==2)? 0.0:delay;
	}

	double time(int h, int w)
	{
		double time_factor;

		time_factor = (h%2==1)?Math.sqrt(2.0):1.0;

		switch (tack(h,w)) {
			case 0:
			    return into_wind_time*time_factor;
			case 1: case 7:
			    return up_wind_time*time_factor;
			case 2: case 6:
			    return cross_wind_time*time_factor;
			case 3: case 5:
			    return down_wind_time*time_factor;
			case 4:
			    return away_wind_time*time_factor;
		}
		return 0;
	}

	int dx(int h)
	{
	    switch(h){
		case 0: case 4:
		    return 0;
		case 1: case 2: case 3:
		    return 1;
		case 5: case 6: case 7:
		    return -1;
	    }
	    return 0;
	}

	int dy(int h)
	{
	    switch(h){
		case 2: case 6:
		    return 0;
		case 0: case 1: case 7:
		    return 1;
		case 3: case 4: case 5:
		    return -1;
	    }
	    return 0;
	}

	int wind(int old_w)
	{
	    int new_w=0,w;
	    double rannum,cumprob;

	    rannum=r.nextDouble();
	    cumprob=0.0;

	    for(w=0;w<8;w++){
		cumprob += p[old_w][w];
		if(rannum<cumprob){
		    new_w=w;
		    break;
		}
	    }
	    return new_w;
	}

	void dynamic_prog(double[][][][] v, int[][][][] bestheading)
	{
		double iNFINITY=1000000.0;
		double tolerance=1.0e-3;

		int x,y,t,w_p,h_min,w,h;
		double error, new_value;
		double f;

		/*********************************************
		* Initialize v
		*/

		for (x=0;x<=n; x++) {
		    for (y=0; y<=n; y++) {
			for (t=0; t<3; t++) {
			    for (w=0; w<8; w++) {
				v[x][y][t][w] = iNFINITY;
			    }
			}
		    }
		}
		for (t=0; t<3; t++) {
		    for (w=0; w<8; w++) {
			v[x_b][y_b][t][w] = 0.0;
		    }
		}

		/*********************************************
		* Compute v
		*/

                do{
                  error=0.0;
                  for (x=1; x<n; ++x) {
                    for (y=1; y<n; ++y) {
                      if (x!=x_b || y!=y_b) {
                        for (t=0; t<3; ++t) {
                          for (w=0; w<8; ++w) {
                            new_value=iNFINITY;
                            h_min=0;
                            for(h=0; h<8; ++h) {
			      f=0;
			      for (w_p=0; w_p<8; ++w_p) {
			          f += p[w][w_p]*
			            v[x+dx(h)][y+dy(h)][tackt(h,w)][w_p];
			      }
			      f += calc_delay(t,tackt(h,w))+time(h,w);
			      if (f<new_value) {
			          new_value=f;
			          h_min=h;
			      }
                            }
                            if (Math.abs(new_value-v[x][y][t][w])>error) {
			      error= Math.abs(new_value-v[x][y][t][w]);
                            }
                            bestheading[x][y][t][w]=h_min;
                            v[x][y][t][w]=new_value;
                          }
                        }
                      }
                    }
                  }
		  op.showanswer("residual = "+error);

                } while (error>tolerance);
	}
}

class MenuPanel extends Canvas
{
	Image image1;

	public MenuPanel()
	{
		setBackground(new Color(190,190,190));
		setSize(30,30);
	}
	public void getPicture(Image image)
	{	image1 = image;
	}
	public void paint(Graphics g)
	{	g.drawImage(image1,0,0,30,30,this);
	}
}

class GraphPad extends Canvas
{
	int xa, ya, xb, yb, n;
	int m = 0;
	double exptime;

	int type, simul=0, histo=1;
	double times[] = new double[1000];
	int ntimes=0;

	Color bgcolor = new Color(190,190,190);
	Color color[];
	int x[], y[], t[], w[], dxh[], dyh[], dxw[], dyw[];

	int isize = 500;
	double dsize = (double) isize;
	double xmin, xmax, ymin, ymax;

	Graphics g;
	Hashtable ht = new Hashtable(200);

	Image beach, sailboat;

	public void getPicture(Image beach1, Image sailboat1)
	{
		beach = beach1; sailboat = sailboat1;
	}

	public void setType(String type1)
	{
		if (type1.equals("Histogram")) {
			type = histo;
		} else
		if (type1.equals("Simulate")) {
			type = simul;
		}
	}

	public void init(int n1, int xa1, int ya1, int xb1, int yb1)
	{
		n = n1; xa = xa1; ya = ya1; xb = xb1; yb = yb1;
		color = new Color[4*n];
		x = new int[4*n];
		y = new int[4*n];
		t = new int[4*n];
		w = new int[4*n];
		dxh = new int[4*n];
		dyh = new int[4*n];
		dxw = new int[4*n];
		dyw = new int[4*n];

		// ortho2(-1,n+1,-1,n+1);
		ht.clear();
		ntimes = 0;
		clear();
	}

	public void simgraphics(int x1, int y1, int t1, int w1,
			     int dxh1, int dyh1, int dxw1, int dyw1)
	{
		x[m] = x1; y[m] = y1; t[m] = t1; w[m] = w1;
		dxh[m] = dxh1; dyh[m] = dyh1; dxw[m] = dxw1; dyw[m] = dyw1;
		m++;
	}

	public void histographics(double time)
	{
		int i;
		String timestr;
		if (ntimes<1000) {
			timestr = Double.toString(time);
			if (ht.containsKey(timestr)) {
				i = Integer.parseInt(
					String.valueOf(ht.get(timestr)));
			} else {
				times[ntimes] = time;
				ntimes++;
				i=0;
			}
			ht.put(timestr, Integer.toString(i+1));
		}
	}

	public void putExpTime( double exptime1 )
	{
		exptime = exptime1;
	}

	public GraphPad()
	{
		setBackground(bgcolor);
		setSize(490,490);
	}

	public void clear()
	{
		m = 0;
	}

	public void paint(Graphics g)
	{
	    GL.ginit(getGraphics(), size().width, size().height, this, false);
	    if (type == simul && n != 0) {

		GL.ortho2(-1,n+1,-1,n+1);

		/*******************************************
		*  Border
		*/

		GL.color(Color.yellow);
		GL.move2(0,0);
		GL.draw2(0,n);
		GL.draw2(n,n);
		GL.draw2(n,0);
		GL.draw2(0,0);

		/*******************************************
		*  Draw beach
		*/

		GL.drawImage(beach, n/2.0, n,
			GL.x(n)-GL.x(0), 20, "CENTER", "BOTTOM", this);

		/*******************************************
		*  dots at nodes
		*/

		GL.color(Color.black);
		for (int i=1; i<n; i++) {
			for (int j=1; j<n; j++) {
				GL.circf(i,j,0.03);
			}
		}

		/*******************************************
		*  dots at start and end
		*/

		GL.color(Color.gray);
		GL.circf(xa,ya,0.1);
		GL.circf(xb,yb,0.1);

		for (int i=0; i<m; i++) {

		    /***************************************************
		    *   Draw sailboat
		    **************************************************/

		    if (i==m/2)
		    GL.drawImage(sailboat, x[i]+dxw[i], y[i]+dyw[i],
					50, 50, "CENTER", "CENTER", this);

		    /***************************************************
		    *   Decide color of path and draw it
		    **************************************************/

		    if(t[i]==0){
			GL.color(Color.red);
		    }else if (t[i]==1){
			GL.color(Color.green);
		    }else{
			GL.color(Color.black);
		    }
		    GL.move2( x[i], y[i]);
		    GL.draw2( x[i]+dxh[i], y[i]+dyh[i]);

		    GL.pmv2( x[i]+dxh[i], y[i]+dyh[i] );
		    GL.pdr2( x[i]+dxh[i]*5./6.+dyh[i]/12.,
			     y[i]+dyh[i]*5./6.-dxh[i]/12.);
		    GL.pdr2( x[i]+dxh[i]*5./6.-dyh[i]/12.,
			     y[i]+dyh[i]*5./6.+dxh[i]/12.);
		    GL.pclos();

		    /***************************************************
		    *    Draw wind arrow
		    **************************************************/

		    GL.color(Color.blue);
		    GL.move2( x[i]+dxh[i]/2.+dxw[i]/4.,
			      y[i]+dyh[i]/2.+dyw[i]/4. );
		    GL.draw2( x[i]+dxh[i]/2.-dxw[i]/4.,
			      y[i]+dyh[i]/2.-dyw[i]/4. );

		    GL.pmv2( x[i]+dxh[i]/2.-dxw[i]/4.,
			     y[i]+dyh[i]/2.-dyw[i]/4.);
		    GL.pdr2( x[i]+dxh[i]/2.-dxw[i]/6.+dyw[i]/12.,
			     y[i]+dyh[i]/2.-dyw[i]/6.-dxw[i]/12. );
		    GL.pdr2( x[i]+dxh[i]/2.-dxw[i]/6.-dyw[i]/12.,
			     y[i]+dyh[i]/2.-dyw[i]/6.+dxw[i]/12. );
		    GL.pclos();
		}
	    } else if (type == histo) {
		double time=0, tottime;
		double mintime, maxtime;
		int count, maxcount = 0;
		int cum, nn;
		double mode, median, mean;
		double xmargin, ymargin;

		if (ntimes == 0) return;

	        /***************************************************
	         * Find the mode
	         ***************************************************/

		for (int j=0; j<ntimes; j++) {
			time = times[j];
			count = Integer.parseInt(
				String.valueOf(
				ht.get(Double.toString(time))));
			if (count > maxcount){
				maxcount = count;
				mode = time;
			}
		}

	        /***************************************************
	         * Sort times[]
	         ***************************************************/

		qksort( times, 0, ntimes-1 );

	        /***************************************************
	         * Find the min, max, mean, and median
	         ***************************************************/

		nn = 0;
		tottime = 0.0;
		for (int j=0; j<ntimes; j++) {
			time = times[j];
			count = Integer.parseInt(
				String.valueOf(
				ht.get(Double.toString(time))));
			nn += count;
			tottime += time*count;
		}
		cum = 0;
		for (int j=0; j<ntimes; j++) {
			time = times[j];
			count = Integer.parseInt(
				String.valueOf(
				ht.get(Double.toString(time))));
			cum += count;
			if (cum > 0.5*nn) break;
		}
		mintime = times[0];
		maxtime = times[ntimes-1];
		median = time;
		mean = tottime/nn;
		System.out.println("min    = "+mintime);
		System.out.println("median = "+median);
		System.out.println("mean   = "+mean);
		System.out.println("max    = "+maxtime);
		System.out.println("maxcnt = "+maxcount);

	        /***************************************************
	         * Do the graphics
	         ***************************************************/

		xmargin = 0.2 *(maxtime-mintime);
		ymargin = 0.25*(maxcount);
		GL.ortho2( mintime-xmargin, maxtime+xmargin,
		           -ymargin, maxcount+ymargin);
		GL.color(Color.black);
		GL.move2(mintime, 0);
		GL.draw2(maxtime, 0);
		for (int j=0; j<ntimes; j++) {
			time = times[j];
			count = Integer.parseInt(
				String.valueOf(
				ht.get(Double.toString(time))));
			GL.move2(time, 0);
			GL.draw2(time, count);
		}

		GL.color(Color.black);
		GL.drawString("Expected best time: "
			+Format.floating(7,2,exptime),
			0.6*maxtime, maxcount, "LEFT", "CENTER");
		GL.drawString("num simuls: "+nn,
				mintime, maxcount, "LEFT", "CENTER");

		time = mintime;
		count = Integer.parseInt(
			String.valueOf(
			ht.get(Double.toString(time))));
		GL.color(Color.black);
		GL.move2(time, 0);
		GL.draw2(time, count);
		GL.drawString(Format.floating(7,2,mintime),
				time, -0.1*ymargin, "CENTER", "CENTER");
		GL.drawString("min:        "+Format.floating(7,2,mintime),
				mintime, 0.95*maxcount, "LEFT", "CENTER");

		time = maxtime;
		count = Integer.parseInt(
			String.valueOf(
			ht.get(Double.toString(time))));
		GL.color(Color.black);
		GL.move2(time, 0);
		GL.draw2(time, count);
		GL.drawString(Format.floating(7,2,maxtime),
				time, -0.1*ymargin, "CENTER", "CENTER");
		GL.drawString("max:        "+Format.floating(7,2,maxtime),
				mintime, 0.90*maxcount, "LEFT", "CENTER");

		time = median;
		count = Integer.parseInt(
			String.valueOf(
			ht.get(Double.toString(time))));
		GL.color(Color.red);
		GL.move2(time, 0);
		GL.draw2(time, count);
		GL.drawString(Format.floating(7,2,median),
				time, -0.1*ymargin, "CENTER", "CENTER");
		GL.drawString("median:     "+Format.floating(7,2,median),
				mintime, 0.85*maxcount, "LEFT", "CENTER");

		time = mean;
		GL.color(Color.blue);
		GL.move2(time, 0);
		GL.draw2(time, maxcount/20);
		GL.drawString(Format.floating(7,2,mean),
				time, -0.2*ymargin, "CENTER", "CENTER");
		GL.drawString("mean:       "+Format.floating(7,2,mean),
				mintime, 0.80*maxcount, "LEFT", "CENTER");

	    }
	}

	void qksort( double[] v, int left, int right )
	{
		int i, last;

		if (left >= right) return;   /* do nothing if array contains
						fewer than two elements */

		swap(v, left, (left + right)/2);  /* move partition elem */
		last = left;                      /* to v[left] */
		for (i = left+1; i <= right; i++) /* partition */
		    if (v[i] < v[left])
			swap(v, ++last, i);
		swap(v, left, last);              /* restore partition elem */
		qksort(v, left, last-1);
		qksort(v, last+1, right);
	}

	void swap( double[] v, int i, int j)
	{
		double temp;

		temp = v[i];
		v[i] = v[j];
		v[j] = temp;
	}

}

class OutPanel extends RaisedPanel
{
	TextArea ta;

	public OutPanel()
	{
		setLayout(new BorderLayout(0,0));
		setFont(new Font("Helvetica",Font.PLAIN,10));
		ta = new TextArea(
"1. PreEdit the fields as desired to define the problem you want to solve. \n"+
"2. Press the Calculate button to solve the problem. \n"+
"3. Press the Simulate button to generate a random instance \n"+
"   of the changing wind and show the optimal strategy on the graph pad at \n"+
"   right. \n"+
"4. Press the Simulate button several times to see multiple instances. \n"+
"5. Press the View Stats button to see some statistics \n"+
"   obtained from the simulations",
		5,80);
		ta.setEditable(false);
		Panel p = new Panel();
		p.setFont(new Font("Helvetica",Font.PLAIN,9));
		Panel p1 = new Panel();
		p1.setLayout(new FlowLayout(FlowLayout.LEFT));
		p1.add(new Label("Copyright 1996, Princeton University"));
		Panel p2 = new Panel();
		p2.setLayout(new FlowLayout(FlowLayout.RIGHT));
		p2.add(new Label("Statistics and Operations Research"+
					" Program"));
		p.setLayout(new GridLayout(1,2));
		p.add(p1);
		p.add(p2);

		add("South",p);
		add("North",ta);
	}

	public void showanswer(String s)
	{
		toTextArea(ta, s);
	}

	public void toTextArea(TextArea x, String s)
	{
		x.appendText( s+"\n" );
	}

	double parsedouble(String str)
	{
		return (Double.valueOf(str).doubleValue());
	}
}


class InPanel extends Panel
{
	public InPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;

		GridPanel p1 = new GridPanel();
		p1.setLayout(gbl);
		p1.setFont(new Font("TimesRoman",Font.PLAIN,10));

		Label pHeading = new Label("Wind Transition Probabilities");
		Label from = new Label("From");
		Label to   = new Label("To");

		Label from_label[] = {
			new Label("N "),
			new Label("NE"),
			new Label("E "),
			new Label("SE"),
			new Label("S "),
			new Label("SW"),
			new Label("W "),
			new Label("NW")
			};

		Label to_label[] = {
			new Label("N "),
			new Label("NE"),
			new Label("E "),
			new Label("SE"),
			new Label("S "),
			new Label("SW"),
			new Label("W "),
			new Label("NW")
			};

		p1.add(pHeading,gbl,gbc,3,0,8,1);
		p1.add(to,gbl,gbc,3,1,8,1);
		for (int i=0; i<8; i++) {
			p1.add(to_label[i], gbl, gbc, i+3, 2, 1, 1);
		}

		p1.add(from,gbl,gbc,1,3,1,8);
		for (int i=0; i<8; i++) {
			p1.add(from_label[i], gbl, gbc, 2, i+3, 1, 1);
		}


		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++) {
			    if ( j==(i+1)%8 || j==i || (j+1)%8==i ) {
				p1.add(P[i][j],    gbl, gbc, j+3, i+3, 1, 1);
			    }
			    /*
			    else {
				Label zero_label = new Label("0.0");
				p1.add(zero_label, gbl, gbc, j+3, i+3, 1, 1);
			    }
			    */
			}
		}

		p1.add(new Label("Into"),  gbl, gbc, 11, 3, 1, 1);
		p1.add(Into_wind_time,     gbl, gbc, 12, 3, 1, 1);

		p1.add(new Label("Up"),    gbl, gbc, 11, 4, 1, 1);
		p1.add(Up_wind_time,       gbl, gbc, 12, 4, 1, 1);

		p1.add(new Label("Cross"), gbl, gbc, 11, 5, 1, 1);
		p1.add(Cross_wind_time,    gbl, gbc, 12, 5, 1, 1);

		p1.add(new Label("Down"),  gbl, gbc, 11, 6, 1, 1);
		p1.add(Down_wind_time,     gbl, gbc, 12, 6, 1, 1);

		p1.add(new Label("Away"),  gbl, gbc, 11, 7, 1, 1);
		p1.add(Away_wind_time,     gbl, gbc, 12, 7, 1, 1);

		p1.add(new Label("Delay"), gbl, gbc, 11, 8, 1, 1);
		p1.add(Delay,              gbl, gbc, 12, 8, 1, 1);

		GridPanel p2 = new GridPanel();
		p2.setLayout(gbl);
		p2.setFont(new Font("TimesRoman",Font.PLAIN,10));

		p2.add(new Label("Lake Size"), gbl, gbc, 1, 1, 1, 1);
		p2.add(N, gbl, gbc, 2, 1, 1, 1);

		p2.add(new Label("Start x"), gbl, gbc, 3, 1, 1, 1);
		p2.add(X_a, gbl, gbc, 4, 1, 1, 1);

		p2.add(new Label("Start y"), gbl, gbc, 5, 1, 1, 1);
		p2.add(Y_a, gbl, gbc, 6, 1, 1, 1);

		p2.add(new Label("End x"),   gbl, gbc, 7, 1, 1, 1);
		p2.add(X_b, gbl, gbc, 8, 1, 1, 1);

		p2.add(new Label("End y"),   gbl, gbc, 9, 1, 1, 1);
		p2.add(Y_b, gbl, gbc, 10, 1, 1, 1);

		setLayout(new BorderLayout(0,0));
		add("North",p1);
		add("South",p2);
	}

	void toTextField(TextField x, double t)
	{
		x.setText( Double.toString(t) );
	}

	double fromTextField(TextField x)
	{
		return (parsedouble(x.getText()));
	}

	double parsedouble(String str)
	{
		return (Double.valueOf(str).doubleValue());
	}

	TextField P[][] =
	{
	    {
		new TextField("0.4",2),
		new TextField("0.3",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.3",2)
	    },
	    {
		new TextField("0.4",2),
		new TextField("0.3",2),
		new TextField("0.3",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2)
	    },
	    {
		new TextField("0.0",2),
		new TextField("0.4",2),
		new TextField("0.3",2),
		new TextField("0.3",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2)
	    },
	    {
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.4",2),
		new TextField("0.3",2),
		new TextField("0.3",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2)
	    },
	    {
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.4",2),
		new TextField("0.2",2),
		new TextField("0.4",2),
		new TextField("0.0",2),
		new TextField("0.0",2)
	    },
	    {
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.3",2),
		new TextField("0.3",2),
		new TextField("0.4",2),
		new TextField("0.0",2)
	    },
	    {
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.3",2),
		new TextField("0.3",2),
		new TextField("0.4",2)
	    },
	    {
		new TextField("0.4",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.0",2),
		new TextField("0.3",2),
		new TextField("0.3",2)
	    }
	};

	TextField N   = new TextField("5",2);
	TextField X_a = new TextField("1",2);
	TextField Y_a = new TextField("1",2);
	TextField X_b = new TextField("4",2);
	TextField Y_b = new TextField("4",2);

	TextField  Into_wind_time = new TextField("100000",5);
	TextField    Up_wind_time = new TextField("4",5);
	TextField Cross_wind_time = new TextField("3",5);
	TextField  Down_wind_time = new TextField("2",5);
	TextField  Away_wind_time = new TextField("1",5);
	TextField           Delay = new TextField("3",5);
}

class RaisedPanel extends Panel
{
        public Insets insets()
        {
                Insets in = new Insets(2,2,2,2);
                return(in);
        }

        public void paint(Graphics g)
        {
                int red, green, blue, rred, ggreen, bblue;
                Color bg, darkColor, lightColor;
                Dimension wSize;

                bg = getBackground();
                red = bg.getRed();
                green = bg.getGreen();
                blue = bg.getBlue();
                wSize = size();

		rred = red<50?0:red-50;
		ggreen = green<50?0:green-50;
		bblue = blue<50?0:blue-50;
                darkColor  = new Color(rred,ggreen,bblue);

		rred = red>205?255:red+50;
		ggreen = green>205?255:green+50;
		bblue = blue>205?255:blue+50;
                lightColor = new Color(rred,ggreen,bblue);

                g.setColor(lightColor);
                g.drawLine(1,1,wSize.width-1,1);
                g.drawLine(2,2,wSize.width-2,2);
                g.drawLine(1,1,1,wSize.height-1);
                g.drawLine(2,2,2,wSize.height-2);
                g.setColor(darkColor);
                g.drawLine(wSize.width-1,1,wSize.width-1,wSize.height-1);
                g.drawLine(wSize.width-2,2,wSize.width-2,wSize.height-2);
                g.drawLine(1,wSize.height-1,wSize.width-1,wSize.height-1);
                g.drawLine(2,wSize.height-2,wSize.width-2,wSize.height-2);
        }
}

class GridPanel extends Panel
{
	public void add(Component c, GridBagLayout gbl,
				GridBagConstraints gbc,
				int x, int y, int w, int h)
	{
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		gbl.setConstraints(c,gbc);
		add(c);
	}
}
