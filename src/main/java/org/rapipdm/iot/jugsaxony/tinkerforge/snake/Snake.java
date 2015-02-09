/**
 * Public Domain
 */
package org.rapipdm.iot.jugsaxony.tinkerforge.snake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.tinkerforge.AlreadyConnectedException;
import com.tinkerforge.BrickletAmbientLight;
import com.tinkerforge.BrickletLCD20x4;
import com.tinkerforge.BrickletMultiTouch;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;

/**
 * A pseudo Snake clone with Tinkerforge
 * 
 * @author Christoph Schmidt
 * @author Jens Pfahl
 *
 */
public class Snake {
	private static final String AMBIENT_ID = "mfV";
	private static final String LCD_ID = "odh";
	private static final String TOUCH_ID = "jS5";
	private static final String HOST = "localhost";
	private static final int PORT = 4223;
	
	private static class Coordinate {
		short line;
		short pos;
		
		
		public Coordinate(short line, short pos) {
			super();
			this.line = line;
			this.pos = pos;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + line;
			result = prime * result + pos;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coordinate other = (Coordinate) obj;
			if (line != other.line)
				return false;
			if (pos != other.pos)
				return false;
			return true;
		}
		
		
	}
	

	private enum Direction { UP, DOWN, RIGHT, LEFT, NOP };
	private static Direction dir = Direction.RIGHT;
	
	public static void main(String[] args) throws Exception {
		
		List<Coordinate> enemies = new LinkedList<Snake.Coordinate>();
		
		final IPConnection ipcon = new IPConnection(); // Create IP connection
        
		
        final BrickletLCD20x4 lcd = new BrickletLCD20x4 (LCD_ID, ipcon);
        final BrickletMultiTouch touch = new BrickletMultiTouch (TOUCH_ID, ipcon);
        try {
			ipcon.connect(HOST, PORT); // Connect to brickd
			lcd.clearDisplay();
			// Don't use device before ipcon is connected
			lcd.backlightOn();
			populate(lcd, enemies);
			
		
			
			touch.addTouchStateListener(new BrickletMultiTouch.TouchStateListener() {
	            public void touchState(int touchState) {
	            	List<Integer> pressed = new ArrayList<>(12);
	                String str = "";

	                if((touchState & (1 << 12)) == (1 << 12)) {
	                    str += "In proximity, ";
	                }

	                if((touchState & 0xfff) == 0) {
	                    str += "No electrodes touched" + System.getProperty("line.separator");
	                } else {
	                    str += "Electrodes ";
	                    for(int i = 0; i < 12; i++) {
	                        if((touchState & (1 << i)) == (1 << i)) {
	                            str += i + " ";
	                             pressed.add(i);
	                        }
	                    }
	                    str += "touched" + System.getProperty("line.separator");
	                }

	                System.out.println(str);
	                try {
						handle(pressed);
					} catch (Exception e) {
						e.printStackTrace();
					}
	            }

				private void handle(List<Integer> pressed) throws Exception {
					for (Integer i: pressed) {
						switch (i){
						case 0:
							dir = Direction.LEFT;
							break;
						case 2:
							dir=Direction.RIGHT;
							break;
						case 1:
							dir = Direction.DOWN;
							break;
						case 4:
							dir = Direction.UP;
							break;
						case 9:
							lcd.clearDisplay();
							enemies.clear();
							draw();
							dir = Direction.NOP;
							break;
						case 11:
							populate(lcd, enemies);
						default:
							dir = Direction.NOP;
						}
						clear();
						move();
						draw();
					}
				}

				private void draw() throws Exception {
					System.out.println("line: "+line+"; pos: "+pos);
					lcd.writeLine(line, pos, "X");
				}

				private short line=0, pos=0;
				
				private void move() {
					switch(dir){
					case DOWN:
						line++;
						break;
					case UP:
						line--;
						break;
					case LEFT:
						pos--;
						break;
					case RIGHT:
						pos++;
						break;
					default:
					}
					
					pos = (short) Math.max(0, pos);
					pos = (short) Math.min(19, pos);
					
					line = (short) Math.max(0, line);
					line = (short) Math.min(3, line);
					
				}
				
				private void clear() throws Exception {
					lcd.writeLine(line, pos, " ");
					Coordinate current = new Coordinate(line, pos);
					if (enemies.contains(current))
						lcd.writeLine(line, pos, "!");
					
				}
	        });

			final BrickletAmbientLight al = new BrickletAmbientLight(AMBIENT_ID, ipcon); 
	        al.setIlluminanceCallbackPeriod(250);

			// Add and implement illuminance listener (called if illuminance changes)
			al.addIlluminanceListener(new BrickletAmbientLight.IlluminanceListener() {
			    public void illuminance(int illuminance) {
			        String string = "Illu: " + illuminance/10.0 + " Lux";
					System.out.println(string);
			        try {
			        	if (illuminance < 1000) {
			        		lcd.backlightOn();
			        	}
			        	else {
			        		lcd.backlightOff();
			        	}
					} catch (TimeoutException | NotConnectedException e) {
						e.printStackTrace();
					}
			    }
			});
			
			System.out.println("Press key to exit"); System.in.read();
			ipcon.disconnect();
		} catch (AlreadyConnectedException | TimeoutException
				| NotConnectedException | IOException e) {
			e.printStackTrace();
		}
        
        
		
	}

	private static void populate(BrickletLCD20x4 lcd, List<Coordinate> enemies) throws Exception {
		short line, pos;
		//enemies.clear();
		for (int i = 0; i < 5; i++) {
			line = (short)(Math.random()*4);
			pos = (short)(Math.random()*20);
			
			enemies.add(new Coordinate(line, pos));
			lcd.writeLine(line, pos, "O");
		}	
	}

}
